package com.campusoverflow.qa.application.command;

import com.campusoverflow.qa.api.event.VoteCastEvent;
import com.campusoverflow.qa.domain.Answer;
import com.campusoverflow.qa.domain.AnswerRepository;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.QuestionRepository;
import com.campusoverflow.qa.domain.TargetType;
import com.campusoverflow.qa.domain.Vote;
import com.campusoverflow.qa.domain.VoteRepository;
import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.NotFoundException;
import com.campusoverflow.shared.event.EventIds;
import com.campusoverflow.shared.event.IntegrationEventPublisher;
import com.campusoverflow.shared.security.Actor;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用例：投票 / 改投 / 撤销（value ∈ {-1, 0, 1}，0 表示撤销）。
 * 发布的事件同时携带旧值与新值，下游据此计算声誉增量。
 *
 * <p>已删除的内容不接受新投票（{@code QA-1021}），但**放行撤销**：撤销走 {@code applyScore(-oldValue)}
 * 的减分路径，拦住它会让用户删掉问题后那张票永远撤不回来，{@code qa_vote} 与声誉账户从此不一致。
 */
@Service
public class VoteService {

    private final VoteRepository votes;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final IntegrationEventPublisher events;
    private final Clock clock;

    public VoteService(VoteRepository votes, QuestionRepository questions, AnswerRepository answers,
                       IntegrationEventPublisher events, Clock clock) {
        this.votes = votes;
        this.questions = questions;
        this.answers = answers;
        this.events = events;
        this.clock = clock;
    }

    public record VoteResult(int myVote, int scoreDelta) {
    }

    private record Target(long authorId, long questionId, boolean deleted) {
    }

    @Transactional
    public VoteResult cast(Actor actor, TargetType type, long targetId, int value) {
        Target target = resolve(type, targetId);
        if (value != 0 && target.deleted()) {
            throw new BusinessRuleException("QA-1021", "内容已删除，不能再投票");
        }
        Optional<Vote> existing = votes.find(actor.userId(), type, targetId);
        int oldValue = existing.map(Vote::value).orElse(0);

        if (value == 0) {
            // 撤销：领域对象只接受 ±1，撤销只能删记录
            if (existing.isEmpty()) {
                return new VoteResult(0, 0);
            }
            votes.delete(existing.get());
            applyScore(type, targetId, -oldValue);
            publish(actor, type, targetId, target, oldValue, 0);
            return new VoteResult(0, -oldValue);
        }

        if (existing.isEmpty()) {
            votes.save(Vote.cast(actor.userId(), target.authorId(), type, targetId, value, clock.instant()));
            applyScore(type, targetId, value);
            publish(actor, type, targetId, target, 0, value);
            return new VoteResult(value, value);
        }

        if (oldValue == value) {
            // 幂等：重复投同一票既不写库也不发事件，否则声誉会被重复结算
            return new VoteResult(value, 0);
        }

        existing.get().change(value, clock.instant());
        votes.save(existing.get());
        applyScore(type, targetId, value - oldValue);
        publish(actor, type, targetId, target, oldValue, value);
        return new VoteResult(value, value - oldValue);
    }

    private Target resolve(TargetType type, long targetId) {
        if (type == TargetType.QUESTION) {
            Question question = questions.findById(targetId)
                    .orElseThrow(() -> new NotFoundException("QA-4001", "问题不存在：" + targetId));
            return new Target(question.authorId(), question.id(), question.isDeleted());
        }
        Answer answer = answers.findById(targetId)
                .orElseThrow(() -> new NotFoundException("QA-4002", "回答不存在：" + targetId));
        return new Target(answer.authorId(), answer.questionId(), answer.isDeleted());
    }

    /** 计数器必须原子增量维护，不能读出来再写回去（AGENTS.md 技术红线第 6 条）。 */
    private void applyScore(TargetType type, long targetId, int delta) {
        if (delta == 0) {
            return;
        }
        if (type == TargetType.QUESTION) {
            questions.adjustScore(targetId, delta);
        } else {
            answers.adjustScore(targetId, delta);
        }
    }

    private void publish(Actor actor, TargetType type, long targetId, Target target, int oldValue, int newValue) {
        events.publish(new VoteCastEvent(EventIds.next(), clock.instant(), type.name(), targetId,
                target.authorId(), actor.userId(), target.questionId(), oldValue, newValue));
    }
}
