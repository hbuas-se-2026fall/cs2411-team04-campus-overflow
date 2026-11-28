package com.campusoverflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 禁用账号相关的领域规则（T4-03）。
 *
 * <p>既有的 {@code UserTest} 里没有任何 {@code disable} / {@code canLogin} / {@code ID-1007} 覆盖，
 * 这里补上；放在独立文件而不是往 {@code UserTest} 里塞，是为了让「禁用」这条规则的用例集中可查。
 */
class UserDisableTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");
    private final Actor admin = new Actor(1, "admin", Role.ADMIN);
    private final Actor teacher = new Actor(5, "王老师", Role.TEACHER);
    private final Actor student = new Actor(2, "alice", Role.STUDENT);

    private User newStudent() {
        User u = User.register("2023001217", "小明", "Ming@Campus.edu", "hash", "计算机工程学院", "计科2301", NOW);
        u.assignId(10);
        return u;
    }

    @Nested
    class Authorization {

        @Test
        void onlyAdminCanDisableUser() {
            assertThatThrownBy(() -> newStudent().disable(student))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("仅管理员");
            assertThatThrownBy(() -> newStudent().disable(teacher))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void nullActorIsRejectedAsForbidden() {
            assertThatThrownBy(() -> newStudent().disable(null))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void adminCannotDisableSelf() {
            User self = User.reconstitute(1L, "admin01", "admin", "a@b.cn", "h", Role.ADMIN, UserStatus.ACTIVE,
                    true, null, null, NOW, 0);
            assertThatThrownBy(() -> self.disable(admin))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("不能禁用自己的账号");
        }

        /**
         * 拒绝「禁用自己」这一步在改状态之前，所以自禁失败后账号必须仍是 ACTIVE。
         * 如果实现把 {@code status = DISABLED} 写在前面，这个用例会红。
         */
        @Test
        void failedSelfDisableLeavesStatusUntouched() {
            User self = User.reconstitute(1L, "admin01", "admin", "a@b.cn", "h", Role.ADMIN, UserStatus.ACTIVE,
                    true, null, null, NOW, 0);
            assertThatThrownBy(() -> self.disable(admin)).isInstanceOf(BusinessRuleException.class);
            assertThat(self.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(self.canLogin()).isTrue();
        }
    }

    @Nested
    class StatusTransition {

        @Test
        void disabledUserCannotLogin() {
            User u = newStudent();
            assertThat(u.canLogin()).isTrue();
            u.disable(admin);
            assertThat(u.status()).isEqualTo(UserStatus.DISABLED);
            assertThat(u.canLogin()).isFalse();
        }

        @Test
        void canLoginOnlyForActiveStatus() {
            for (UserStatus status : UserStatus.values()) {
                User u = User.reconstitute(20L, "2023009999", "某同学", "x@b.cn", "h", Role.STUDENT, status,
                        false, null, null, NOW, 0);
                assertThat(u.canLogin()).isEqualTo(status == UserStatus.ACTIVE);
            }
        }

        /**
         * 重复禁用幂等：不抛异常，状态仍是 DISABLED。
         * 这条行为在 {@code User.disable} 里没有显式分支，是靠「直接赋 DISABLED」自然得到的，
         * 这里把它固定下来，避免以后有人加「已禁用则抛冲突」时无感破坏调用方。
         */
        @Test
        void disablingTwiceIsIdempotent() {
            User u = newStudent();
            u.disable(admin);
            u.disable(admin);
            assertThat(u.status()).isEqualTo(UserStatus.DISABLED);
        }

        /**
         * 非管理员即使对已禁用的账号再调一次，也必须先撞权限检查。
         * 权限判断在方法第一行，不会被「反正已经禁用了」短路掉。
         */
        @Test
        void nonAdminStillRejectedOnAlreadyDisabledUser() {
            User u = newStudent();
            u.disable(admin);
            assertThatThrownBy(() -> u.disable(student)).isInstanceOf(ForbiddenException.class);
        }
    }
}
