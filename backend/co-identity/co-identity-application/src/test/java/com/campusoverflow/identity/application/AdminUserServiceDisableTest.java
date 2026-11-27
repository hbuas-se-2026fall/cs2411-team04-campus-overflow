package com.campusoverflow.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.identity.domain.PasswordHasher;
import com.campusoverflow.identity.domain.User;
import com.campusoverflow.identity.domain.UserRepository;
import com.campusoverflow.identity.domain.UserStatus;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code AdminUserService.disable} 的用例测试（T4-03）。
 *
 * <p>全部依赖手写 Fake，不引入 Mockito（与项目其余测试一致）。
 * 关键断言是「会话被清理几次」与「保存发生在清理之前」——后者靠 Fake 记录调用顺序来验证。
 */
class AdminUserServiceDisableTest {

    private static final Instant NOW = Instant.parse("2026-11-27T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final Actor admin = new Actor(1, "admin", Role.ADMIN);
    private final Actor student = new Actor(2, "alice", Role.STUDENT);

    private InMemoryUsers users;
    private RecordingAudit audit;
    private RecordingSessions sessions;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        audit = new RecordingAudit();
        sessions = new RecordingSessions();
        PasswordHasher hasher = new PasswordHasher() {
            @Override
            public String hash(String raw) {
                return "hash";
            }

            @Override
            public boolean matches(String raw, String hash) {
                return false;
            }
        };
        service = new AdminUserService(users, hasher, event -> { }, audit, sessions, CLOCK);
    }

    private User persistedStudent() {
        User u = User.register("2023001217", "小明", "ming@campus.edu", "hash", "计算机工程学院", "计科2301", NOW);
        u.assignId(10);
        users.store.put(10L, u);
        return u;
    }

    @Test
    void disableSavesUserAndWritesAudit() {
        persistedStudent();

        service.disable(admin, 10L);

        assertThat(users.saveCount).isEqualTo(1);
        assertThat(users.store.get(10L).status()).isEqualTo(UserStatus.DISABLED);
        assertThat(users.store.get(10L).canLogin()).isFalse();
        assertThat(audit.records).hasSize(1);
        assertThat(audit.records.get(0).action()).isEqualTo("USER_DISABLED");
        assertThat(audit.records.get(0).targetId()).isEqualTo("10");
    }

    @Test
    void disableRemovesAllSessionsOfThatUser() {
        persistedStudent();
        sessions.add("2023001217", "s1", "s2", "s3");

        service.disable(admin, 10L);

        assertThat(sessions.revokedUsernames).containsExactly("2023001217");
        assertThat(sessions.remaining()).isZero();
        assertThat(audit.records.get(0).detail()).contains("3");
    }

    /**
     * 被测的调用顺序：先 {@code users.save}，再清会话。
     *
     * <p>为什么这个顺序重要：若先清会话后落库，落库失败时用户明明还是正常状态却被踢下线；
     * 反过来落库成功、清会话失败时抛异常回滚，保证「不会出现已禁用但会话还在」。
     */
    @Test
    void savesBeforeRevokingSessions() {
        persistedStudent();
        sessions.add("2023001217", "s1");

        service.disable(admin, 10L);

        assertThat(sessions.firstCallIndex).isGreaterThan(users.lastSaveIndex);
    }

    @Test
    void userWithoutSessionsIsStillDisabled() {
        persistedStudent();

        service.disable(admin, 10L);

        assertThat(users.store.get(10L).status()).isEqualTo(UserStatus.DISABLED);
        assertThat(sessions.revokedUsernames).containsExactly("2023001217");
        assertThat(audit.records.get(0).detail()).isNull();
    }

    @Test
    void revokesByUsernameNotDisplayName() {
        persistedStudent();

        service.disable(admin, 10L);

        // 登录名是学号；用昵称「小明」查 Redis 索引永远返回空，禁用看起来成功但会话没清掉。
        assertThat(sessions.revokedUsernames).containsExactly("2023001217");
        assertThat(sessions.revokedUsernames).doesNotContain("小明");
        assertThat(sessions.remaining()).isZero();
    }

