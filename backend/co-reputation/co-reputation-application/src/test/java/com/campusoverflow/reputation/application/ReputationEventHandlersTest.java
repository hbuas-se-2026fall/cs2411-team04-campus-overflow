package com.campusoverflow.reputation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.identity.api.event.UserRegisteredEvent;
import com.campusoverflow.qa.api.event.AnswerAcceptedEvent;
import com.campusoverflow.qa.api.event.AnswerEndorsedEvent;
import com.campusoverflow.qa.api.event.AnswerSubmittedEvent;
import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.reputation.api.event.BadgeAwardedEvent;
import com.campusoverflow.reputation.api.event.ReputationChangedEvent;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryAccounts;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryBadges;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.InMemoryLedger;
import com.campusoverflow.reputation.application.fake.ReputationTestFixtures.RecordingEvents;
import com.campusoverflow.reputation.domain.BadgeType;
import com.campusoverflow.reputation.domain.ReputationAccount;
import com.campusoverflow.reputation.domain.ReputationReason;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 声誉事件处理器的应用层测试。全部手写替身、不启动 Spring：这 5 个处理器只依赖端口接口，
 * 因此可以在毫秒级验证「幂等、自问自答不加分、每日上限、开户积分」四条业务规则。
 *
 * <p>{@code BountyService} 传 {@code null}：采纳结算的最后一步（悬赏结算）归 {@code #47}，
 * 本卡的处理器有意不调用它，传入 null 可让「误调用」立刻炸成 NPE 而不是静默通过。
 */
class ReputationEventHandlersTest {

    private static final Instant NOW = Instant.parse("2026-10-08T02:00:00Z");
    private static final long ASKER = 1L;
    private static final long ANSWERER = 2L;

    private InMemoryAccounts accounts;
    private InMemoryLedger ledger;
    private InMemoryBadges badges;
    private RecordingEvents events;
    private ReputationEventHandlers handlers;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccounts();
        ledger = new InMemoryLedger();
        badges = new InMemoryBadges();
        events = new RecordingEvents();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        BadgeService badgeService = new BadgeService(badges, events, clock);
        ReputationLedgerService ledgerService =
                new ReputationLedgerService(accounts, ledger, badgeService, events, clock);
        handlers = new ReputationEventHandlers(accounts, ledger, ledgerService, badgeService, null, clock);
    }

    // ---------------- 开户 ----------------

    @Test
    @DisplayName("开户：教师 1000 分、学生 0 分，且声誉从 0 起")
    void openingAccountGrantsRoleBasedPoints() {
        handlers.on(registered("e-teacher", 9L, Role.TEACHER));
        handlers.on(registered("e-student", 1L, Role.STUDENT));

        assertThat(accounts.findByUserId(9L).orElseThrow().availablePoints()).isEqualTo(1000);
        assertThat(accounts.findByUserId(1L).orElseThrow().availablePoints()).isZero();
        assertThat(accounts.findByUserId(9L).orElseThrow().reputation()).isZero();
        // 初始积分是悬赏积分，不是声誉，因此不该写进账本。
        assertThat(ledger.entries).isEmpty();
    }

    @Test
    @DisplayName("开户幂等：同一用户重复投递注册事件，不会重置已有余额")
    void openingAccountIsIdempotent() {
        handlers.on(registered("e1", 9L, Role.TEACHER));
        ReputationAccount afterOpen = accounts.findByUserId(9L).orElseThrow();
        afterOpen.freezePoints(300);
        accounts.save(afterOpen);

        handlers.on(registered("e2", 9L, Role.TEACHER));

        ReputationAccount reloaded = accounts.findByUserId(9L).orElseThrow();
        assertThat(reloaded.availablePoints()).isEqualTo(700);
        assertThat(reloaded.frozenPoints()).isEqualTo(300);
    }

    // ---------------- 采纳 ----------------

    @Test
    @DisplayName("采纳：回答者 +15，写一条 ANSWER_ACCEPTED 流水，并发声誉变更事件")
    void acceptanceAwardsFifteenToAnswerer() {
        handlers.on(registered("e-open", ANSWERER, Role.STUDENT));

        handlers.on(accepted("e-accept", ANSWERER, ASKER));

        assertThat(accounts.findByUserId(ANSWERER).orElseThrow().reputation()).isEqualTo(15);
        assertThat(ledger.entries).singleElement().satisfies(entry -> {
            assertThat(entry.delta()).isEqualTo(15);
            assertThat(entry.reason()).isEqualTo(ReputationReason.ANSWER_ACCEPTED);
            assertThat(entry.eventId()).isEqualTo("e-accept");
        });
        assertThat(events.of(ReputationChangedEvent.class)).singleElement()
                .satisfies(e -> assertThat(e.delta()).isEqualTo(15));
    }

    @Test
    @DisplayName("采纳幂等：同一 eventId 投递两次，只记一次账")
    void acceptanceIsIdempotent() {
        handlers.on(registered("e-open", ANSWERER, Role.STUDENT));

        handlers.on(accepted("e-accept", ANSWERER, ASKER));
        handlers.on(accepted("e-accept", ANSWERER, ASKER));

        assertThat(ledger.entries).hasSize(1);
        assertThat(accounts.findByUserId(ANSWERER).orElseThrow().reputation()).isEqualTo(15);
        assertThat(events.of(ReputationChangedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("自问自答被采纳：作者声誉不变，也不写流水")
    void selfAcceptanceDoesNotChangeReputation() {
        handlers.on(registered("e-open", ASKER, Role.STUDENT));

        handlers.on(accepted("e-self", ASKER, ASKER));

        assertThat(accounts.findByUserId(ASKER).orElseThrow().reputation()).isZero();
        assertThat(ledger.entries).isEmpty();
        assertThat(events.of(ReputationChangedEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("采纳数达到阈值时授予「初露锋芒」")
    void firstAcceptanceGrantsBadge() {
        handlers.on(registered("e-open", ANSWERER, Role.STUDENT));

        handlers.on(accepted("e-accept", ANSWERER, ASKER));

        assertThat(badges.has(ANSWERER, BadgeType.FIRST_ACCEPTED)).isTrue();
        assertThat(events.of(BadgeAwardedEvent.class)).singleElement()
                .satisfies(e -> assertThat(e.badgeCode()).isEqualTo(BadgeType.FIRST_ACCEPTED.name()));
    }

    // ---------------- 投票 ----------------

    @Test
    @DisplayName("投票：被赞 +10，reason 按 targetType 取 ANSWER_VOTE")
    void upvoteAwardsTenPoints() {
        handlers.on(registered("e-open", ANSWERER, Role.STUDENT));

        handlers.on(vote("e-vote", "ANSWER", 100L, ANSWERER, 0, 1));

        assertThat(accounts.findByUserId(ANSWERER).orElseThrow().reputation()).isEqualTo(10);
        assertThat(ledger.entries).singleElement()
                .satisfies(e -> assertThat(e.reason()).isEqualTo(ReputationReason.ANSWER_VOTE));
    }

    @Test
    @DisplayName("投票：问题维度被赞，reason 为 QUESTION_VOTE")
    void questionUpvoteUsesQuestionReason() {
        handlers.on(registered("e-open", ANSWERER, Role.STUDENT));

        handlers.on(vote("e-vote", "QUESTION", 10L, ANSWERER, 0, 1));

        assertThat(ledger.entries).singleElement()
                .satisfies(e -> assertThat(e.reason()).isEqualTo(ReputationReason.QUESTION_VOTE));
    }

    @Test
    @DisplayName("改投：赞改踩按差值 −12 计分")
    void changingVoteAppliesDelta() {
        handlers.on(registered("e-open", ANSWERER, Role.STUDENT));
        handlers.on(vote("e-vote-1", "ANSWER", 100L, ANSWERER, 0, 1));

        handlers.on(vote("e-vote-2", "ANSWER", 100L, ANSWERER, 1, -1));

        assertThat(accounts.findByUserId(ANSWERER).orElseThrow().reputation()).isEqualTo(-2);
        assertThat(ledger.entries).extracting(e -> e.delta()).containsExactly(10, -12);
    }

    @Test
    @DisplayName("每日上限：当日正向已达 200 时，被赞增量被削到 0")
    void dailyCapTruncatesPositiveVoteGain() {
        handlers.on(registered("e-open", ANSWERER, Role.STUDENT));
        // 20 次 +10 = 200，正好打满当日上限。
        for (int i = 0; i < 20; i++) {
            handlers.on(vote("e-fill-" + i, "ANSWER", 100L + i, ANSWERER, 0, 1));
        }
        assertThat(accounts.findByUserId(ANSWERER).orElseThrow().reputation()).isEqualTo(200);

        handlers.on(vote("e-over", "ANSWER", 999L, ANSWERER, 0, 1));

        assertThat(accounts.findByUserId(ANSWERER).orElseThrow().reputation()).isEqualTo(200);
        assertThat(ledger.countByUserAndReason(ANSWERER, ReputationReason.ANSWER_VOTE)).isEqualTo(20);
    }

    @Test
    @DisplayName("每日上限不影响负向投票：被踩照常 −2")
    void dailyCapDoesNotAffectDownvotes() {
        handlers.on(registered("e-open", ANSWERER, Role.STUDENT));
        for (int i = 0; i < 20; i++) {
            handlers.on(vote("e-fill-" + i, "ANSWER", 100L + i, ANSWERER, 0, 1));
        }

        handlers.on(vote("e-down", "ANSWER", 999L, ANSWERER, 0, -1));

        assertThat(accounts.findByUserId(ANSWERER).orElseThrow().reputation()).isEqualTo(198);
    }

    @Test
    @DisplayName("投票幂等：同一 eventId 投递两次只记一次")
    void voteIsIdempotent() {
        handlers.on(registered("e-open", ANSWERER, Role.STUDENT));

        handlers.on(vote("e-vote", "ANSWER", 100L, ANSWERER, 0, 1));
        handlers.on(vote("e-vote", "ANSWER", 100L, ANSWERER, 0, 1));

        assertThat(ledger.entries).hasSize(1);
        assertThat(accounts.findByUserId(ANSWERER).orElseThrow().reputation()).isEqualTo(10);
    }

    // ---------------- 徽章 ----------------

    @Test
    @DisplayName("首个回答授「初来乍到」，重复投递不重复授予")
    void firstAnswerBadgeIsGrantedOnce() {
        handlers.on(submitted("e-answer", ANSWERER));
        handlers.on(submitted("e-answer", ANSWERER));

        assertThat(badges.awards).singleElement()
                .satisfies(a -> assertThat(a.type()).isEqualTo(BadgeType.FIRST_ANSWER));
    }

    @Test
    @DisplayName("教师认证授「名师认可」，重复投递不重复授予")
    void endorsedBadgeIsGrantedOnce() {
        handlers.on(endorsed("e-endorse", ANSWERER));
        handlers.on(endorsed("e-endorse", ANSWERER));

        assertThat(badges.awards).singleElement()
                .satisfies(a -> assertThat(a.type()).isEqualTo(BadgeType.TEACHER_ENDORSED));
    }

    // ---------------- 构造事件的helper ----------------

    private static UserRegisteredEvent registered(String eventId, long userId, Role role) {
        return new UserRegisteredEvent(eventId, NOW, userId, "用户" + userId, role);
    }

    private static AnswerAcceptedEvent accepted(String eventId, long answerAuthorId, long questionAuthorId) {
        return new AnswerAcceptedEvent(eventId, NOW, 100L, 10L, answerAuthorId, questionAuthorId, questionAuthorId,
                "红黑树插入后如何判断旋转方向？");
    }

    private static VoteCastEvent vote(String eventId, String targetType, long targetId, long authorId,
                                      int oldValue, int newValue) {
        return new VoteCastEvent(eventId, NOW, targetType, targetId, authorId, 3L, 10L, oldValue, newValue);
    }

    private static AnswerSubmittedEvent submitted(String eventId, long answerAuthorId) {
        return new AnswerSubmittedEvent(eventId, NOW, 100L, 10L, answerAuthorId, ASKER, "红黑树插入后如何判断旋转方向？");
    }

    private static AnswerEndorsedEvent endorsed(String eventId, long answerAuthorId) {
        return new AnswerEndorsedEvent(eventId, NOW, 100L, 10L, answerAuthorId, 9L, "红黑树插入后如何判断旋转方向？");
    }
}
