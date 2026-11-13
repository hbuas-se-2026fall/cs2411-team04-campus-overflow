package com.campusoverflow.discovery.application;

import com.campusoverflow.discovery.domain.search.SearchIndex;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BountyAwardedEvent;
import com.campusoverflow.reputation.api.event.BountyClosedEvent;
import com.campusoverflow.reputation.api.event.BountyOpenedEvent;
import com.campusoverflow.shared.event.IntegrationEvent;
import com.campusoverflow.shared.event.ProcessedEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事件投影器：把上游事件折叠为搜索投影（dis_search_doc）。
 * 计数器类更新不是天然幂等的，因此每个事件先经 ProcessedEventStore 去重（与投影更新同一事务）。
 *
 * <p><b>批次 2 临时状态</b>：方法体全部为「记录日志 + 不做任何事」的空实现。原因是
 * {@code OutboxRelay.dispatch()} 捕获异常后会 {@code failed.increment()} 并按指数退避推后
 * {@code locked_until}，超过 10 次进死信——任何一次「提问」或「回答」都会让事件链堵在那里。
 * 真正的搜索投影写入在 Sprint 3 实现（见各方法上的 {@code TODO(S3)}）。
 */
@Component
public class SearchIndexProjector {

    static final String CONSUMER = "discovery.search-index";

    private static final Logger log = LoggerFactory.getLogger(SearchIndexProjector.class);

    private final SearchIndex index;
    private final ProcessedEventStore processed;

    public SearchIndexProjector(SearchIndex index, ProcessedEventStore processed) {
        this.index = index;
        this.processed = processed;
    }

    private boolean firstTime(IntegrationEvent e) {
        // TODO(S3)：调用 processed.markIfAbsent(CONSUMER, e.eventId())，首次返回 true
        log.debug("跳过事件去重判定 event={}（Sprint 3 实现）", e.eventId());
        return false;
    }

    @EventListener
    @Transactional
    public void on(QuestionPostedEvent e) {
        // TODO(S3)：写入 dis_search_doc 并索引标签
        log.debug("忽略提问事件 {}（Sprint 3 实现搜索投影）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(QuestionEditedEvent e) {
        // TODO(S3)：更新标题、正文纯文本与标签
        log.debug("忽略问题编辑事件 {}（Sprint 3 实现搜索投影）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        // TODO(S3)：answer_count +1，刷新 last_activity_at
        log.debug("忽略回答提交事件 {}（Sprint 3 实现搜索投影）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(ContentDeletedEvent e) {
        // TODO(S3)：置 deleted = TRUE
        log.debug("忽略内容删除事件 {}（Sprint 3 实现搜索投影）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        // TODO(S3)：置 accepted = TRUE
        log.debug("忽略回答采纳事件 {}（Sprint 3 实现搜索投影）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(VoteCastEvent e) {
        // TODO(S3)：按 newValue - oldValue 调整 score
        log.debug("忽略投票事件 {}（Sprint 3 实现搜索投影）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(BountyOpenedEvent e) {
        // TODO(S3)：置 bounty_open = TRUE
        log.debug("忽略悬赏开启事件 {}（Sprint 3 实现搜索投影）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(BountyAwardedEvent e) {
        // TODO(S3)：置 bounty_open = FALSE
        log.debug("忽略悬赏结算事件 {}（Sprint 3 实现搜索投影）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(BountyClosedEvent e) {
        // TODO(S3)：置 bounty_open = FALSE
        log.debug("忽略悬赏关闭事件 {}（Sprint 3 实现搜索投影）", e.eventId());
    }
}
