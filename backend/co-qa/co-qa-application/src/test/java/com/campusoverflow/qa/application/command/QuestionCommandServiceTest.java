package com.campusoverflow.qa.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingAudit;
import com.campusoverflow.qa.application.fake.QaTestFixtures.RecordingEvents;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.domain.DomainException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 应用服务用例测试：验证编排逻辑（事件发布、缓存失效、标签计数、审计、权限入口），
 * 而不是领域规则本身——后者已由 {@code QuestionTest} 与 {@code QuestionBoundaryTest} 覆盖。
 */
class QuestionCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final RecordingEvents events = new RecordingEvents();
    private final CountingCache cache = new CountingCache();
    private final RecordingAudit audit = new RecordingAudit();
    private final RecordingTags tags = new RecordingTags();
    private final StubRenderer renderer = new StubRenderer();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1, "小明", Role.STUDENT).withUser(2, "小红", Role.STUDENT).withUser(9, "王老师", Role.TEACHER)
            .withCourseManager(9);

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);
    private final Actor hong = new Actor(2, "小红", Role.STUDENT);
    private final Actor teacher = new Actor(9, "王老师", Role.TEACHER);

    private QuestionCommandService service;

    @BeforeEach
    void setUp() {
        service = new QuestionCommandService(questions, tags, identity, events, renderer, cache, audit, clock);
    }

    private static PostQuestionCommand postCommand(String title, String body, List<String> tagNames) {
        return new PostQuestionCommand(100L, title, body, tagNames);
    }

    /** 取调用抛出的领域异常的错误码；没抛或抛了别的类型都算失败。 */
    private static String codeOf(Runnable call) {
        try {
            call.run();
        } catch (DomainException e) {
            return e.code();
        } catch (Throwable t) {
            throw new AssertionError("期望领域异常，实际抛出：" + t, t);
        }
        throw new AssertionError("期望抛出领域异常，但调用正常返回了");
    }

    @Test
    @DisplayName("提问：发布 QuestionPosted 事件、失效列表缓存、写审计，并返回持久化后的 id")
    void postPublishesEventAndInvalidatesCache() {
        long id = service.post(ming, postCommand("红黑树插入后为什么必须旋转？",
                "变色为什么不够？希望有直观解释，最好能给出一个反例。", List.of("数据结构", "红黑树")));

        assertThat(id).isPositive();
        List<QuestionPostedEvent> posted = events.ofType(QuestionPostedEvent.class);
        assertThat(posted).hasSize(1);
        QuestionPostedEvent event = posted.get(0);
        assertThat(event.questionId()).isEqualTo(id);
        assertThat(event.authorId()).isEqualTo(ming.userId());
        assertThat(event.courseId()).isEqualTo(100L);
        assertThat(event.title()).isEqualTo("红黑树插入后为什么必须旋转？");
        assertThat(event.tags()).containsExactly("数据结构", "红黑树");
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.occurredAt()).isEqualTo(NOW);

        assertThat(cache.invalidations).isEqualTo(1);
        assertThat(audit.actions).contains("QUESTION_POSTED");
    }

    @Test
    @DisplayName("提问：新问题的标签使用量各 +1")
    void postBumpsTagUsage() {
        service.post(ming, postCommand("红黑树插入后为什么必须旋转？",
                "变色为什么不够？希望有直观解释，最好能给出一个反例。", List.of("数据结构", "红黑树")));

        assertThat(tags.deltas()).containsEntry("数据结构", 1).containsEntry("红黑树", 1);
    }

    @Test
    @DisplayName("提问：正文超长时列表摘要按 2000 字截断")
    void postTruncatesPlainTextForIndex() {
        String longBody = "红".repeat(5000);
        service.post(ming, postCommand("一个足够长的问题标题", longBody, List.of("数据结构")));

        assertThat(renderer.lastMaxLength).isEqualTo(QuestionCommandService.PLAIN_TEXT_FOR_INDEX);
    }

    @Test
    @DisplayName("编辑：非作者且非课程管理者被拒绝，错误码 QA-3002")
    void editRejectsStranger() {
        long id = service.post(ming, postCommand("红黑树插入后为什么必须旋转？",
                "变色为什么不够？希望有直观解释，最好能给出一个反例。", List.of("数据结构")));

        assertThatThrownBy(() -> service.edit(hong, id,
                new EditQuestionCommand("换个标题试试看这个能不能改", "随便改点正文内容吧，反正这段文字够长能通过校验。", List.of("数据结构"))))
                .isInstanceOf(ForbiddenException.class);
        assertThat(codeOf(() -> service.edit(hong, id,
                new EditQuestionCommand("换个标题试试看这个能不能改", "随便改点正文内容吧，反正这段文字够长能通过校验。", List.of("数据结构")))))
                .isEqualTo("QA-3002");
    }

    @Test
    @DisplayName("编辑：只对真正变化的标签调整使用量，未变的标签不重复计数")
    void editShiftsTagUsage() {
        long id = service.post(ming, postCommand("红黑树插入后为什么必须旋转？",
                "变色为什么不够？希望有直观解释，最好能给出一个反例。", List.of("数据结构", "红黑树")));
        tags.clear();

        service.edit(ming, id, new EditQuestionCommand("红黑树插入后的旋转规则是什么？",
                "变色为什么不够？希望有直观解释，最好能给出一个反例。", List.of("数据结构", "算法")));

        // 数据结构 前后都有 → 不动；红黑树 被移除 → -1；算法 新增 → +1
        assertThat(tags.deltas()).containsOnlyKeys("红黑树", "算法");
        assertThat(tags.deltas()).containsEntry("红黑树", -1).containsEntry("算法", 1);

        List<QuestionEditedEvent> edited = events.ofType(QuestionEditedEvent.class);
        assertThat(edited).hasSize(1);
        assertThat(edited.get(0).title()).isEqualTo("红黑树插入后的旋转规则是什么？");
        assertThat(edited.get(0).tags()).containsExactly("数据结构", "算法");
        assertThat(audit.actions).contains("QUESTION_EDITED");
    }

    @Test
    @DisplayName("删除：已有回答的问题，提问者本人不能删（QA-1013），课程管理者可以")
    void deleteWithAnswersIsRejected() {
        long id = service.post(ming, postCommand("红黑树插入后为什么必须旋转？",
                "变色为什么不够？希望有直观解释，最好能给出一个反例。", List.of("数据结构")));
        // 直接改写成「已有 1 个回答」的状态：领域规则只读 answerCount，不需要真的存回答
        Question answered = Question.reconstitute(id, ming.userId(), 100L,
                new Title("红黑树插入后为什么必须旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？希望有直观解释，最好能给出一个反例。"),
                Tags.of(List.of("数据结构")), null, 0, 1, 0, NOW, NOW, null, 0);
        questions.save(answered);

        assertThat(codeOf(() -> service.delete(ming, id))).isEqualTo("QA-1013");

        service.delete(teacher, id);
        assertThat(questions.findById(id).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("删除：发布 ContentDeleted 事件（targetType=QUESTION），标签使用量 -1")
    void deletePublishesContentDeletedEvent() {
        long id = service.post(ming, postCommand("红黑树插入后为什么必须旋转？",
                "变色为什么不够？希望有直观解释，最好能给出一个反例。", List.of("数据结构", "红黑树")));
        tags.clear();

        service.delete(ming, id);

        List<ContentDeletedEvent> deleted = events.ofType(ContentDeletedEvent.class);
        assertThat(deleted).hasSize(1);
        assertThat(deleted.get(0).targetType()).isEqualTo(TargetType.QUESTION.name());
        assertThat(deleted.get(0).targetId()).isEqualTo(id);
        assertThat(deleted.get(0).questionId()).isEqualTo(id);
        assertThat(tags.deltas()).containsEntry("数据结构", -1).containsEntry("红黑树", -1);
        assertThat(audit.actions).contains("QUESTION_DELETED");
    }

    @Test
    @DisplayName("不存在的问题：编辑与删除都报 QA-1010")
    void missingQuestionIsRejected() {
        assertThatThrownBy(() -> service.edit(ming, 999L,
                new EditQuestionCommand("换个标题试试看这个能不能改", "随便改点正文内容吧，反正这段文字够长能通过校验。", List.of("数据结构"))))
                .isInstanceOf(NotFoundException.class);
        assertThat(codeOf(() -> service.delete(ming, 999L))).isEqualTo("QA-1010");
    }

    /** 记录每次 {@code adjustUsage} 的增量，便于断言「谁加了谁减了」。 */
    static class RecordingTags implements TagRepository {
        private final Map<String, Integer> deltas = new LinkedHashMap<>();

        @Override
        public void adjustUsage(Collection<String> tagNames, int delta) {
            tagNames.forEach(name -> deltas.merge(name, delta, Integer::sum));
        }

        Map<String, Integer> deltas() {
            return deltas;
        }

        void clear() {
            deltas.clear();
        }
    }

    /** 渲染端口替身：只记录最近一次的截断长度，正文内容不参与本层断言。 */
    static class StubRenderer implements ContentRenderer {
        int lastMaxLength;

        @Override
        public String toSafeHtml(String markdown) {
            return markdown;
        }

        @Override
        public String toPlainText(String markdown, int maxLength) {
            lastMaxLength = maxLength;
            return markdown.length() <= maxLength ? markdown : markdown.substring(0, maxLength);
        }
    }
}
