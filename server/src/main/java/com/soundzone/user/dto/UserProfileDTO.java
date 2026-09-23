package com.soundzone.user.dto;

/** 我的页用户信息（歌品值 + 行为统计，docs/02 成长体系） */
public record UserProfileDTO(
        Long id,
        String name,
        String avatarColor,
        Double tasteScore,
        Stats stats
) {
    public record Stats(
            long requests,   // 点歌数
            long likes,      // 获赞数（队列点赞）
            long moments     // 碎片数
    ) {}
}
