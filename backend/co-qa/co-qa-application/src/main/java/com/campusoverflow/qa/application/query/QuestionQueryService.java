package com.campusoverflow.qa.application.query;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.identity.api.UserSummary;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QaReadModel;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.application.port.ViewCounter;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.page.PageRequest;
import com.campusoverflow.shared.page.PageResult;
import com.campusoverflow.shared.security.Actor;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 问答查询用例（读路径）。 */
@Service
@Transactional(readOnly = true)
public class QuestionQueryService {

    private static final int EXCERPT_LENGTH = 160;

    private final QaReadModel readModel;
    private final IdentityApi identity;
    private final ContentRenderer renderer;
    private final ViewCounter viewCounter;
    private final QuestionListCache cache;

    public QuestionQueryService(QaReadModel readModel, IdentityApi identity, ContentRenderer renderer,
                                ViewCounter viewCounter, QuestionListCache cache) {
        this.readModel = readModel;
        this.identity = identity;
        this.renderer = renderer;
        this.viewCounter = viewCounter;
        this.cache = cache;
    }

    /** 问题列表：Cache-Aside，空结果不写缓存，作者信息一次批量查避免 N+1。 */
    public PageResult<QuestionSummaryView> list(Long courseId, PageRequest page) {
        Optional<PageResult<QuestionSummaryView>> cached = cache.get(courseId, page);
        if (cached.isPresent()) {
            return cached.get();
        }

        PageResult<QuestionRow> rows = readModel.listQuestions(courseId, page);
        if (rows.items().isEmpty()) {
            return PageResult.empty(page.page(), page.size());
        }

        Set<Long> authorIds = rows.items().stream().map(QuestionRow::authorId).collect(Collectors.toSet());
        Map<Long, UserSummary> authors = identity.findUsers(authorIds);

        PageResult<QuestionSummaryView> result = rows.map(row -> new QuestionSummaryView(
                row.id(),
                row.title(),
                renderer.toPlainText(row.body(), EXCERPT_LENGTH),
                row.tags(),
                row.courseId(),
                AuthorView.of(row.authorId(), authors.get(row.authorId())),
                row.score(),
                row.answerCount(),
                row.viewCount(),
                row.acceptedAnswerId() != null,
                row.createdAt()));

        cache.put(courseId, page, result);
        return result;
    }

    /** 问题详情：一次装配问题、回答、评论、课程与权限；作者信息一次批量查避免 N+1。 */
    public QuestionDetailView detail(long questionId, Actor actorOrNull) {
        QuestionRow q = readModel.findQuestion(questionId)
                .orElseThrow(() -> new NotFoundException("QA-1010", "问题不存在：" + questionId));

        // 浏览计数：先写 Redis，定时批量落库（不阻塞读路径）
        viewCounter.increment(questionId);

        List<AnswerRow> answerRows = readModel.findAnswers(questionId);
        List<CommentRow> commentRows = readModel.findComments(questionId);

        Set<Long> authorIds = new HashSet<>();
        authorIds.add(q.authorId());
        answerRows.forEach(a -> authorIds.add(a.authorId()));
        answerRows.stream().filter(a -> a.endorsedBy() != null).forEach(a -> authorIds.add(a.endorsedBy()));
        commentRows.forEach(c -> authorIds.add(c.authorId()));
        Map<Long, UserSummary> authors = identity.findUsers(authorIds);

        List<Long> answerIds = answerRows.stream().map(AnswerRow::id).toList();
        Map<String, Integer> myVotes = actorOrNull == null
                ? Map.of()
                : readModel.findMyVotes(actorOrNull.userId(), questionId, answerIds);
        int myVote = myVotes.getOrDefault("QUESTION:" + questionId, 0);

        CourseRefView course = q.courseId() == null ? null
                : identity.findCourse(q.courseId())
                        .map(c -> new CourseRefView(c.id(), c.code(), c.name()))
                        .orElse(null);

        boolean canManage = actorOrNull != null && identity.canManageCourse(actorOrNull.userId(), q.courseId());
        boolean isAuthor = actorOrNull != null && actorOrNull.is(q.authorId());
        PermissionsView permissions = actorOrNull == null
                ? PermissionsView.NONE
                : new PermissionsView(
                        isAuthor || canManage,   // canEdit
                        isAuthor || canManage,   // canDelete（领域层还会拦「已有回答」）
                        isAuthor || canManage,   // canAccept
                        canManage);              // canManage

        List<AnswerView> answers = answerRows.stream().map(a -> new AnswerView(
                a.id(),
                a.body(),
                renderer.toSafeHtml(a.body()),
                AuthorView.of(a.authorId(), authors.get(a.authorId())),
                a.score(),
                Objects.equals(q.acceptedAnswerId(), a.id()),
                a.endorsedBy() == null ? null : AuthorView.of(a.endorsedBy(), authors.get(a.endorsedBy())),
                myVotes.getOrDefault("ANSWER:" + a.id(), 0),
                actorOrNull != null && (actorOrNull.is(a.authorId()) || canManage),
                a.createdAt(),
                a.updatedAt(),
                commentsOf(commentRows, "ANSWER", a.id(), authors))).toList();

        return new QuestionDetailView(
                q.id(), q.title(), q.body(), renderer.toSafeHtml(q.body()), q.tags(),
                course, AuthorView.of(q.authorId(), authors.get(q.authorId())),
                q.score(), q.answerCount(), q.viewCount(), q.acceptedAnswerId(), myVote,
                q.createdAt(), q.updatedAt(), permissions,
                commentsOf(commentRows, "QUESTION", questionId, authors), answers);
    }

    /** 热门标签：limit ≤ 0 时显式返回空列表，避免把无意义参数透传到 SQL。 */
    public List<TagView> popularTags(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return readModel.popularTags(limit);
    }

    /** 按挂载对象过滤评论：CommentRow.targetType 是字符串（"QUESTION" / "ANSWER"），不是枚举。 */
    private List<CommentView> commentsOf(List<CommentRow> all, String targetType, long targetId,
                                         Map<Long, UserSummary> authors) {
        return all.stream()
                .filter(c -> targetType.equals(c.targetType()) && c.targetId() == targetId)
                .map(c -> new CommentView(c.id(), AuthorView.of(c.authorId(), authors.get(c.authorId())),
                        c.parentId(), c.body(), c.createdAt()))
                .toList();
    }
}
