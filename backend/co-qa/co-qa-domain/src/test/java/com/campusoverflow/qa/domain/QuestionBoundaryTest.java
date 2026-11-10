package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.shared.domain.DomainException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 问题聚合的边界测试（T1-06）。
 * <p>与 {@link QuestionTest} 的分工：{@code QuestionTest} 验证「规则有没有」，本类验证「边界卡在哪」——
 * 每个阈值都测「差一个」与「刚好等于」两侧，并断言具体错误码。
 * <p>不修改 {@code QuestionTest} / {@code AnswerTest} / {@code VoteAndCommentTest} 的任何断言。
 */
class QuestionBoundaryTest {

    private static final Instant NOW = Instant.parse("2026-10-01T08:00:00Z");
    private static final Actor ASKER = new Actor(1, "提问者", Role.STUDENT);
    private static final Actor ANSWERER = new Actor(3, "回答者", Role.STUDENT);

    /** 恰好 10 个字符的合法标题。 */
    private static Title titleOfLength(int chars) {
        return new Title("a".repeat(chars));
    }

    private static MarkdownBody validBody() {
        return MarkdownBody.ofQuestion("插入节点后出现连续红色节点，什么时候左旋、什么时候右旋？");
    }

    /**
     * 取调用抛出的领域异常的错误码；没抛或抛了别的类型都算失败。
     * <p>用自写的 {@code Runnable} 而不是 AssertJ 的 {@code catchThrowableOfType}，
     * 是因为后者的参数顺序在 AssertJ 3.26 前后变过，升级依赖时会静默编译失败。
     */
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
    void titleLengthBoundaryIsTenTo150() {
        // 下界：9 拒、10 收
        assertThat(codeOf(() -> titleOfLength(9))).isEqualTo("QA-1001");
        assertThat(titleOfLength(10).value()).hasSize(10);
        // 上界：150 收、151 拒
        assertThat(titleOfLength(150).value()).hasSize(150);
        assertThat(codeOf(() -> titleOfLength(151))).isEqualTo("QA-1001");
    }

    @Test
    void titleIsTrimmedBeforeLengthIsCounted() {
        // 首尾空白先 strip 再计数：两侧各 3 个空格 + 10 个字符 = 合法
        assertThat(new Title("   " + "a".repeat(10) + "   ").value()).hasSize(10);
    }

    @Test
    void tagCountBoundaryIsOneToFive() {
        assertThat(codeOf(() -> Tags.of(List.of()))).isEqualTo("QA-1005");
        assertThat(Tags.of(List.of("java")).names()).hasSize(1);
        assertThat(Tags.of(List.of("java", "c++", "算法", "数据库", "网络")).names()).hasSize(5);
        assertThat(codeOf(() -> Tags.of(List.of("a", "b", "c", "d", "e", "f")))).isEqualTo("QA-1005");
    }

    @Test
    void tagCountIsCheckedAfterDeduplication() {
        // 6 个原始标签但只有 5 个不同 → 先去重再计数，合法。
        // 这条锁死了「去重与计数」的先后顺序，顺序反过来会误拒。
        assertThat(Tags.of(List.of("java", "java", "c++", "算法", "数据库", "网络")).names())
                .containsExactly("java", "c++", "算法", "数据库", "网络");
    }

    @Test
    void nullTagsAreRejected() {
        assertThat(codeOf(() -> Tags.of(null))).isEqualTo("QA-1005");
    }

    @Test
    void newQuestionIsNotAccepted() {
        Question q = Question.post(ASKER.userId(), 100L, titleOfLength(20), validBody(),
                Tags.of(List.of("数据结构")), NOW);
        assertThat(q.acceptedAnswerId()).isNull();
        assertThat(q.isAccepted(50)).isFalse();
    }

    @Test
    void editingDoesNotAccidentallySetAcceptedAnswer() {
        Question q = QuestionTest.persistedQuestion();
        q.edit(ASKER, false, titleOfLength(20), validBody(), Tags.of(List.of("数据结构")), NOW.plusSeconds(60));
        assertThat(q.acceptedAnswerId()).isNull();
    }

    @Test
    void cannotAcceptTwiceOnSameQuestion() {
        Question q = QuestionTest.persistedQuestion();
        q.accept(QuestionTest.persistedAnswer(q, ANSWERER.userId(), 50), ASKER, false);
        assertThat(q.acceptedAnswerId()).isEqualTo(50L);

        Answer second = QuestionTest.persistedAnswer(q, ANSWERER.userId(), 51);
        assertThat(codeOf(() -> q.accept(second, ASKER, false))).isEqualTo("QA-2002");
        // 重复采纳被拒后，原采纳关系不能被破坏
        assertThat(q.acceptedAnswerId()).isEqualTo(50L);
    }

    @Test
    void acceptingOnUnpersistedQuestionFailsWithBusinessRuleNotNpe() {
        // 回归保护：id 为 null 时若直接拆箱比较会抛 NPE（PR #19 修过），必须是 QA-1010
        Question fresh = Question.post(ASKER.userId(), 100L, titleOfLength(20), validBody(),
                Tags.of(List.of("数据结构")), NOW);
        Answer answer = Answer.reconstitute(50L, 999L, ANSWERER.userId(),
                MarkdownBody.ofAnswer("随便一条还没落到库里的回答内容"), 0, null, NOW, NOW, null, 0);
        assertThat(codeOf(() -> fresh.accept(answer, ASKER, false))).isEqualTo("QA-1010");
    }

    @Test
    void deletedQuestionCannotBeEdited() {
        Question q = QuestionTest.persistedQuestion();
        q.softDelete(ASKER, false, NOW);
        assertThat(codeOf(() -> q.edit(ASKER, false, titleOfLength(20), validBody(),
                Tags.of(List.of("数据结构")), NOW.plusSeconds(60)))).isEqualTo("QA-1020");
    }

    @Test
    void deletedQuestionCannotBeAnswered() {
        Question q = QuestionTest.persistedQuestion();
        q.softDelete(ASKER, false, NOW);
        assertThat(codeOf(() -> Answer.submit(q, ANSWERER.userId(),
                MarkdownBody.ofAnswer("这是一个迟到了很久的回答内容"), NOW))).isEqualTo("QA-1020");
    }

    @Test
    void deletedQuestionCannotBeAccepted() {
        Question q = QuestionTest.persistedQuestion();
        Answer a = QuestionTest.persistedAnswer(q, ANSWERER.userId(), 50);
        q.softDelete(ASKER, false, NOW);
        assertThat(codeOf(() -> q.accept(a, ASKER, false))).isEqualTo("QA-1020");
    }

    @Test
    void deletedQuestionCannotBeDeletedAgain() {
        Question q = QuestionTest.persistedQuestion();
        q.softDelete(ASKER, false, NOW);
        assertThat(codeOf(() -> q.softDelete(ASKER, false, NOW.plusSeconds(1)))).isEqualTo("QA-1020");
    }
}
