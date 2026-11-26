package com.campusoverflow.discovery.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.ForbiddenException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link Notification} 的边界测试。
 *
 * <p>重点覆盖三件事：工厂方法的初值与标题规范化、{@code markRead} 的权限判定顺序与幂等性、
 * 以及越权访问在「已读 / 未读」两种状态下行为是否一致。
 */
class NotificationBoundaryTest {

    private static final long RECIPIENT = 7L;
    private static final long STRANGER = 8L;
    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");

    private static Notification notification() {
        return Notification.create(RECIPIENT, NotificationType.ANSWER_RECEIVED, "你的问题收到新回答",
                "/questions/1", "evt-1", NOW);
    }

    @Nested
    @DisplayName("工厂方法")
    class Create {

        @Test
        @DisplayName("新建的通知是未读的，id 由仓储在落库后回填")
        void newNotificationIsUnreadAndHasNoId() {
            Notification n = notification();
            assertThat(n.read()).isFalse();
            assertThat(n.id()).isNull();
        }

        @Test
        @DisplayName("接收者、类型、链接、来源事件、时间原样保留")
        void fieldsArePreserved() {
            Notification n = notification();
            assertThat(n.recipientId()).isEqualTo(RECIPIENT);
            assertThat(n.type()).isEqualTo(NotificationType.ANSWER_RECEIVED);
            assertThat(n.link()).isEqualTo("/questions/1");
            assertThat(n.sourceEventId()).isEqualTo("evt-1");
            assertThat(n.createdAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("标题去除首尾空白")
        void titleIsStripped() {
            Notification n = Notification.create(RECIPIENT, NotificationType.MENTIONED, "  有人提到了你  ",
                    "/questions/1", "evt-2", NOW);
            assertThat(n.title()).isEqualTo("有人提到了你");
        }

        @Test
        @DisplayName("空标题被拒绝：这是系统拼装的文案，为空说明代码缺陷")
        void blankTitleIsRejectedByConstructor() {
            // 标题的 null / 空白由工厂方法拦住，抛 IllegalArgumentException 而不是业务异常，
            // 因为它来自上游事件拼装、不是用户输入。
            assertThatThrownBy(() -> Notification.create(RECIPIENT, NotificationType.MENTIONED, "   ",
                    "/questions/1", "evt-3", NOW))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Notification.create(RECIPIENT, NotificationType.MENTIONED, null,
                    "/questions/1", "evt-3", NOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("超过 200 字符的标题被截断到 200，不会整条通知丢失")
        void overlyLongTitleIsTruncated() {
            // dis_notification.title 是 VARCHAR(200)。若直接抛异常，一条长标题就会让整条通知丢失，
            // 用户完全收不到提醒 —— 截断是更合适的降级方式。
            String longTitle = "标".repeat(250);
            Notification n = Notification.create(RECIPIENT, NotificationType.ANSWER_RECEIVED, longTitle,
                    "/questions/1", "evt-4", NOW);
            assertThat(n.title()).hasSize(200);
            assertThat(n.title()).isEqualTo("标".repeat(200));
        }

        @Test
        @DisplayName("恰好 200 字符的标题不被截断")
        void titleOfExactlyTwoHundredIsNotTruncated() {
            String title = "标".repeat(200);
            assertThat(Notification.create(RECIPIENT, NotificationType.ANSWER_RECEIVED, title,
                    "/questions/1", "evt-5", NOW).title()).isEqualTo(title);
        }

        @Test
        @DisplayName("type 与 sourceEventId 为 null 时构造器拒绝")
        void nullTypeAndSourceEventAreRejected() {
            assertThatThrownBy(() -> Notification.create(RECIPIENT, null, "标题", "/q/1", "evt-6", NOW))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Notification.create(RECIPIENT, NotificationType.MENTIONED, "标题", "/q/1", null,
                    NOW))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("标记已读")
    class MarkRead {

        @Test
        @DisplayName("接收者可以标记已读")
        void recipientCanMarkRead() {
            Notification n = notification();
            n.markRead(RECIPIENT);
            assertThat(n.read()).isTrue();
        }

        @Test
        @DisplayName("非接收者标记已读抛 DIS-3001")
        void strangerCannotMarkRead() {
            Notification n = notification();
            assertThatThrownBy(() -> n.markRead(STRANGER))
                    .isInstanceOfSatisfying(ForbiddenException.class,
                            e -> assertThat(e.code()).isEqualTo("DIS-3001"));
            assertThat(n.read()).isFalse();
        }

        @Test
        @DisplayName("已读之后，非接收者依然被拒绝（权限判定在状态判定之前）")
        void strangerIsStillRejectedAfterRead() {
            // 如果实现写成「先判是否已读、已读就 return」，这条会静默通过，
            // 等于把「这条通知已读」这一信息泄露给了非接收者。
            Notification n = notification();
            n.markRead(RECIPIENT);
            assertThatThrownBy(() -> n.markRead(STRANGER))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("重复标记已读是幂等的，不抛异常")
        void repeatedMarkReadIsIdempotent() {
            Notification n = notification();
            n.markRead(RECIPIENT);
            n.markRead(RECIPIENT);
            n.markRead(RECIPIENT);
            assertThat(n.read()).isTrue();
        }
    }

    @Nested
    @DisplayName("reconstitute 与 assignId")
    class Reconstitution {

        @Test
        @DisplayName("从持久化状态重建时保留已读标记与 id")
        void reconstituteKeepsReadState() {
            Notification n = Notification.reconstitute(42L, RECIPIENT, NotificationType.BADGE_AWARDED,
                    "你获得了徽章", "/users/7", "evt-7", true, NOW);
            assertThat(n.id()).isEqualTo(42L);
            assertThat(n.read()).isTrue();
        }

        @Test
        @DisplayName("落库后回填 id 不影响其他字段")
        void assignIdOnlySetsId() {
            Notification n = notification();
            n.assignId(99L);
            assertThat(n.id()).isEqualTo(99L);
            assertThat(n.read()).isFalse();
            assertThat(n.recipientId()).isEqualTo(RECIPIENT);
        }
    }

    /** 确认 6 种通知类型都能构造出通知（防止以后加枚举值时漏改工厂方法）。 */
    @Test
    @DisplayName("全部 6 种通知类型都能正常创建")
    void allNotificationTypesCanBeCreated() {
        for (NotificationType type : NotificationType.values()) {
            Notification n = Notification.create(RECIPIENT, type, "标题", "/q/1", "evt-" + type.name(), NOW);
            assertThat(n.type()).isEqualTo(type);
            assertThat(n.read()).isFalse();
        }
    }
}
