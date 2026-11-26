package com.campusoverflow.discovery.domain.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link SearchQuery} 的边界测试。
 *
 * <p>{@code DiscoveryDomainTest} 只覆盖了「典型输入」这一条主线，本类补的是不容易想到、
 * 但线上真会遇到的情况：极限长度、恰好卡在阈值上、重复词项占名额、运算符剥离后留下的空格等。
 * 这些用例**不改动原有测试**，独立成类，避免与别人的断言冲突。
 */
class SearchQueryBoundaryTest {

    private static final int MAX_TERMS = 5;
    private static final int MAX_KEYWORD_LENGTH = 50;

    private static SearchQuery query(String keyword) {
        return new SearchQuery(keyword, null, null, null, false, false, null);
    }

    private static void assertRejected(String keyword, String expectedCode) {
        assertThatThrownBy(() -> query(keyword))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        e -> assertThat(e.code()).isEqualTo(expectedCode));
    }

    @Nested
    @DisplayName("词项切分")
    class TermSplitting {

        @Test
        @DisplayName("连续空白与首尾空白不会产生空词项")
        void repeatedWhitespaceDoesNotProduceEmptyTerms() {
            assertThat(query("红黑树    旋转").terms()).containsExactly("红黑树", "旋转");
        }

        @Test
        @DisplayName("去重在截断之前：6 个词但有重复时仍能凑满 5 个不重复词项")
        void deduplicationHappensBeforeLimit() {
            // 输入有 6 个词，前 5 个里有 2 个重复 —— 先去重再 limit 才能拿到 5 个不同词项。
            // 如果实现写成「先 limit(5) 再 distinct」，结果只会剩 4 个，这是一个很容易写错的顺序。
            assertThat(query("红黑树 红黑树 旋转 平衡 插入 删除").terms())
                    .containsExactly("红黑树", "旋转", "平衡", "插入", "删除");
        }

        @Test
        @DisplayName("超过 5 个不重复词项时只取前 5 个，且保持原顺序")
        void capsAtFiveTermsKeepingOriginalOrder() {
            assertThat(query("一棵 二棵 三棵 四棵 五棵 六棵 七棵").terms())
                    .containsExactly("一棵", "二棵", "三棵", "四棵", "五棵");
        }

        @Test
        @DisplayName("恰好 5 个词项不触发截断")
        void exactlyFiveTermsIsAccepted() {
            assertThat(query("红黑树 旋转 平衡 插入 删除").terms()).hasSize(MAX_TERMS);
        }

        @Test
        @DisplayName("没有关键词时返回空列表而不是 null")
        void nullKeywordYieldsEmptyList() {
            assertThat(query(null).terms()).isEmpty();
            assertThat(query("    ").terms()).isEmpty();
        }

        @Test
        @DisplayName("只由运算符组成的关键词被清空后视为无关键词")
        void operatorOnlyKeywordBecomesNull() {
            SearchQuery q = query("+-*<>()~@");
            assertThat(q.keyword()).isNull();
            assertThat(q.terms()).isEmpty();
            assertThat(q.sort()).isEqualTo(SearchSort.NEWEST);
        }
    }

    @Nested
    @DisplayName("长度校验")
    class LengthRules {

        @Test
        @DisplayName("单个词项恰好 2 个字符是允许的下限")
        void twoCharacterTermIsAccepted() {
            assertThat(query("红黑").terms()).containsExactly("红黑");
        }

        @Test
        @DisplayName("单字词项被 DIS-1001 拒绝（ngram_token_size=2 的约束）")
        void singleCharacterTermIsRejected() {
            assertRejected("树", "DIS-1001");
        }

        @Test
        @DisplayName("只要有一个词项不足 2 字符，整个关键词就被拒绝")
        void anyTooShortTermRejectsWholeKeyword() {
            assertRejected("红黑树 树", "DIS-1001");
        }

        @Test
        @DisplayName("关键词恰好 50 个字符是允许的上限")
        void fiftyCharactersIsAccepted() {
            assertThat(query("红".repeat(MAX_KEYWORD_LENGTH)).terms()).hasSize(1);
        }

        @Test
        @DisplayName("关键词 51 个字符被 DIS-1002 拒绝")
        void fiftyOneCharactersIsRejected() {
            assertRejected("红".repeat(MAX_KEYWORD_LENGTH + 1), "DIS-1002");
        }
    }

    @Nested
    @DisplayName("排序推断")
    class SortInference {

        @Test
        @DisplayName("有关键词且未指定排序时默认按相关度")
        void keywordImpliesRelevance() {
            assertThat(query("红黑树").sort()).isEqualTo(SearchSort.RELEVANCE);
        }

        @Test
        @DisplayName("无关键词时即使显式指定相关度也退化为最新")
        void relevanceWithoutKeywordFallsBackToNewest() {
            SearchQuery q = new SearchQuery("  ", null, null, SearchSort.RELEVANCE, false, false, null);
            assertThat(q.sort()).isEqualTo(SearchSort.NEWEST);
        }

        @Test
        @DisplayName("显式指定的非相关度排序不会被覆盖")
        void explicitSortIsPreserved() {
            SearchQuery q = new SearchQuery("红黑树", null, null, SearchSort.HOT, false, false, null);
            assertThat(q.sort()).isEqualTo(SearchSort.HOT);
        }
    }

    @Nested
    @DisplayName("标签规范化")
    class TagNormalization {

        @Test
        @DisplayName("标签去首尾空白并转小写")
        void tagIsTrimmedAndLowercased() {
            assertThat(new SearchQuery(null, null, "  Java  ", null, false, false, null).tag()).isEqualTo("java");
        }

        @Test
        @DisplayName("空白标签视为没有标签")
        void blankTagBecomesNull() {
            assertThat(new SearchQuery(null, null, "   ", null, false, false, null).tag()).isNull();
        }
    }

    @Nested
    @DisplayName("分页默认值")
    class PagingDefaults {

        @Test
        @DisplayName("未传分页时默认第 1 页每页 20 条")
        void defaultsToFirstPageOfTwenty() {
            assertThat(query("红黑树").page().page()).isEqualTo(1);
            assertThat(query("红黑树").page().size()).isEqualTo(20);
        }
    }
}
