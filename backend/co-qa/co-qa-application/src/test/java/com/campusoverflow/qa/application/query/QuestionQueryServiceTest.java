package com.campusoverflow.qa.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.qa.application.fake.QaTestFixtures.CountingCache;
import com.campusoverflow.qa.application.fake.QaTestFixtures.StubIdentity;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.ViewCounter;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 读路径用例测试：验证编排（缓存命中、摘要渲染、浏览量、评论归组、权限）而非 SQL 本身。
 * 用假 QaReadModel 喂投影行，毫秒级完成，不需要 Spring 容器与数据库。
 */
class QuestionQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");
    private static final long QUESTION_ID = 10L;
    private static final long ANSWER_ID = 20L;

    private final FakeReadModel readModel = new FakeReadModel();
    private final RecordingViewCounter viewCounter = new RecordingViewCounter();
    private final CountingCache cache = new CountingCache();
    private final StubRenderer renderer = new StubRenderer();
    private final StubIdentity identity = new StubIdentity()
            .withUser(1, "小明", Role.STUDENT).withUser(2, "小红", Role.STUDENT).withUser(9, "王老师", Role.TEACHER);

    private final Actor ming = new Actor(1, "小明", Role.STUDENT);

    private QuestionQueryService service;

    @BeforeEach
    void setUp() {
        service = new QuestionQueryService(readModel, identity, renderer, viewCounter, cache);
        readModel.questions.add(new QuestionRow(QUESTION_ID, 1L, 100L, "红黑树插入后为什么必须旋转？",
                "# 红黑树\n\n**变色**为什么不够？", List.of("数据结构", "红黑树"), null, 3, 1, 7, NOW, NOW));
        readModel.answers.put(QUESTION_ID, List.of(
                new AnswerRow(ANSWER_ID, 2L, "关键在于黑高这个不变量。", 5, 9L, NOW, NOW)));
        readModel.comments.put(QUESTION_ID, List.of(
                new CommentRow(101L, "QUESTION", QUESTION_ID, 2L, null, "问得好", NOW),
                new CommentRow(102L, "ANSWER", ANSWER_ID, 1L, null, "同意", NOW)));
    }

    @Test
    @DisplayName("列表：第二次调用直接命中缓存，不再查读模型")
    void listUsesCacheOnSecondCall() {
        PageRequest page = new PageRequest(1, 10);

        PageResult<QuestionSummaryView> first = service.list(null, page);
        PageResult<QuestionSummaryView> second = service.list(null, page);

        assertThat(readModel.listCalls).isEqualTo(1);
        assertThat(cache.puts).isEqualTo(1);
        assertThat(cache.gets).isEqualTo(2);
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("列表：摘要走 renderer 生成纯文本，不留 Markdown 语法")
    void listMapsExcerptThroughRenderer() {
        PageResult<QuestionSummaryView> result = service.list(null, new PageRequest(1, 10));

        QuestionSummaryView view = result.items().get(0);
        assertThat(view.excerpt()).isEqualTo(renderer.toPlainText(readModel.questions.get(0).body(), 160));
        assertThat(view.excerpt()).doesNotContain("#").doesNotContain("**");
        assertThat(view.author().displayName()).isEqualTo("小明");
        assertThat(view.tags()).containsExactly("数据结构", "红黑树");
    }

    @Test
    @DisplayName("列表：空结果不写缓存，避免把空列表缓存 60 秒")
    void listDoesNotCacheEmptyResult() {
        readModel.questions.clear();

        PageResult<QuestionSummaryView> result = service.list(null, new PageRequest(1, 10));

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isZero();
        assertThat(cache.puts).isZero();
    }

    @Test
    @DisplayName("详情：问题不存在抛 QA-4001，且不产生浏览计数")
    void detailThrowsWhenQuestionMissing() {
        assertThatThrownBy(() -> service.detail(999L, ming))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).code())
                .isEqualTo("QA-4001");
        assertThat(viewCounter.ids).isEmpty();
    }

    @Test
    @DisplayName("详情：每次访问把浏览量 +1 交给计数器端口")
    void detailIncrementsViewCounter() {
        service.detail(QUESTION_ID, ming);

        assertThat(viewCounter.ids).containsExactly(QUESTION_ID);
    }

    @Test
    @DisplayName("详情：评论按挂载对象归组，问题评论不串到回答上")
    void detailGroupsCommentsUnderTheirTarget() {
        QuestionDetailView detail = service.detail(QUESTION_ID, ming);

        assertThat(detail.comments()).extracting(CommentView::id).containsExactly(101L);
        assertThat(detail.answers()).hasSize(1);
        assertThat(detail.answers().get(0).comments()).extracting(CommentView::id).containsExactly(102L);
        assertThat(detail.answers().get(0).author().displayName()).isEqualTo("小红");
        assertThat(detail.answers().get(0).endorsedBy().displayName()).isEqualTo("王老师");
        assertThat(detail.course().code()).isEqualTo("CS2001");
    }

    @Test
    @DisplayName("详情：匿名访问没有可执行操作，myVote 为 0")
    void detailReturnsNonePermissionsForAnonymous() {
        QuestionDetailView detail = service.detail(QUESTION_ID, null);

        assertThat(detail.permissions()).isEqualTo(PermissionsView.NONE);
        assertThat(detail.myVote()).isZero();
        assertThat(detail.answers().get(0).myVote()).isZero();
        assertThat(readModel.myVotesQueries).isZero();
    }

    @Test
    @DisplayName("详情：提问者看到自己的 myVote，被采纳的回答标记 accepted")
    void detailMapsMyVoteAndAcceptedAnswer() {
        readModel.questions.set(0, new QuestionRow(QUESTION_ID, 1L, 100L, "红黑树插入后为什么必须旋转？",
                "正文", List.of("数据结构"), ANSWER_ID, 3, 1, 7, NOW, NOW));
        readModel.myVotes.put("QUESTION:" + QUESTION_ID, 1);
        readModel.myVotes.put("ANSWER:" + ANSWER_ID, -1);

        QuestionDetailView detail = service.detail(QUESTION_ID, ming);

        assertThat(detail.myVote()).isEqualTo(1);
        assertThat(detail.answers().get(0).myVote()).isEqualTo(-1);
        assertThat(detail.answers().get(0).accepted()).isTrue();
        assertThat(detail.permissions().canAccept()).isTrue();
    }

    @Test
    @DisplayName("热门标签：limit ≤ 0 直接返回空列表，不打扰读模型")
    void popularTagsIgnoresNonPositiveLimit() {
        assertThat(service.popularTags(0)).isEmpty();
        assertThat(readModel.popularTagsCalls).isZero();
    }

    /** 假读模型：按 courseId 过滤并手写分页，其余查询直接返回预置数据。 */
    static class FakeReadModel implements QaReadModel {

        final List<QuestionRow> questions = new ArrayList<>();
        final Map<Long, List<AnswerRow>> answers = new HashMap<>();
        final Map<Long, List<CommentRow>> comments = new HashMap<>();
        final Map<String, Integer> myVotes = new HashMap<>();
        int listCalls;
        int myVotesQueries;
        int popularTagsCalls;

        @Override
        public PageResult<QuestionRow> listQuestions(Long courseId, PageRequest page) {
            listCalls++;
            List<QuestionRow> filtered = questions.stream()
                    .filter(q -> courseId == null || courseId.equals(q.courseId()))
                    .toList();
            int from = Math.min(page.offset(), filtered.size());
            int to = Math.min(from + page.size(), filtered.size());
            return new PageResult<>(filtered.subList(from, to), filtered.size(), page.page(), page.size());
        }

        @Override
        public Optional<QuestionRow> findQuestion(long questionId) {
            return questions.stream().filter(q -> q.id() == questionId).findFirst();
        }

        @Override
        public List<AnswerRow> findAnswers(long questionId) {
            return answers.getOrDefault(questionId, List.of());
        }

        @Override
        public List<CommentRow> findComments(long questionId) {
            return comments.getOrDefault(questionId, List.of());
        }

        @Override
        public Map<String, Integer> findMyVotes(long voterId, long questionId, Collection<Long> answerIds) {
            myVotesQueries++;
            return myVotes;
        }

        @Override
        public List<TagView> popularTags(int limit) {
            popularTagsCalls++;
            return List.of(new TagView("数据结构", 12));
        }
    }

    /** 渲染替身：去掉 Markdown 记号并截断，便于断言“摘要里没有残留语法”。 */
    static class StubRenderer implements ContentRenderer {

        @Override
        public String toSafeHtml(String markdown) {
            return "<p>" + markdown + "</p>";
        }

        @Override
        public String toPlainText(String markdown, int maxLength) {
            String text = markdown.replaceAll("[#*`_>]", "").replaceAll("\\s+", " ").trim();
            return text.length() <= maxLength ? text : text.substring(0, maxLength);
        }
    }

    /** 浏览量替身：记录被 +1 的问题 id。 */
    static class RecordingViewCounter implements ViewCounter {

        final List<Long> ids = new ArrayList<>();

        @Override
        public void increment(long questionId) {
            ids.add(questionId);
        }
    }
}
