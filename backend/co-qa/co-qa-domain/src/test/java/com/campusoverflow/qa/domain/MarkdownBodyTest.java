package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

/** 正文长度与规范化规则（对应 #4：MarkdownBody）。 */
class MarkdownBodyTest {

    @Test
    void questionBodyMustBeAtLeast20Chars() {
        assertThatThrownBy(() -> MarkdownBody.ofQuestion("字".repeat(19)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("问题正文");
        assertThat(MarkdownBody.ofQuestion("字".repeat(20)).value()).hasSize(20);
    }

    @Test
    void answerBodyMustBeAtLeast10Chars() {
        assertThatThrownBy(() -> MarkdownBody.ofAnswer("字".repeat(9)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("回答正文");
        assertThat(MarkdownBody.ofAnswer("字".repeat(10)).value()).hasSize(10);
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(MarkdownBody.ofAnswer("  " + "字".repeat(10) + "  ").value())
                .isEqualTo("字".repeat(10));
    }
}
