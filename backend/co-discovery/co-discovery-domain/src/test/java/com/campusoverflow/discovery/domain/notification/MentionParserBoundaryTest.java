package com.campusoverflow.discovery.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link MentionParser} 的边界测试。
 *
 * <p>@提及最容易出问题的是误匹配与配额计算。正则本身由规格给定（昵称 2–20 字符、不含空格与 @），
 * 本类把它的实际行为固定下来，避免以后有人「顺手放宽」而没人发现。
 */
class MentionParserBoundaryTest {

    @Test
    @DisplayName("null 与空串返回空集合而不是抛异常")
    void nullAndEmptyYieldEmptySet() {
        assertThat(MentionParser.extract(null)).isEmpty();
        assertThat(MentionParser.extract("")).isEmpty();
        assertThat(MentionParser.extract("   ")).isEmpty();
    }

    @Test
    @DisplayName("没有 @ 的文本返回空集合")
    void textWithoutMentionYieldsEmptySet() {
        assertThat(MentionParser.extract("这是一段没有提及的普通评论")).isEmpty();
    }

    @Test
    @DisplayName("@ 后面不足 2 个字符时不匹配")
    void atFollowedByLessThanTwoCharactersIsNotAMention() {
        // 规格明确给出的用例：a@b 里 @ 后只有一个 b，不满足 {2,20}。
        assertThat(MentionParser.extract("邮箱 a@b 不算")).isEmpty();
        assertThat(MentionParser.extract("结尾的 @ ")).isEmpty();
    }

    @Test
    @DisplayName("去重并保持首次出现的顺序")
    void deduplicatesKeepingFirstOccurrenceOrder() {
        assertThat(MentionParser.extract("@张三 @李四 @张三 @王五"))
                .containsExactly("张三", "李四", "王五");
    }

    @Test
    @DisplayName("恰好 5 个提及全部保留")
    void exactlyFiveMentionsAreAllKept() {
        Set<String> mentions = MentionParser.extract("@a1 @b2 @c3 @d4 @e5");
        assertThat(mentions).hasSize(MentionParser.MAX_MENTIONS);
        assertThat(mentions).containsExactly("a1", "b2", "c3", "d4", "e5");
    }

    @Test
    @DisplayName("超过 5 个提及只保留前 5 个")
    void moreThanFiveMentionsAreCapped() {
        Set<String> mentions = MentionParser.extract("@a1 @b2 @c3 @d4 @e5 @f6 @g7");
        assertThat(mentions).hasSize(MentionParser.MAX_MENTIONS);
        assertThat(mentions).containsExactly("a1", "b2", "c3", "d4", "e5");
    }

    @Test
    @DisplayName("重复提及不占名额：上限针对「不同的人」而不是「出现的次数」")
    void duplicatesDoNotConsumeQuota() {
        // 如果配额按「出现次数」算，刷 5 个 @同一个人 就能挤掉真正想提醒的其他 4 个人，
        // 防滥用规则反而变成滥用工具。去重后再计数才符合规则初衷。
        Set<String> mentions = MentionParser.extract("@张三 @张三 @张三 @李四 @王五 @赵六 @钱七");
        assertThat(mentions).containsExactly("张三", "李四", "王五", "赵六", "钱七");
    }

    @Test
    @DisplayName("中文标点会终止昵称匹配")
    void chinesePunctuationTerminatesNickname() {
        assertThat(MentionParser.extract("感谢 @王老师，另外 @小明 你看看"))
                .containsExactly("王老师", "小明");
        assertThat(MentionParser.extract("@王老师。谢谢")).containsExactly("王老师");
        assertThat(MentionParser.extract("@小明：你好")).containsExactly("小明");
        assertThat(MentionParser.extract("@小明、@小红")).containsExactly("小明", "小红");
    }

    @Test
    @DisplayName("英文标点同样终止昵称匹配")
    void englishPunctuationTerminatesNickname() {
        assertThat(MentionParser.extract("@alice, please check")).containsExactly("alice");
        assertThat(MentionParser.extract("@bob: thanks")).containsExactly("bob");
    }

    @Test
    @DisplayName("恰好 20 个字符的昵称可以匹配")
    void nicknameOfTwentyIsMatched() {
        String twenty = "a".repeat(20);
        assertThat(MentionParser.extract("@" + twenty)).containsExactly(twenty);
    }

    @Test
    @DisplayName("单个字符的昵称不匹配")
    void singleCharacterNicknameIsNotMatched() {
        assertThat(MentionParser.extract("@a")).isEmpty();
    }

    @Test
    @DisplayName("同一句话里的英文与中文昵称都能提取")
    void mixedChineseAndEnglishNicknames() {
        assertThat(MentionParser.extract("@alice 和 @张三 都看看"))
                .containsExactly("alice", "张三");
    }

    @Test
    @DisplayName("连续两个 @ 时从第二个开始匹配")
    void consecutiveAtSigns() {
        // 第一个 @ 后面紧跟 @，不是合法昵称字符，所以匹配从第二个 @ 开始。
        assertThat(MentionParser.extract("@@张三")).containsExactly("张三");
    }
}
