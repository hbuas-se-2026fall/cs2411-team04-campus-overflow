package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryVotes;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 补 {@code cast(..., 0)} 撤销分支的测试。
 *
 * <p>{@link AnswerAndVoteServiceTest} 只覆盖了 +1 / 幂等 / 改投 -1 三条路径，撤销这条分支
 * 之前一行测试都没有——它是我新写的应用层逻辑，需要独立钉死「删记录 + 回滚分值 + 事件携带新旧值」。
 * 已有测试类不动。
 */
class VoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final InMemoryVotes votes = new InMemoryVotes();
    private final RecordingEvents events = new RecordingEvents();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1, "小明", Role.STUDENT).withUser(2, "小红", Role.STUDENT);

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);

    private VoteService voteService;
    private long questionId;
    private long answerId;

    @BeforeEach
    void setUp() {
        voteService = new VoteService(votes, questions, answers, events, clock);
        Question q = questions.save(Question.post(ming.userId(), 100L, new Title("红黑树插入后为什么必须旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？希望有直观解释，最好能给出一个反例。"),
                Tags.of(List.of("数据结构", "红黑树")), NOW));
        questionId = q.id();
        Answer a = answers.save(Answer.submit(q, hong.userId(),
                MarkdownBody.ofAnswer("关键在于黑高这个不变量，仅靠变色无法调整。"), NOW));
        answerId = a.id();
    }

    @Test
    @DisplayName("撤销：没有旧票时是空操作，不写库、不发事件、分值不变")
    void revokingNothingIsNoOp() {
        VoteService.VoteResult result = voteService.cast(ming, TargetType.ANSWER, answerId, 0);

        assertThat(result.myVote()).isZero();
        assertThat(result.scoreDelta()).isZero();
        assertThat(votes.find(ming.userId(), TargetType.ANSWER, answerId)).isEmpty();
        assertThat(answers.scoreDeltas).doesNotContainKey(answerId);
        assertThat(events.ofType(VoteCastEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("撤销：已有赞时删除记录、分值回滚 -1、事件带 oldValue=1 与 newValue=0")
    void revokingUpvoteRemovesVoteAndRevertsScore() {
        voteService.cast(ming, TargetType.ANSWER, answerId, 1);
        VoteService.VoteResult result = voteService.cast(ming, TargetType.ANSWER, answerId, 0);

        assertThat(result.myVote()).isZero();
        assertThat(result.scoreDelta()).isEqualTo(-1);
        // 撤销是物理删除，不是把值改成 0（Vote.checkValue 只接受 ±1）
        assertThat(votes.find(ming.userId(), TargetType.ANSWER, answerId)).isEmpty();
        assertThat(answers.scoreDeltas).containsEntry(answerId, 0); // +1 之后再 -1

        List<VoteCastEvent> voteEvents = events.ofType(VoteCastEvent.class);
        assertThat(voteEvents).hasSize(2);
        assertThat(voteEvents.get(1).oldValue()).isEqualTo(1);
        assertThat(voteEvents.get(1).newValue()).isZero();
        assertThat(voteEvents.get(1).voterId()).isEqualTo(ming.userId());
        assertThat(voteEvents.get(1).targetAuthorId()).isEqualTo(hong.userId());
        assertThat(voteEvents.get(1).questionId()).isNotNull();
    }

    @Test
    @DisplayName("撤销：已有踩时分值回滚 +1，因为增量是 -(-1)")
    void revokingDownvoteRevertsScoreByPlusOne() {
        voteService.cast(ming, TargetType.ANSWER, answerId, -1);
        VoteService.VoteResult result = voteService.cast(ming, TargetType.ANSWER, answerId, 0);

        assertThat(result.myVote()).isZero();
        assertThat(result.scoreDelta()).isEqualTo(1);
        assertThat(votes.find(ming.userId(), TargetType.ANSWER, answerId)).isEmpty();
        assertThat(answers.scoreDeltas).containsEntry(answerId, 0); // -1 之后再 +1
    }

    @Test
    @DisplayName("撤销之后再投票，视为全新的一票（旧票已删，走 0 → value 分支）")
    void revoteAfterRevokeStartsFresh() {
        voteService.cast(ming, TargetType.ANSWER, answerId, 1);
        voteService.cast(ming, TargetType.ANSWER, answerId, 0);
        VoteService.VoteResult again = voteService.cast(ming, TargetType.ANSWER, answerId, 1);

        assertThat(again.myVote()).isEqualTo(1);
        assertThat(again.scoreDelta()).isEqualTo(1);
        assertThat(votes.find(ming.userId(), TargetType.ANSWER, answerId)).isPresent();

        List<VoteCastEvent> voteEvents = events.ofType(VoteCastEvent.class);
        assertThat(voteEvents).hasSize(3);
        assertThat(voteEvents.get(2).oldValue()).isZero();
        assertThat(voteEvents.get(2).newValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("撤销：对问题投票同样生效，走 resolve 的 QUESTION 分支与 questions.adjustScore")
    void revokingQuestionVoteUsesQuestionBranch() {
        // 问题由 ming 发布，所以必须由 hong 投，否则撞自投校验 QA-1015
        voteService.cast(hong, TargetType.QUESTION, questionId, 1);
        VoteService.VoteResult result = voteService.cast(hong, TargetType.QUESTION, questionId, 0);

        assertThat(result.myVote()).isZero();
        assertThat(result.scoreDelta()).isEqualTo(-1);
        assertThat(votes.find(hong.userId(), TargetType.QUESTION, questionId)).isEmpty();
        // 走的是 questions.adjustScore，不是 answers.adjustScore
        assertThat(questions.scoreDeltas).containsEntry(questionId, 0);
        assertThat(answers.scoreDeltas).doesNotContainKey(answerId);

        List<VoteCastEvent> voteEvents = events.ofType(VoteCastEvent.class);
        assertThat(voteEvents).hasSize(2);
        assertThat(voteEvents.get(1).targetType()).isEqualTo("QUESTION");
        assertThat(voteEvents.get(1).targetId()).isEqualTo(questionId);
        assertThat(voteEvents.get(1).targetAuthorId()).isEqualTo(ming.userId());
        assertThat(voteEvents.get(1).voterId()).isEqualTo(hong.userId());
        assertThat(voteEvents.get(1).oldValue()).isEqualTo(1);
        assertThat(voteEvents.get(1).newValue()).isZero();
    }
}
