package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅上游上下文的集成事件（由 OutboxRelay 在事务提交后投递，至少一次语义）。
 * 每个处理器运行在自己的事务中，并通过账本唯一键 / 徽章存在性检查实现幂等。
 *
 * <p><b>批次 2 临时状态</b>：方法体全部为「记录日志 + 不做任何事」的空实现。原因是
 * {@code OutboxRelay.dispatch()} 捕获异常后会 {@code failed.increment()} 并按指数退避推后
 * {@code locked_until}，超过 10 次进死信——任何一次「提问」或「回答」都会让事件链堵在那里。
 * 真正的声誉记账、徽章授予、悬赏结算在 Sprint 3 实现（见各方法上的 {@code TODO(S3)}）。
 */
@Component
public class ReputationEventHandlers {

    private static final Logger log = LoggerFactory.getLogger(ReputationEventHandlers.class);

    private final ReputationAccountRepository accounts;
    private final LedgerRepository ledger;
    private final ReputationLedgerService ledgerService;
    private final BadgeService badges;
    private final BountyService bounties;
    private final Clock clock;

    public ReputationEventHandlers(ReputationAccountRepository accounts, LedgerRepository ledger,
                                   ReputationLedgerService ledgerService, BadgeService badges,
                                   BountyService bounties, Clock clock) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.ledgerService = ledgerService;
        this.badges = badges;
        this.bounties = bounties;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void on(UserRegisteredEvent e) {
        // TODO(S3)：开户——按角色发放初始悬赏积分（ReputationRulebook.initialPoints）并写首条账本
        log.debug("忽略用户注册事件 {}（Sprint 3 实现开户）", e.eventId());
    }

    /** 场景 R-2 的异步后半段：采纳 → 回答者 +15 → 徽章 → 悬赏结算。 */
    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        // TODO(S3)：回答被采纳 +15（自问自答不加分）→ 判定采纳数徽章 → 悬赏结算
        log.debug("忽略回答采纳事件 {}（Sprint 3 实现声誉结算）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(VoteCastEvent e) {
        // TODO(S3)：按 oldValue→newValue 计算增量，应用每日正向上限后记账
        log.debug("忽略投票事件 {}（Sprint 3 实现声誉结算）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        // TODO(S3)：判定「首次回答」类徽章
        log.debug("忽略回答提交事件 {}（Sprint 3 实现徽章判定）", e.eventId());
    }

    @EventListener
    @Transactional
    public void on(AnswerEndorsedEvent e) {
        // TODO(S3)：判定教师认证相关徽章 / 声誉加成
        log.debug("忽略回答认证事件 {}（Sprint 3 实现）", e.eventId());
    }
}
