package com.campusoverflow.discovery.domain.notification;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 解析评论中的 @昵称（昵称不含空格与 @，长度 2–20）。每条评论最多提醒 5 人，防止滥用。 */
public final class MentionParser {

    private static final Pattern MENTION = Pattern.compile("@([^\\s@，。,.!?！？:：;；、]{2,20})");
    public static final int MAX_MENTIONS = 5;

    private MentionParser() {
    }

    /**
     * 从一段文本中提取 @昵称，去重并保持出现顺序，最多返回 {@value MAX_MENTIONS} 个。
     *
     * <p>用 {@link LinkedHashSet} 而不是 {@link java.util.HashSet}：通知的发送顺序会影响用户体验
     * （先被提到的人先收到），也让测试断言稳定可复现。
     *
     * <p>「满 5 个就停」在循环里做而不是先收集全部再截断：一条长评论可能有上百个 @，
     * 提前退出避免无谓的正则扫描。
     *
     * <p>注意正则只匹配「@ 后面跟 2–20 个非空白非标点字符」，所以邮箱地址 {@code a@b} 不会被误判
     * ——它前面的字符不构成合法的昵称（只有 1 个字符），正则要求至少 2 个。
     */
    public static Set<String> extract(String text) {
        if (text == null || text.isEmpty()) {
            return Set.of();
        }
        Set<String> mentions = new LinkedHashSet<>();
        Matcher matcher = MENTION.matcher(text);
        while (matcher.find() && mentions.size() < MAX_MENTIONS) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }
}
