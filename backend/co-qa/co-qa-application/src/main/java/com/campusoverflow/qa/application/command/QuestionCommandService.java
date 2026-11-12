package com.campusoverflow.qa.application.command;

import com.campusoverflow.identity.api.IdentityApi;
import com.campusoverflow.qa.api.event.ContentDeletedEvent;
import com.campusoverflow.qa.api.event.QuestionEditedEvent;
import com.campusoverflow.qa.api.event.QuestionPostedEvent;
import com.campusoverflow.qa.application.port.ContentRenderer;
import com.campusoverflow.qa.application.port.QuestionListCache;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TagRepository;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.audit.AuditTrail;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：发布 / 编辑 / 删除问题。 */
@Service
public class QuestionCommandService {

    static final int PLAIN_TEXT_FOR_INDEX = 2000;

    private final QuestionRepository questions;
    private final TagRepository tags;
    private final IdentityApi identity;
    private final IntegrationEventPublisher events;
    private final ContentRenderer renderer;
    private final QuestionListCache listCache;
    private final AuditTrail audit;
    private final Clock clock;

    public QuestionCommandService(QuestionRepository questions, TagRepository tags, IdentityApi identity,
                                  IntegrationEventPublisher events, ContentRenderer renderer,
                                  QuestionListCache listCache, AuditTrail audit, Clock clock) {
        this.questions = questions;
        this.tags = tags;
        this.identity = identity;
        this.events = events;
        this.renderer = renderer;
        this.listCache = listCache;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public long post(Actor actor, PostQuestionCommand cmd) {
        Question question = Question.post(actor.userId(), cmd.courseId(), new Title(cmd.title()),
                MarkdownBody.ofQuestion(cmd.body()), Tags.of(cmd.tags()), clock.instant());
        // 仓储适配器内部负责 assignId，这里拿到的是已带 id 的实例
        Question saved = questions.save(question);
        events.publish(new QuestionPostedEvent(EventIds.next(), clock.instant(),
                saved.id(), actor.userId(), saved.courseId(), saved.title().value(),
                renderer.toPlainText(cmd.body(), PLAIN_TEXT_FOR_INDEX), saved.tags().names()));
        tags.adjustUsage(saved.tags().names(), 1);
        listCache.invalidateAll();
        audit.record("QUESTION_POSTED", actor.userId(), "QUESTION", String.valueOf(saved.id()),
                saved.title().value());
        return saved.id();
    }

    @Transactional
    public void edit(Actor actor, long questionId, EditQuestionCommand cmd) {
        Question question = load(questionId);
        boolean actorManagesCourse = identity.canManageCourse(actor.userId(), question.courseId());
        // 先记下旧标签，编辑后按差集调整使用量，避免把没变的标签重复计数
        Set<String> before = new HashSet<>(question.tags().names());
        question.edit(actor, actorManagesCourse, new Title(cmd.title()), MarkdownBody.ofQuestion(cmd.body()),
                Tags.of(cmd.tags()), clock.instant());
        questions.save(question);
        adjustTagUsage(before, new HashSet<>(question.tags().names()));
        events.publish(new QuestionEditedEvent(EventIds.next(), clock.instant(),
                question.id(), question.title().value(),
                renderer.toPlainText(cmd.body(), PLAIN_TEXT_FOR_INDEX), question.tags().names()));
        listCache.invalidateAll();
        audit.record("QUESTION_EDITED", actor.userId(), "QUESTION", String.valueOf(questionId),
                question.title().value());
    }

    @Transactional
    public void delete(Actor actor, long questionId) {
        Question question = load(questionId);
        boolean actorManagesCourse = identity.canManageCourse(actor.userId(), question.courseId());
        question.softDelete(actor, actorManagesCourse, clock.instant());
        questions.save(question);
        tags.adjustUsage(question.tags().names(), -1);
        events.publish(new ContentDeletedEvent(EventIds.next(), clock.instant(),
                TargetType.QUESTION.name(), questionId, questionId));
        listCache.invalidateAll();
        audit.record("QUESTION_DELETED", actor.userId(), "QUESTION", String.valueOf(questionId),
                question.title().value());
    }

    private Question load(long questionId) {
        return questions.findById(questionId)
                .orElseThrow(() -> new NotFoundException("QA-1010", "问题不存在：" + questionId));
    }

    /** 只对真正变化的标签调整使用量：移除的 -1、新增的 +1，两边都有的不动。 */
    private void adjustTagUsage(Set<String> before, Set<String> after) {
        List<String> removed = before.stream().filter(tag -> !after.contains(tag)).toList();
        List<String> added = after.stream().filter(tag -> !before.contains(tag)).toList();
        if (!removed.isEmpty()) {
            tags.adjustUsage(removed, -1);
        }
        if (!added.isEmpty()) {
            tags.adjustUsage(added, 1);
        }
    }
}
