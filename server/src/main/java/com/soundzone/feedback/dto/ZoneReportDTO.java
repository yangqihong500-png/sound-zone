package com.soundzone.feedback.dto;

import java.util.List;

/** 历史战报结构，保留兼容数据语义；本轮不开放战报接口。 */
public record ZoneReportDTO(Long zoneId, String zoneName, List<Entry> entries) {
    /** 单个参与者的战报条目 */
    public record Entry(
            Long userId,
            String userName,
            long uploadedCount, // 上传歌曲数
            long collectCount, // 收到收藏数（微光提示次数）
            long likeCount, // 收到点赞数
            long emojiCount // 收到 emoji 互动数
            ) {}
}
