package com.campusoverflow.qa.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.qa.api.QuestionRef;
import com.campusoverflow.qa.application.fake.QaTestFixtures.InMemoryQuestions;
import com.campusoverflow.qa.domain.MarkdownBody;
import com.campusoverflow.qa.domain.Question;
import com.campusoverflow.qa.domain.Tags;
import com.campusoverflow.qa.domain.Title;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 发布语言测试：把聚合翻译成给其他上下文的 QuestionRef（字段一个都不能错，否则下游投影会歪）。 */
class QaQueryApiImplTest {

    private static final Instant NOW = Instant.parse("2026-10-08T06:00:00Z");

    private final InMemoryQuestions questions = new InMemoryQuestions();
    private final QaQueryApiImpl api = new QaQueryApiImpl(questions);

    @Test
    @DisplayName("问题存在时，6 个字段逐个映射到 QuestionRef")
    void findQuestionMapsToRef() {
        Question question = questions.save(Question.post(7L, 100L,
                new Title("红黑树插入后为什么必须旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？希望有直观解释，最好能给出一个具体的反例来说明。"),
                Tags.of(List.of("数据结构", "红黑树")), NOW));

        Optional<QuestionRef> ref = api.findQuestion(question.id());

        assertThat(ref).isPresent();
        QuestionRef value = ref.get();
        assertThat(value.id()).isEqualTo(question.id());
        assertThat(value.authorId()).isEqualTo(7L);
        assertThat(value.courseId()).isEqualTo(100L);
        assertThat(value.title()).isEqualTo("红黑树插入后为什么必须旋转？");
        assertThat(value.acceptedAnswerId()).isNull();
        assertThat(value.deleted()).isFalse();
    }

    @Test
    @DisplayName("问题不存在时返回 empty（不是抛异常），调用方据此跳过")
    void findQuestionReturnsEmptyWhenMissing() {
        assertThat(api.findQuestion(999L)).isEmpty();
    }

    @Test
    @DisplayName("软删除后 deleted 为 true，acceptedAnswerId 原样透出")
    void findQuestionReflectsDeletionAndAcceptance() {
        Question question = questions.save(Question.post(7L, null,
                new Title("红黑树插入后为什么必须旋转？"),
                MarkdownBody.ofQuestion("变色为什么不够？希望有直观解释，最好能给出一个具体的反例来说明。"),
                Tags.of(List.of("数据结构")), NOW));
        long id = question.id();
        question.softDelete(new Actor(7L, "小明", Role.STUDENT), false, NOW);

        QuestionRef value = api.findQuestion(id).orElseThrow();

        assertThat(value.deleted()).isTrue();
        assertThat(value.courseId()).isNull();
        assertThat(value.acceptedAnswerId()).isNull();
    }
}
