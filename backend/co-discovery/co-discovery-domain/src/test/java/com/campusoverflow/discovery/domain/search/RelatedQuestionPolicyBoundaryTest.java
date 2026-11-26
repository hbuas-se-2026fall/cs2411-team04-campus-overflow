package com.campusoverflow.discovery.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusoverflow.discovery.domain.search.SearchIndex.RelatedCandidate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link RelatedQuestionPolicy} 的边界测试。
 *
 * <p>重点在两处容易写错的地方：Jaccard 的分母（并集，不是交集也不是较小集合）和排序的三级
 * 比较链（相似度 → 得分 → 已采纳）。原测试只给了一组数值，覆盖不到并列与截断。
 */
class RelatedQuestionPolicyBoundaryTest {

    private static RelatedCandidate candidate(long id, int sharedTags, int tagCount, int score, boolean accepted) {
        return new RelatedCandidate(id, "Q" + id, sharedTags, tagCount, score, 0, accepted);
    }

    @Nested
    @DisplayName("Jaccard 相似度")
    class Jaccard {

        @Test
        @DisplayName("标准算式：交集 / 并集")
        void standardFormula() {
            assertThat(RelatedQuestionPolicy.jaccard(2, 3, 3)).isEqualTo(0.5);
            assertThat(RelatedQuestionPolicy.jaccard(3, 3, 3)).isEqualTo(1.0);
            assertThat(RelatedQuestionPolicy.jaccard(1, 5, 3)).isEqualTo(1.0 / 7.0);
        }

        @Test
        @DisplayName("完全没有交集时相似度为 0")
        void disjointSetsYieldZero() {
            assertThat(RelatedQuestionPolicy.jaccard(0, 3, 4)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("两个空集合时分母为 0，返回 0 而不是抛除零异常")
        void bothEmptyYieldsZero() {
            assertThat(RelatedQuestionPolicy.jaccard(0, 0, 0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("一个空集合时相似度为 0")
        void oneEmptyYieldsZero() {
            assertThat(RelatedQuestionPolicy.jaccard(0, 0, 4)).isEqualTo(0.0);
            assertThat(RelatedQuestionPolicy.jaccard(0, 4, 0)).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("排序与截断")
    class Ranking {

        @Test
        @DisplayName("相似度不同时按相似度降序，得分无法翻盘")
        void similarityDominatesScore() {
            // 相似度 2/3 ≈ 0.67 vs 1/7 ≈ 0.14，即使后者得分 99 也排在后面。
            List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                    candidate(1, 1, 5, 99, true),
                    candidate(2, 2, 2, 0, false)), 3, 10);
            assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("相似度相同时得分高者优先")
        void sameSimilarityPrefersHigherScore() {
            // 两个候选的 J 都是 2/(2+3-2) = 2/3。
            List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                    candidate(1, 2, 2, 10, false),
                    candidate(2, 2, 2, 50, false)), 3, 10);
            assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("相似度与得分都相同时已采纳者优先")
        void sameSimilarityAndScorePrefersAccepted() {
            List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                    candidate(1, 2, 2, 10, false),
                    candidate(2, 2, 2, 10, true)), 3, 10);
            assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("三项都相同时按 questionId 升序，保证结果稳定可复现")
        void fullyTiedCandidatesAreOrderedById() {
            // 没有这一级兜底的话，排序结果取决于流的内部分组方式，同样的输入可能得到不同的顺序，
            // 分页时会出现「第 2 页把第 1 页的条目又返回一次」这类问题。
            List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                    candidate(3, 2, 2, 10, true),
                    candidate(1, 2, 2, 10, true),
                    candidate(2, 2, 2, 10, true)), 3, 10);
            assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("截取前 limit 条")
        void truncatesToLimit() {
            List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                    candidate(1, 3, 3, 10, false),
                    candidate(2, 2, 3, 10, false),
                    candidate(3, 1, 3, 10, false)), 3, 2);
            assertThat(ranked).extracting(RelatedCandidate::questionId).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("候选数量少于 limit 时全量返回")
        void fewerThanLimitReturnsAll() {
            List<RelatedCandidate> ranked = RelatedQuestionPolicy.rank(List.of(
                    candidate(1, 1, 2, 0, false)), 2, 10);
            assertThat(ranked).hasSize(1);
        }

        @Test
        @DisplayName("limit 为 0 或负数时返回空列表而不是抛异常")
        void nonPositiveLimitYieldsEmpty() {
            List<RelatedCandidate> candidates = List.of(candidate(1, 1, 2, 0, false));
            assertThat(RelatedQuestionPolicy.rank(candidates, 2, 0)).isEmpty();
            assertThat(RelatedQuestionPolicy.rank(candidates, 2, -1)).isEmpty();
        }

        @Test
        @DisplayName("空候选列表返回空列表")
        void emptyCandidatesYieldEmpty() {
            assertThat(RelatedQuestionPolicy.rank(List.of(), 3, 10)).isEmpty();
        }

        @Test
        @DisplayName("null 候选列表返回空列表而不是 NPE")
        void nullCandidatesYieldEmpty() {
            // 上游 JDBC 查询在「没有任何共享标签的问题」时可能返回 null 而不是空集合，
            // 这里兜住能避免把一次正常的空结果变成 500。
            assertThat(RelatedQuestionPolicy.rank(null, 3, 10)).isEmpty();
        }
    }
}
