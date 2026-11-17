package com.campusoverflow.reputation.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * 徽章授予规则（纯函数，易于测试与扩展）。
 *
 * <p>阈值取自 {@link BadgeType} 的枚举说明：累计 10 个采纳授「解惑达人」，声誉 100 / 1000 分别授
 * 「小有名气」「德高望重」。徽章是<em>累计态</em>而非增量：返回的是「该用户此刻应持有的全部徽章」，
 * 因此上界徽章会随着阈值达成而不断加入集合，已授予的不会被移出（{@code BadgeService.grant} 幂等，重复授予无副作用）。
 */
public final class BadgePolicy {

    /** 授「解惑达人」所需的累计采纳数。 */
    public static final int HELPER_ACCEPTED_THRESHOLD = 10;

    private BadgePolicy() {
    }

    public static Set<BadgeType> forAcceptedCount(long acceptedCount) {
        Set<BadgeType> result = EnumSet.noneOf(BadgeType.class);
        if (acceptedCount >= 1) {
            result.add(BadgeType.FIRST_ACCEPTED);
        }
        if (acceptedCount >= HELPER_ACCEPTED_THRESHOLD) {
            result.add(BadgeType.HELPER_10);
        }
        return result;
    }

    public static Set<BadgeType> forReputation(int reputation) {
        Set<BadgeType> result = EnumSet.noneOf(BadgeType.class);
        if (reputation >= 100) {
            result.add(BadgeType.REPUTATION_100);
        }
        if (reputation >= 1000) {
            result.add(BadgeType.REPUTATION_1000);
        }
        return result;
    }
}
