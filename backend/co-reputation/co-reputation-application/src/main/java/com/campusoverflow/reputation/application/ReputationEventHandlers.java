package com.campusoverflow.reputation.application;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.domain.BadgePolicy;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.LedgerRepository;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationAccountRepository;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.reputation.domain.ReputationRulebook;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅上游上下文的集成事件（由 OutboxRelay 在事务提交后投递，至少一次语义）。
 * 每个处理器运行在自己的事务中，并通过账本唯一键 / 徽章存在性检查实现幂等。
 *
 * <p>三条红线在本类中的落点：
 * <ul>
 *   <li>幂等（第 4 条）：记账一律经 {@link ReputationLedgerService#record}，它按
 *       {@code (event_id, user_id, reason)} 查重；徽章一律经 {@link BadgeService#grant}，它按
 *       {@code (user_id, type)} 查重。</li>
 *   <li>计数器原子更新（第 6 条）：本类不读账户再回写整行，声誉的加法只在
 *       {@link ReputationLedgerService#record} 内部发生。</li>
 *   <li>事件在保存之后、同一事务内发布（第 3 条）：各方法均为 {@code @Transactional}，发布链为
 *       {@code record → ReputationChangedEvent} 与 {@code grant → BadgeAwardedEvent}。</li>
 * </ul>
 *
 * <p><b>悬赏结算</b>：采纳事件在同一事务内调用 {@code BountyService.settleOnAccept}；
 * 重复事件由悬赏终态与账本唯一键共同保证幂等，发起人自己的回答被采纳时关闭悬赏并退款。
 */
@Component
public class ReputationEventHandlers {

    /** {@code VoteCastEvent.targetType} 取这两个值；用字面量而非 qa 的 {@code TargetType} 枚举，避免跨上下文 import domain。 */
    private static final String TARGET_QUESTION = "QUESTION";
    private static final String TARGET_ANSWER = "ANSWER";

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

    /** 开户：声誉从 0 起，悬赏积分按角色发放。账户已存在则忽略（幂等）。 */
    @EventListener
    @Transactional
    public void on(UserRegisteredEvent e) {
        if (accounts.findByUserId(e.userId()).isPresent()) {
            return;
        }
        // 初始积分是悬赏积分（availablePoints），不是声誉，所以不进账本——
        // 否则会破坏「声誉 = 流水之和」的对账不变量 FF-8。
        accounts.save(ReputationAccount.open(e.userId(), ReputationRulebook.initialPoints(e.role())));
    }

    /** 场景 R-2 的异步后半段：采纳 → 回答者 +15 → 徽章（悬赏结算见类注释，归 #47）。 */
    @EventListener
    @Transactional
    public void on(AnswerAcceptedEvent e) {
        int delta = ReputationRulebook.acceptedDelta(e.answerAuthorId(), e.questionAuthorId());
        boolean recorded = ledgerService.record(e.answerAuthorId(), e.eventId(), ReputationReason.ANSWER_ACCEPTED,
                delta, "ANSWER", e.answerId());
        bounties.settleOnAccept(e.questionId(), e.answerId(), e.answerAuthorId(), e.eventId());
        if (!recorded) {
            // 重复事件，或自问自答（delta = 0）——两种情况都不该再判徽章。
            return;
        }
        long acceptedCount = ledger.countByUserAndReason(e.answerAuthorId(), ReputationReason.ANSWER_ACCEPTED);
        BadgePolicy.forAcceptedCount(acceptedCount).forEach(b -> badges.grant(e.answerAuthorId(), b));
    }

    @EventListener
    @Transactional
    public void on(VoteCastEvent e) {
        ReputationReason reason = switch (e.targetType()) {
            case TARGET_QUESTION -> ReputationReason.QUESTION_VOTE;
            case TARGET_ANSWER -> ReputationReason.ANSWER_VOTE;
            default -> null;
        };
        if (reason == null) {
            return;
        }
        int delta = ReputationRulebook.voteDelta(e.oldValue(), e.newValue());
        if (delta > 0) {
            int gainToday = ledger.sumPositiveVoteDeltaSince(e.targetAuthorId(), utcDayStart());
            delta = ReputationRulebook.applyDailyCap(delta, gainToday);
        }
        ledgerService.record(e.targetAuthorId(), e.eventId(), reason, delta, e.targetType(), e.targetId());
    }

    @EventListener
    @Transactional
    public void on(AnswerSubmittedEvent e) {
        badges.grant(e.answerAuthorId(), BadgeType.FIRST_ANSWER);
    }

    @EventListener
    @Transactional
    public void on(AnswerEndorsedEvent e) {
        badges.grant(e.answerAuthorId(), BadgeType.TEACHER_ENDORSED);
    }

    /** 每日上限按 UTC 自然日计（{@code ReputationRulebook} 类注释：「每人每个自然日（UTC）」）。 */
    private Instant utcDayStart() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
