package com.campusoverflow.reputation.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;

/**
 * 排行榜周期键。学期划分（可按学校校历调整）：2–7 月为春季学期，8–12 月与次年 1 月为秋季学期。
 *
 * <p>注意 1 月的归属：1 月是<em>上一个</em>秋季学期的一部分，所以 {@code 2027-01} 属于 {@code 2026-FALL}。
 * 这也是本类必须带年份前缀、不能只返回 {@code SPRING}/{@code FALL} 的原因。
 */
public final class AcademicTerm {

    public static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Shanghai");

    private AcademicTerm() {
    }

    public static String termKey(Instant at) {
        LocalDate date = at.atZone(CAMPUS_ZONE).toLocalDate();
        int month = date.getMonthValue();
        if (month >= 2 && month <= 7) {
            return date.getYear() + "-SPRING";
        }
        // 8–12 月属本年秋季；次年 1 月仍属上一年的秋季学期。
        int termStartYear = month == 1 ? date.getYear() - 1 : date.getYear();
        return termStartYear + "-FALL";
    }

    public static String weekKey(Instant at) {
        LocalDate date = at.atZone(CAMPUS_ZONE).toLocalDate();
        // 用 ISO 周所在年，而不是日历年：跨年那一周（如 12-31）两者会不一致。
        int weekBasedYear = date.get(IsoFields.WEEK_BASED_YEAR);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return weekBasedYear + "-W" + (week < 10 ? "0" + week : String.valueOf(week));
    }

    public static String monthKey(Instant at) {
        LocalDate date = at.atZone(CAMPUS_ZONE).toLocalDate();
        int month = date.getMonthValue();
        return date.getYear() + "-" + (month < 10 ? "0" + month : String.valueOf(month));
    }
}
