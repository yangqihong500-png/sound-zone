package com.soundzone.feedback.dto;

import java.util.List;

/**
 * 域后个人战报（docs/02 决议 D2）：
 * "你推荐的歌让 X 人第一次听到""被 Y 人喜欢/收藏""特效触发 N 次"
 */
public record ZoneReportDTO(
        Long zoneId,
        String zoneName,
        List<Entry> entries
) {
    /** 单个参与者的战报条目 */
    public record Entry(
            Long userId,
            String userName,
            long requestedCount,   // 点歌数
            long heartCount,       // 收到红心眼（特效次数）
            long collectCount,     // 收到收藏数
            long effectCount       // 特效总触发数 = heart + collect
    ) {}
}
