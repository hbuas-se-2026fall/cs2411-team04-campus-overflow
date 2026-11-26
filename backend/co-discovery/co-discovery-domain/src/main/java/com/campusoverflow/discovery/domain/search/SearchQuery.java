package com.campusoverflow.discovery.domain.search;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.page.PageRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 搜索条件（值对象）。关键词规范化：去除全文检索运算符，按空白切分为词项，最多 5 个。
 * MySQL ngram 分词粒度为 2（ngram_token_size=2），因此单个词项至少 2 个字符。
 */
public record SearchQuery(String keyword, Long courseId, String tag, SearchSort sort, boolean unansweredOnly,
                          boolean bountyOnly, PageRequest page) {

    private static final int MAX_TERMS = 5;

    public SearchQuery {
        keyword = keyword == null ? null : keyword.replaceAll("[\"+\\-<>()~*@]", " ").strip();
        if (keyword != null && keyword.isEmpty()) {
            keyword = null;
        }
        if (keyword != null && keyword.codePointCount(0, keyword.length()) > 50) {
            throw new BusinessRuleException("DIS-1002", "关键词不能超过 50 个字符");
        }
        if (keyword != null && terms(keyword).stream().anyMatch(t -> t.codePointCount(0, t.length()) < 2)) {
            throw new BusinessRuleException("DIS-1001", "每个关键词至少 2 个字符");
        }
        tag = tag == null || tag.isBlank() ? null : tag.strip().toLowerCase(Locale.ROOT);
        if (sort == null) {
            sort = keyword != null ? SearchSort.RELEVANCE : SearchSort.NEWEST;
        }
        if (sort == SearchSort.RELEVANCE && keyword == null) {
            sort = SearchSort.NEWEST;
        }
        page = page == null ? new PageRequest(1, 20) : page;
    }

    /**
     * 返回规范化后的词项列表。没有关键词时返回空列表（调用方无需判空）。
     *
     * <p>注意：构造函数在 {@code :26-28} 处已经调用过这个静态方法做「每个词项至少 2 个字符」的校验，
     * 因此本方法不实现的话，{@code new SearchQuery(...)} 本身就构造不出来。
     */
    public List<String> terms() {
        return terms(keyword);
    }

    /**
     * 按空白切分关键词并规范化词项：丢弃空串、去重（保留首次出现的位置）、最多保留 {@value MAX_TERMS} 个。
     *
     * <p>去重必须在截断之前：先去重再 limit，才能保证「重复词不占名额」。
     * 例如 {@code "红黑树 红黑树 旋转 平衡 插入 删除"} 得到 5 个互不相同的词项，
     * 而不是先去重前 5 个再压缩成 4 个。
     *
     * <p>用 {@link String#split(String)} 配 {@code \s+} 切分而不是 {@code StringTokenizer}，
     * 是因为前者对连续空白、首尾空白都按同一个规则处理，配合后面的空串过滤即可。
     */
    private static List<String> terms(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keyword.split("\\s+"))
                .filter(term -> !term.isEmpty())
                .distinct()
                .limit(MAX_TERMS)
                .toList();
    }
}
