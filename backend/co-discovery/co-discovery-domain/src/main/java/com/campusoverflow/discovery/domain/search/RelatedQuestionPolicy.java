package com.campusoverflow.discovery.domain.search;

import java.util.Comparator;
import java.util.List;

/**
 * 相关问题推荐（规则引擎，非 AI）：按标签集合的 Jaccard 相似度排序，
 * J(A,B) = |A∩B| / |A∪B|；相似度相同则得分高、已采纳者优先。
 */
public final class RelatedQuestionPolicy {

    private RelatedQuestionPolicy() {
    }

    /**
     * Jaccard 相似度：J(A,B) = |A∩B| / |A∪B|，其中 |A∪B| = |A| + |B| − |A∩B|。
     *
     * <p>分母为 0 时返回 {@code 0.0}：这种情况只会出现在两个问题都没有标签，
     * 此时「相似度」在数学上无定义，业务上按「没有任何相似」处理，避免除零异常。
     *
     * @param shared 两个标签集合的交集大小
     * @param sizeA  集合 A（候选问题的标签数）
     * @param sizeB  集合 B（当前问题的标签数）
     */
    public static double jaccard(int shared, int sizeA, int sizeB) {
        int union = sizeA + sizeB - shared;
        if (union <= 0) {
            return 0.0;
        }
        return (double) shared / union;
    }

    /**
     * 按相似度对候选问题排序并截取前 {@code limit} 条。
     *
     * <p>排序规则（逐级生效，上一级相等才看下一级）：
     * <ol>
     *   <li>Jaccard 相似度降序 —— 标签越接近越相关</li>
     *   <li>得分降序 —— 同样相关时，社区认可度高的优先</li>
     *   <li>已采纳优先 —— 再相同时，问题已有被采纳的答案说明它有确定结论，更值得推荐</li>
     * </ol>
     *
     * <p>为什么把 {@code ownTagCount} 作为参数传入而不在这里算：调用方（查询服务）已经知道
     * 当前问题的标签列表，传长度比再传来一个集合更省。候选的 {@code tagCount} 则来自投影表。
     */
    public static List<SearchIndex.RelatedCandidate> rank(List<SearchIndex.RelatedCandidate> candidates, int ownTagCount,
                                                          int limit) {
        if (candidates == null || candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparingDouble((SearchIndex.RelatedCandidate c) ->
                                jaccard(c.sharedTags(), c.tagCount(), ownTagCount))
                        .reversed()
                        .thenComparing(Comparator.comparingInt(SearchIndex.RelatedCandidate::score).reversed())
                        .thenComparing(SearchIndex.RelatedCandidate::accepted, Comparator.reverseOrder())
                        .thenComparingLong(SearchIndex.RelatedCandidate::questionId))
                .limit(limit)
                .toList();
    }
}
