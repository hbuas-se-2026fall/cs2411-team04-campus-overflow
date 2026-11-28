package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.identity.domain.UserStatus;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.ConflictException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code UserCommandService.updateProfile} 的用例测试（T4-05）。
 *
 * <p>全部依赖手写 Fake，不引入 Mockito。重点覆盖四件事：
 * ① 三个字段都能落库（用 Fake 的 store 反查，而不是只看返回值）；
 * ② <b>部分更新语义</b>：{@code null} 保留现值、空串清空，两者不能混为一谈
 *    （见 {@link #nullDisplayNameMeansKeepCurrentNicknameNotClear} 等 4 个用例）；
 * ③ 昵称重名判定不能把自己算成「别人占用」——这是本方法唯一容易写错的地方；
 * ④ 学号/角色/认证状态不可能被改（{@link UpdateProfileCommand} 根本没有这些字段，靠编译期保证）。
 */
class UserCommandServiceUpdateProfileTest {

    private static final Instant NOW = Instant.parse("2026-11-28T10:00:00Z");

    private final Actor alice = new Actor(10, "alice", Role.STUDENT);

    private InMemoryUsers users;
    private RecordingAudit audit;
    private UserCommandService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        audit = new RecordingAudit();
        service = new UserCommandService(users, audit);
    }

    private User persisted(String username, String displayName, long id) {
        User u = User.register(username, displayName, username + "@campus.edu", "hash",
                "计算机工程学院", "计科2301", NOW);
        u.assignId(id);
        users.store.put(id, u);
        return u;
    }

    @Test
    void updatesAllThreeEditableFields() {
        persisted("2023000001", "小明", 10);

        service.updateProfile(alice, new UpdateProfileCommand("小明同学", "信息工程学院", "软工2302"));

        User after = users.store.get(10L);
        assertThat(after.displayName()).isEqualTo("小明同学");
        assertThat(after.college()).isEqualTo("信息工程学院");
        assertThat(after.className()).isEqualTo("软工2302");
    }

    @Test
    void writesAuditRecordWithActorAndNewDisplayName() {
        persisted("2023000001", "小明", 10);

        service.updateProfile(alice, new UpdateProfileCommand("小明同学", null, null));

        assertThat(audit.records).hasSize(1);
        RecordingAudit.Entry e = audit.records.get(0);
        assertThat(e.action()).isEqualTo("PROFILE_UPDATED");
        assertThat(e.actorId()).isEqualTo(10L);
        assertThat(e.targetType()).isEqualTo("USER");
        assertThat(e.targetId()).isEqualTo("10");
        assertThat(e.detail()).isEqualTo("小明同学");
    }

    @Test
    void keepsOwnNicknameWhenItIsSubmittedUnchanged() {
        persisted("2023000001", "小明", 10);

        // 「什么都不改、只改学院」是最常见的姿态，不能因为昵称与自己重名就报 409
        service.updateProfile(alice, new UpdateProfileCommand("小明", null, "软工2302"));

        assertThat(users.store.get(10L).displayName()).isEqualTo("小明");
        assertThat(users.store.get(10L).className()).isEqualTo("软工2302");
    }

    @Test
    void nullDisplayNameMeansKeepCurrentNicknameNotClear() {
        persisted("2023000001", "小明", 10);

        // 部分更新语义：只传学院，昵称传 null 表示「本次不改」，而不是「清空」
        service.updateProfile(alice, new UpdateProfileCommand(null, "信息工程学院", null));

        User after = users.store.get(10L);
        assertThat(after.displayName()).isEqualTo("小明");
        assertThat(after.college()).isEqualTo("信息工程学院");
        assertThat(after.className()).isEqualTo("计科2301");
    }

    @Test
    void nullCollegeAndClassNameKeepCurrentValuesNotClear() {
        persisted("2023000001", "小明", 10);

        // 只改昵称：学院/班级传 null 必须沿用现值（注册时是「计算机工程学院」「计科2301」）
        service.updateProfile(alice, new UpdateProfileCommand("小明同学", null, null));

        User after = users.store.get(10L);
        assertThat(after.displayName()).isEqualTo("小明同学");
        assertThat(after.college()).isEqualTo("计算机工程学院");
        assertThat(after.className()).isEqualTo("计科2301");
    }

    @Test
    void allNullKeepsEverythingButStillWritesAudit() {
        persisted("2023000001", "小明", 10);

        // 三个字段全 null 是一次「空更新」：不改任何值，但仍然落一条审计，便于排查调用方误发空请求
        service.updateProfile(alice, new UpdateProfileCommand(null, null, null));

        User after = users.store.get(10L);
        assertThat(after.displayName()).isEqualTo("小明");
        assertThat(after.college()).isEqualTo("计算机工程学院");
        assertThat(after.className()).isEqualTo("计科2301");
        assertThat(audit.records).hasSize(1);
    }

    @Test
    void emptyStringStillClearsWhileNullKeeps() {
        persisted("2023000001", "小明", 10);

        // 同一次请求里演示两种语义：学院传 null → 保留；班级传空串 → 清空
        service.updateProfile(alice, new UpdateProfileCommand(null, null, ""));

        User after = users.store.get(10L);
        assertThat(after.college()).isEqualTo("计算机工程学院");
        assertThat(after.className()).isNull();
    }

    @Test
    void emptyStringNicknameIsRejectedByDomainRuleNotCleared() {
        persisted("2023000001", "小明", 10);

        // 昵称不像学院/班级那样「空串即清空」：它是必填字段，空串必须走领域层的 ID-1002。
        // 这条同时锁住「接口层不再加 @Size」——若加了，空串会被 Bean Validation 拦成 400
        // SYS-400-VALIDATION，同一个规则出现两种错误码。
        assertThatThrownBy(() -> service.updateProfile(alice, new UpdateProfileCommand("", "信息工程学院", null)))
                .isInstanceOf(com.campusoverflow.shared.domain.BusinessRuleException.class)
                .hasMessageContaining("昵称");

        // 整笔事务不落地：昵称仍是旧值，学院也没被改
        User after = users.store.get(10L);
        assertThat(after.displayName()).isEqualTo("小明");
        assertThat(after.college()).isEqualTo("计算机工程学院");
    }

    @Test
    void rejectsNicknameTakenByAnotherUser() {
        persisted("2023000001", "小明", 10);
        persisted("2023000002", "小红", 11);

        assertThatThrownBy(() -> service.updateProfile(alice, new UpdateProfileCommand("小红", null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("该昵称已被使用");

        assertThat(users.store.get(10L).displayName()).isEqualTo("小明");
    }

    @Test
    void rejectsTooShortNicknameWithDomainRule() {
        persisted("2023000001", "小明", 10);

        // 长度规则复用 User.checkDisplayName，错误码必须是 ID-1002 而不是随便一个新码
        assertThatThrownBy(() -> service.updateProfile(alice, new UpdateProfileCommand("短", null, null)))
                .isInstanceOf(com.campusoverflow.shared.domain.BusinessRuleException.class)
                .hasMessageContaining("昵称");

        assertThat(users.store.get(10L).displayName()).isEqualTo("小明");
    }

    @Test
    void blankCollegeAndClassNameBecomeNull() {
        persisted("2023000001", "小明", 10);

        service.updateProfile(alice, new UpdateProfileCommand("小明", "   ", ""));

        // 前端清空输入框提交的是空串，库里应该变成 NULL 而不是 ''
        assertThat(users.store.get(10L).college()).isNull();
        assertThat(users.store.get(10L).className()).isNull();
    }

    @Test
    void unknownActorIsRejected() {
        persisted("2023000001", "小明", 10);
        Actor ghost = new Actor(99, "ghost", Role.STUDENT);

        assertThatThrownBy(() -> service.updateProfile(ghost, new UpdateProfileCommand("幽灵", null, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void doesNotTouchUsernameRoleOrVerifiedFlag() {
        User before = persisted("2023000001", "小明", 10);

        service.updateProfile(alice, new UpdateProfileCommand("小明同学", "信息工程学院", "软工2302"));

        User after = users.store.get(10L);
        assertThat(after.username()).isEqualTo("2023000001");
        assertThat(after.role()).isEqualTo(before.role());
        assertThat(after.verified()).isEqualTo(before.verified());
        assertThat(after.status()).isEqualTo(UserStatus.ACTIVE);
    }

    // ---------------------------------------------------------------- Fakes

    private static final class InMemoryUsers implements UserRepository {
        private final Map<Long, User> store = new LinkedHashMap<>();

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return store.values().stream().filter(u -> u.username().equals(username)).findFirst();
        }

        @Override
        public Optional<User> findByDisplayName(String displayName) {
            return store.values().stream().filter(u -> u.displayName().equals(displayName)).findFirst();
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public boolean existsByDisplayName(String displayName) {
            return findByDisplayName(displayName).isPresent();
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.values().stream().anyMatch(u -> u.email().equalsIgnoreCase(email));
        }

        @Override
        public List<User> findAllById(Collection<Long> ids) {
            return ids.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public PageResult<User> findByVerified(boolean verified, PageRequest page) {
            List<User> hits = store.values().stream().filter(u -> u.verified() == verified).toList();
            return new PageResult<>(hits, hits.size(), page.page(), page.size());
        }

        @Override
        public User save(User user) {
            store.put(user.id(), user);
            return user;
        }
    }

    private static final class RecordingAudit implements AuditTrail {
        private final List<Entry> records = new java.util.ArrayList<>();

        @Override
        public void record(String action, Long actorId, String targetType, String targetId, String detail) {
            records.add(new Entry(action, actorId, targetType, targetId, detail));
        }

        private record Entry(String action, Long actorId, String targetType, String targetId, String detail) {
        }
    }
}
