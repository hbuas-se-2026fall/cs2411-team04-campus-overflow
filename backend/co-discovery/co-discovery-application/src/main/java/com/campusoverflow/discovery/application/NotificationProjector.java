package com.campusoverflow.discovery.application;

import com.campusoverflow.discovery.domain.notification.NotificationRepository;
import com.campusoverflow.discovery.domain.notification.NotificationType;
import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.QaQueryApi;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知中心：把上游事件转换为站内通知。幂等键 (sourceEventId, recipientId)。
 *
 * <p><b>批次 2 临时状态</b>：方法体全部为「记录日志 + 不做任何事」的空实现。原因是
 * {@code OutboxRelay.dispatch()} 捕获异常后会 {@code failed.increment()} 并按指数退避推后
 * {@code locked_until}，超过 10 次进死信——任何一次「提问」或「回答」都会让事件链堵在那里。
 * 真正的通知落库与 @提及解析在 Sprint 3 实现（见各方法上的 {@code TODO(S3)}）。
 */
@Component
public class NotificationProjector {

    private static final Logger log = LoggerFactory.getLogger(NotificationProjector.class);

    private final NotificationRepository notifications;
    private final IdentityApi identity;
    private final QaQueryApi qa;

    public NotificationProjector(NotificationRepository notifications, IdentityApi identity, QaQueryApi qa) {
        this.notifications = notifications;
        this.identity = identity;
        this.qa = qa;
    }

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        // TODO(S3)：给问题作者发「你的问题有新回答」通知
        log.debug("忽略回答提交事件 {}（Sprint 3 实现通知落库）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        // TODO(S3)：给回答者发「你的回答被采纳」通知
        log.debug("忽略回答采纳事件 {}（Sprint 3 实现通知落库）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(AnswerEndorsedEvent e) {
        // TODO(S3)：给回答者发「你的回答被教师认证」通知
        log.debug("忽略回答认证事件 {}（Sprint 3 实现通知落库）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(CommentPostedEvent e) {
        // TODO(S3)：解析 @昵称，给被提及者发通知
        log.debug("忽略评论事件 {}（Sprint 3 实现 @提及与通知落库）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(BountyAwardedEvent e) {
        // TODO(S3)：给悬赏获奖者发通知
        log.debug("忽略悬赏结算事件 {}（Sprint 3 实现通知落库）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(BadgeAwardedEvent e) {
        // TODO(S3)：给徽章获得者发通知
        log.debug("忽略徽章授予事件 {}（Sprint 3 实现通知落库）", e.eventId());
    }

    private void notify(long recipientId, NotificationType type, String title, String link, String eventId,
                        java.time.Instant at) {
        // TODO(S3)：落库 Notification 并靠唯一键 (source_event_id, recipient_id) 实现幂等
        log.debug("跳过通知写入 recipient={} type={} event={}（Sprint 3 实现）", recipientId, type, eventId);
    }

    private String name(long userId) {
        // TODO(S3)：把用户 id 换成显示名，供通知文案使用；当前返回空串占位
        log.debug("跳过用户昵称解析 userId={}（Sprint 3 实现）", userId);
        return "";
    }
}
