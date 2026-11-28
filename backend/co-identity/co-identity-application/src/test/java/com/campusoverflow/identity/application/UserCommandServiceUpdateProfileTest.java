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
 * <p>全部依赖手写 Fake，不引入 Mockito。重点覆盖三件事：
 * ① 三个字段都能落库（用 Fake 的 store 反查，而不是只看返回值）；
 * ② 昵称重名判定不能把自己算成「别人占用」——这是本方法唯一容易写错的地方；
 * ③ 学号/角色/认证状态不可能被改（{@link UpdateProfileCommand} 根本没有这些字段，靠编译期保证）。
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
