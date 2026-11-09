package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.shared.domain.DomainException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 投票与评论的边界测试（T1-06）。
 * <p>与 {@link VoteAndCommentTest} 的分工：既有测试验证「规则存在」，
 * 本类验证「校验顺序」与「数值边界」——顺序错了错误码就会错，前端提示也会错。
 */
class VoteBoundaryTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");

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
    void voteValueBoundaryIsExactlyPlusOrMinusOne() {
        assertThat(Vote.cast(1, 3, TargetType.ANSWER, 50, 1, NOW).value()).isEqualTo(1);
        assertThat(Vote.cast(1, 3, TargetType.ANSWER, 50, -1, NOW).value()).isEqualTo(-1);
        assertThat(codeOf(() -> Vote.cast(1, 3, TargetType.ANSWER, 50, 0, NOW))).isEqualTo("QA-1014");
        assertThat(codeOf(() -> Vote.cast(1, 3, TargetType.ANSWER, 50, 2, NOW))).isEqualTo("QA-1014");
        assertThat(codeOf(() -> Vote.cast(1, 3, TargetType.ANSWER, 50, -2, NOW))).isEqualTo("QA-1014");
    }

    @Test
    void selfVoteIsCheckedBeforeValue() {
        // 顺序保护：自投且票值非法时，必须报 QA-1015（更具体、更能指导用户），而不是 QA-1014。
        // 顺序反过来，用户看到的是「投票值不合法」，会一直改票值却始终失败。
        assertThat(codeOf(() -> Vote.cast(3, 3, TargetType.ANSWER, 50, 7, NOW))).isEqualTo("QA-1015");
    }

    @Test
    void changeRejectsIllegalValueAndKeepsOldOne() {
        Vote v = Vote.cast(1, 3, TargetType.ANSWER, 50, 1, NOW);
        assertThat(codeOf(() -> v.change(0, NOW.plusSeconds(1)))).isEqualTo("QA-1014");
        // 失败后原值必须保持不变，否则会出现「改投失败但票没了」
        assertThat(v.value()).isEqualTo(1);
    }

    @Test
    void changeReturnsPreviousValueAndUpdatesTimestamp() {
        Vote v = Vote.cast(1, 3, TargetType.ANSWER, 50, 1, NOW);
        Instant later = NOW.plusSeconds(120);
        assertThat(v.change(-1, later)).isEqualTo(1);
        assertThat(v.value()).isEqualTo(-1);
        assertThat(v.updatedAt()).isEqualTo(later);
        // 改回原值也返回旧值，应用层靠这个差值算声誉增减
        assertThat(v.change(1, later.plusSeconds(1))).isEqualTo(-1);
    }

    @Test
    void commentBodyBoundaryIsTwoTo600() {
        assertThat(codeOf(() -> Comment.post(TargetType.ANSWER, 50, 10, 1, null, "短", NOW))).isEqualTo("QA-1003");
        assertThat(Comment.post(TargetType.ANSWER, 50, 10, 1, null, "刚好", NOW).body()).isEqualTo("刚好");
        assertThat(Comment.post(TargetType.ANSWER, 50, 10, 1, null, "字".repeat(600), NOW).body()).hasSize(600);
        assertThat(codeOf(() -> Comment.post(TargetType.ANSWER, 50, 10, 1, null, "字".repeat(601), NOW)))
                .isEqualTo("QA-1003");
    }

    @Test
    void thirdLevelReplyIsRejectedWithItsOwnCode() {
        Comment top = Comment.post(TargetType.ANSWER, 50, 10, 1, null, "请问第二步为什么要变色？", NOW);
        top.assignId(1);
        Comment reply = Comment.post(TargetType.ANSWER, 50, 10, 3, top, "因为要保持黑高一致。", NOW);
        reply.assignId(2);
        // QA-1016 是「层级超限」，与 QA-1017「挂错对象」必须区分开
        assertThat(codeOf(() -> Comment.post(TargetType.ANSWER, 50, 10, 1, reply, "再回复一层试试", NOW)))
                .isEqualTo("QA-1016");
    }

    @Test
    void replyToDifferentTargetIsRejected() {
        Comment top = Comment.post(TargetType.ANSWER, 50, 10, 1, null, "请问第二步为什么要变色？", NOW);
        top.assignId(1);
        assertThat(codeOf(() -> Comment.post(TargetType.QUESTION, 10, 10, 3, top, "挂错位置的回复", NOW)))
                .isEqualTo("QA-1017");
        assertThat(codeOf(() -> Comment.post(TargetType.ANSWER, 51, 10, 3, top, "挂到别的回答下", NOW)))
                .isEqualTo("QA-1017");
    }

    @Test
    void topLevelCommentHasNoParent() {
        Comment top = Comment.post(TargetType.QUESTION, 10, 10, 1, null, "这是一条顶层评论", NOW);
        assertThat(top.parentId()).isNull();
        assertThat(top.id()).isNull();
        assertThat(top.questionId()).isEqualTo(10L);
    }
}
