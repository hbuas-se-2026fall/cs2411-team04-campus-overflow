package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryAnswers;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryComments;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.DomainException;
import com.campusoverflow.shared.domain.NotFoundException;
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
 * 评论用例测试：验证「问题 id 解析 → 领域校验 → 落库 → 发出站事件」的编排，
 * 以及二级回复的 parentId 语义。领域校验本身由 VoteAndCommentTest 钉死，这里不重复。
 */
class CommentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final InMemoryAnswers answers = new InMemoryAnswers();
    private final InMemoryComments comments = new InMemoryComments();
    private final RecordingEvents events = new RecordingEvents();

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);

    private CommentService service;
    private long questionId;
    private long answerId;

    @BeforeEach
    void setUp() {
        service = new CommentService(comments, questions, answers, events, clock);
        Question question = questions.save(Question.post(ming.userId(), 100L,
                new Title("红黑树插入后为什么必须旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？希望有直观解释，最好能给出一个反例。"),
                Tags.of(List.of("数据结构", "红黑树")), NOW));
        questionId = question.id();
        answerId = answers.save(Answer.submit(question, hong.userId(),
                MarkdownBody.ofAnswer("关键在于黑高这个不变量，仅靠变色无法调整。"), NOW)).id();
    }

    @Test
    @DisplayName("评论问题：落库并发布 CommentPostedEvent，questionId 指向被评论的问题")
    void postCommentOnQuestionPublishesEvent() {
        long commentId = service.post(ming,
                new PostCommentCommand(TargetType.QUESTION, questionId, null, "问得好，我也想知道"));

        List<CommentPostedEvent> posted = events.ofType(CommentPostedEvent.class);
        assertThat(posted).hasSize(1);
        CommentPostedEvent event = posted.get(0);
        assertThat(event.commentId()).isEqualTo(commentId);
        assertThat(event.questionId()).isEqualTo(questionId);
        assertThat(event.authorId()).isEqualTo(ming.userId());
        assertThat(event.body()).isEqualTo("问得好，我也想知道");
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.eventId()).isNotBlank();
        assertThat(comments.findById(commentId)).isPresent();
    }

    @Test
    @DisplayName("回复回答下的评论：事件的 questionId 从回答反查，而不是回答 id")
    void postReplyResolvesQuestionIdFromAnswer() {
        long rootId = service.post(hong,
                new PostCommentCommand(TargetType.ANSWER, answerId, null, "讲得很清楚"));
        long replyId = service.post(ming,
                new PostCommentCommand(TargetType.ANSWER, answerId, rootId, "补充一句：还要考虑黑高"));

        List<CommentPostedEvent> posted = events.ofType(CommentPostedEvent.class);
        assertThat(posted).hasSize(2);
        assertThat(posted.get(1).questionId()).isEqualTo(questionId);
        assertThat(posted.get(1).commentId()).isEqualTo(replyId);
        assertThat(comments.findById(replyId).orElseThrow().parentId()).isEqualTo(rootId);
    }

    @Test
    @DisplayName("只能回复顶层评论：对已有 parentId 的评论再回复被领域层拒绝（QA-1016）")
    void replyToReplyIsRejected() {
        long rootId = service.post(hong, new PostCommentCommand(TargetType.ANSWER, answerId, null, "讲得很清楚"));
        long replyId = service.post(ming,
                new PostCommentCommand(TargetType.ANSWER, answerId, rootId, "补充一句"));

        assertThatThrownBy(() -> service.post(hong,
                new PostCommentCommand(TargetType.ANSWER, answerId, replyId, "再回一层")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((DomainException) e).code())
                .isEqualTo("QA-1016");
        assertThat(events.ofType(CommentPostedEvent.class)).hasSize(2);
    }

    @Test
    @DisplayName("父评论不存在：404 QA-1018（本卡新增的错误码）")
    void missingParentIsNotFound() {
        assertThatThrownBy(() -> service.post(ming,
                new PostCommentCommand(TargetType.QUESTION, questionId, 999L, "回复一个不存在的评论")))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((DomainException) e).code())
                .isEqualTo("QA-1018");
        assertThat(events.ofType(CommentPostedEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("目标不存在：问题 QA-1010 / 回答 QA-1011")
    void missingTargetIsNotFound() {
        assertThatThrownBy(() -> service.post(ming,
                new PostCommentCommand(TargetType.QUESTION, 999L, null, "评论不存在的问题")))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((DomainException) e).code())
                .isEqualTo("QA-1010");

        assertThatThrownBy(() -> service.post(ming,
                new PostCommentCommand(TargetType.ANSWER, 999L, null, "评论不存在的回答")))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((DomainException) e).code())
                .isEqualTo("QA-1011");
    }
}
