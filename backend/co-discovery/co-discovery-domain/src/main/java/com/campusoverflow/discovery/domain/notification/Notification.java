package com.campusoverflow.discovery.domain.notification;

import com.campusoverflow.shared.domain.ForbiddenException;
import java.time.Instant;
import java.util.Objects;

/** 站内通知。(sourceEventId, recipientId) 唯一，事件重复投递不会产生重复通知。 */
public class Notification {

    private Long id;
    private final long recipientId;
    private final NotificationType type;
    private final String title;
    private final String link;
    private final String sourceEventId;
    private boolean read;
    private final Instant createdAt;

    private Notification(Long id, long recipientId, NotificationType type, String title, String link,
                         String sourceEventId, boolean read, Instant createdAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.type = Objects.requireNonNull(type);
        this.title = title;
        this.link = link;
        this.sourceEventId = Objects.requireNonNull(sourceEventId);
        this.read = read;
        this.createdAt = createdAt;
    }

    /** 通知标题的最大长度，与 {@code dis_notification.title VARCHAR(200)} 对齐。 */
    private static final int MAX_TITLE_LENGTH = 200;

    /**
     * 新建一条未读通知。
     *
     * <p><b>标题为什么抛 {@link IllegalArgumentException} 而不是 {@code BusinessRuleException}：</b>
     * 标题由 {@code NotificationProjector} 根据上游事件拼装，不来自用户输入。上游各事件的标题
     * （问题标题、徽章名等）在各自的聚合里已经校验过，到这里仍为空说明是代码缺陷而不是业务规则被违反。
     * 若用业务异常，异常会被 {@code OutboxRelay} 当作可重试错误退避十次后进死信，把一个编码错误
     * 伪装成环境问题；{@code IllegalArgumentException} 能更准确地把问题指向代码。
     *
     * <p><b>为什么截断而不是拒绝过长标题：</b>标题是系统生成的摘要文案，不是用户提交的内容。
     * 超长说明上游正文很长，截断后仍然可读；直接抛异常会让整条通知丢失，用户反而收不到提醒。
     * 按 Unicode 码点截断（{@link String#offsetByCodePoints}）避免从中间劈开一个代理对。
     */
    public static Notification create(long recipientId, NotificationType type, String title, String link,
                                      String sourceEventId, Instant now) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(now, "now");
        String normalizedTitle = title.strip();
        if (normalizedTitle.isEmpty()) {
            throw new IllegalArgumentException("通知标题不能为空");
        }
        if (normalizedTitle.codePointCount(0, normalizedTitle.length()) > MAX_TITLE_LENGTH) {
            normalizedTitle = normalizedTitle.substring(0, normalizedTitle.offsetByCodePoints(0, MAX_TITLE_LENGTH));
        }
        return new Notification(null, recipientId, type, normalizedTitle, link, sourceEventId, false, now);
    }

    public static Notification reconstitute(Long id, long recipientId, NotificationType type, String title,
                                            String link, String sourceEventId, boolean read, Instant createdAt) {
        return new Notification(id, recipientId, type, title, link, sourceEventId, read, createdAt);
    }

    /**
     * 标记为已读。只有接收者本人能操作，其他人抛 {@code ForbiddenException("DIS-3001")}。
     *
     * <p><b>先判权限、再判是否已读</b>：如果反过来（已读就直接 return），一个非接收者去标记
     * 一条已读通知时会「成功」返回而不报错，等于泄露了「这条通知已读」这一信息。先校验归属
     * 能保证无论通知处于什么状态，越权访问都得到一致的 403。
     *
     * <p>已读时重复调用是幂等的（不抛异常、不改状态）：前端可能因网络重试重复提交，
     * 把「重复标记」当成错误会让用户看到一个莫名其妙的失败提示。
     */
    public void markRead(long actorId) {
        if (actorId != recipientId) {
            throw new ForbiddenException("DIS-3001", "只能标记自己的通知为已读");
        }
        this.read = true;
    }

    public void assignId(long id) {
        this.id = id;
    }

    public Long id() { return id; }
    public long recipientId() { return recipientId; }
    public NotificationType type() { return type; }
    public String title() { return title; }
    public String link() { return link; }
    public String sourceEventId() { return sourceEventId; }
    public boolean read() { return read; }
    public Instant createdAt() { return createdAt; }
}
