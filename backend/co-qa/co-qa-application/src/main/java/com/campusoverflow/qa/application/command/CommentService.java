package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.CommentPostedEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Comment;
import com.campusoverflow.qa.domain.CommentRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用例：评论与二级回复。评论中的 @昵称 由 Discovery 解析并发送提醒。 */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public CommentService(CommentRepository comments, QuestionRepository questions, AnswerRepository answers,
                          IntegrationEventPublisher events, Clock clock) {
        this.comments = comments;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    /**
     * 发表评论或二级回复。业务校验（正文 2–600 字、只能回复顶层评论、父子须挂同一对象）全在领域层，
     * 这里只解析问题 id（评论总是挂在问题下，即使目标是一条回答）并发出站事件。
     */
    @Transactional
    public long post(Actor actor, PostCommentCommand cmd) {
        long questionId = resolveQuestionId(cmd);

        Comment parent = cmd.parentId() == null ? null
                : comments.findById(cmd.parentId())
                        .orElseThrow(() -> new NotFoundException("QA-1018", "父评论不存在：" + cmd.parentId()));

        Comment comment = Comment.post(cmd.targetType(), cmd.targetId(), questionId, actor.userId(), parent,
                cmd.body(), clock.instant());
        Comment saved = comments.save(comment);

        events.publish(new CommentPostedEvent(EventIds.next(), clock.instant(), saved.id(), questionId,
                actor.userId(), cmd.body()));

        return saved.id();
    }

    private long resolveQuestionId(PostCommentCommand cmd) {
        if (cmd.targetType() == TargetType.QUESTION) {
            Question question = questions.findById(cmd.targetId())
                    .orElseThrow(() -> new NotFoundException("QA-1010", "问题不存在：" + cmd.targetId()));
            return question.id();
        }
        Answer answer = answers.findById(cmd.targetId())
                .orElseThrow(() -> new NotFoundException("QA-1011", "回答不存在：" + cmd.targetId()));
        return answer.questionId();
    }
}
