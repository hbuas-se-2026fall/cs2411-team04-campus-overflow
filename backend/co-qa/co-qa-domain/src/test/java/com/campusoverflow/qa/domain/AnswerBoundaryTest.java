package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.shared.domain.DomainException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 回答聚合的边界测试（T1-06）。
 * <p>与 {@link AnswerTest} 的分工：{@code AnswerTest} 覆盖「谁能做什么」，
 * 本类覆盖「做第二次会怎样」——认证、删除、编辑的状态机边界，并断言具体错误码。
 */
class AnswerBoundaryTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    private static final Actor ANSWERER = new Actor(3, "回答者", Role.STUDENT);
    private static final Actor TEACHER = new Actor(9, "王老师", Role.TEACHER);
    private static final Actor STRANGER = new Actor(4, "路人", Role.STUDENT);

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

    private static Answer freshAnswer() {
        Question q = QuestionTest.persistedQuestion();
        return QuestionTest.persistedAnswer(q, ANSWERER.userId(), 50);
    }

    @Test
    void sameAnswerCannotBeEndorsedTwice() {
        Answer a = freshAnswer();
        a.endorse(TEACHER, true);
        assertThat(a.endorsedBy()).isEqualTo(TEACHER.userId());

        // 第二次认证：即便换成另一位老师，也必须是 QA-2003（已认证），不是 403
        Actor anotherTeacher = new Actor(10, "李老师", Role.TEACHER);
        assertThat(codeOf(() -> a.endorse(anotherTeacher, true))).isEqualTo("QA-2003");
        // 认证归属不被第二次调用改写
        assertThat(a.endorsedBy()).isEqualTo(TEACHER.userId());
    }

    @Test
    void onlyCourseManagerCanEndorse() {
        Answer a = freshAnswer();
        // 回答者本人也不行：认证是教师特权，与作者身份无关
        assertThat(codeOf(() -> a.endorse(ANSWERER, false))).isEqualTo("QA-3004");
        assertThat(a.endorsedBy()).isNull();
    }

    @Test
    void deletedAnswerCannotBeEndorsed() {
        Answer a = freshAnswer();
        a.softDelete(ANSWERER, false, false, NOW);
        assertThat(codeOf(() -> a.endorse(TEACHER, true))).isEqualTo("QA-1020");
    }

    @Test
    void deletedAnswerCannotBeEditedOrDeletedAgain() {
        Answer a = freshAnswer();
        a.softDelete(ANSWERER, false, false, NOW);
        assertThat(codeOf(() -> a.edit(ANSWERER, false, MarkdownBody.ofAnswer("改一下已删除的回答内容"), NOW)))
                .isEqualTo("QA-1020");
        assertThat(codeOf(() -> a.softDelete(ANSWERER, false, false, NOW.plusSeconds(1)))).isEqualTo("QA-1020");
    }

    @Test
    void acceptedAnswerCannotBeDeletedByAuthorButManagerCan() {
        Answer a = freshAnswer();
        assertThat(codeOf(() -> a.softDelete(ANSWERER, false, true, NOW))).isEqualTo("QA-1012");
        assertThat(a.isDeleted()).isFalse();

        a.softDelete(TEACHER, true, true, NOW);
        assertThat(a.isDeleted()).isTrue();
    }

    @Test
    void acceptedFlagAloneDoesNotBlockManager() {
        // 反向保护：accepted=true 但 actorManagesCourse=true 时必须放行，
        // 否则「先骗采纳再删除」的规则会误伤管理员清理违规内容。
        Answer a = freshAnswer();
        a.softDelete(TEACHER, true, true, NOW);
        assertThat(a.isDeleted()).isTrue();
    }

    @Test
    void strangersCannotEditOrDelete() {
        Answer a = freshAnswer();
        assertThat(codeOf(() -> a.edit(STRANGER, false, MarkdownBody.ofAnswer("路人想改别人的回答内容"), NOW)))
                .isEqualTo("QA-3002");
        assertThat(codeOf(() -> a.softDelete(STRANGER, false, false, NOW))).isEqualTo("QA-3003");
        assertThat(a.isDeleted()).isFalse();
    }

    @Test
    void editingUpdatesTimestampButNotAuthorOrQuestion() {
        Answer a = freshAnswer();
        Instant later = NOW.plusSeconds(300);
        a.edit(ANSWERER, false, MarkdownBody.ofAnswer("看叔叔节点的颜色和插入位置，再决定旋转方向。"), later);
        assertThat(a.updatedAt()).isEqualTo(later);
        assertThat(a.createdAt()).isEqualTo(NOW);
        assertThat(a.authorId()).isEqualTo(ANSWERER.userId());
        assertThat(a.questionId()).isEqualTo(QuestionTest.persistedQuestion().id());
    }

    @Test
    void submittingAnswerOnDeletedQuestionIsRejected() {
        Question q = QuestionTest.persistedQuestion();
        q.softDelete(QuestionTest.ASKER, false, NOW);
        assertThat(codeOf(() -> Answer.submit(q, ANSWERER.userId(),
                MarkdownBody.ofAnswer("这是一个迟到了很久的回答内容"), NOW))).isEqualTo("QA-1020");
    }

    @Test
    void newAnswerStartsUnendorsedWithZeroScore() {
        Answer a = Answer.submit(QuestionTest.persistedQuestion(), ANSWERER.userId(),
                MarkdownBody.ofAnswer("看叔叔节点的颜色和插入位置。"), NOW);
        assertThat(a.id()).isNull();
        assertThat(a.score()).isZero();
        assertThat(a.endorsedBy()).isNull();
        assertThat(a.isDeleted()).isFalse();
        assertThat(a.version()).isZero();
    }

    @Test
    void assignIdTwiceIsRejected() {
        Answer a = freshAnswer();
        assertThat(codeOfIllegalState(() -> a.assignId(99))).isEqualTo("ID 已分配");
    }

    /** {@code assignId} 二次调用抛的是 IllegalStateException，不走 DomainException 体系。 */
    private static String codeOfIllegalState(Runnable call) {
        try {
            call.run();
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
        throw new AssertionError("期望 IllegalStateException，但调用正常返回了");
    }
}
