package com.soundzone.user.dto;

import java.util.List;

/** 仅向本人展示的共听时长汇总，不进入其他用户的公开资料。 */
public record ListeningSummaryDTO(
        long totalSeconds,
        long todaySeconds,
        long last7DaysSeconds,
        List<DailyListening> daily,
        List<TopZone> topZones) {
    public record DailyListening(String date, long seconds) {}

    public record TopZone(Long zoneId, String name, long seconds) {}
}