    @Test
    void nonAdminCannotDisableAndNothingIsWritten() {
        persistedStudent();

        assertThatThrownBy(() -> service.disable(student, 10L)).isInstanceOf(ForbiddenException.class);

        assertThat(users.saveCount).isZero();
        assertThat(audit.records).isEmpty();
        assertThat(sessions.revokedUsernames).isEmpty();
        assertThat(users.store.get(10L).status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void adminCannotDisableSelfAndNothingIsWritten() {
        User self = User.reconstitute(1L, "admin01", "admin", "a@b.cn", "h", Role.ADMIN, UserStatus.ACTIVE,
                true, null, null, NOW, 0);
        users.store.put(1L, self);

        assertThatThrownBy(() -> service.disable(admin, 1L)).isInstanceOf(BusinessRuleException.class);

        assertThat(users.saveCount).isZero();
        assertThat(sessions.revokedUsernames).isEmpty();
        assertThat(users.store.get(1L).status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void unknownUserIsRejectedWithId4001() {
        assertThatThrownBy(() -> service.disable(admin, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("用户不存在");

        assertThat(users.saveCount).isZero();
        assertThat(sessions.revokedUsernames).isEmpty();
    }

    /**
     * 清会话抛异常时必须冒泡出去，让 {@code @Transactional} 回滚。
     * 若这里被静默吞掉，就会出现「已禁用但旧会话仍在」——正是本卡要堵的漏洞。
     */
    @Test
    void sessionRevocationFailurePropagates() {
        persistedStudent();
        sessions.failWith = new IllegalStateException("Redis 不可用");

        assertThatThrownBy(() -> service.disable(admin, 10L)).isInstanceOf(IllegalStateException.class);
    }

    // ---------------------------------------------------------------- 手写 Fake

    private static final class InMemoryUsers implements UserRepository {
        final Map<Long, User> store = new LinkedHashMap<>();
        int saveCount;
        int lastSaveIndex;

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
            return store.values().stream().anyMatch(u -> u.email().equals(email));
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
            saveCount++;
            lastSaveIndex = Timeline.next();
            store.put(user.id(), user);
            return user;
        }
    }

    private static final class RecordingSessions implements SessionRevoker {
        private final Map<String, List<String>> byUser = new LinkedHashMap<>();
        final List<String> revokedUsernames = new ArrayList<>();
        int firstCallIndex = -1;
        RuntimeException failWith;

        void add(String username, String... sessionIds) {
            byUser.computeIfAbsent(username, k -> new ArrayList<>()).addAll(List.of(sessionIds));
        }

        int remaining() {
            return byUser.values().stream().mapToInt(List::size).sum();
        }

        @Override
        public int revokeAll(String username) {
            firstCallIndex = Timeline.next();
            revokedUsernames.add(username);
            if (failWith != null) {
                throw failWith;
            }
            List<String> ids = byUser.remove(username);
            return ids == null ? 0 : ids.size();
        }
    }

    private static final class RecordingAudit implements AuditTrail {
        record Entry(String action, Long actorId, String targetType, String targetId, String detail) { }

        final List<Entry> records = new ArrayList<>();

        @Override
        public void record(String action, Long actorId, String targetType, String targetId, String detail) {
            records.add(new Entry(action, actorId, targetType, targetId, detail));
        }
    }

    /** 全局单调递增序号，让「保存」与「清会话」的先后在断言里可比。 */
    private static final class Timeline {
        private static int counter;

        static synchronized int next() {
            return ++counter;
        }
    }

    /** 未使用的发布器/哈希器占位，避免测试里出现裸 null 掩盖构造器签名变化。 */
    @SuppressWarnings("unused")
    private static final class NoopPublisher implements IntegrationEventPublisher {
        @Override
        public void publish(IntegrationEvent event) { }
    }
}
