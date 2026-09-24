package com.soundzone.user.dto;

/**
 * 我的页用户信息（v2：功能型主页，docs/02）
 * v2 变更：移除歌品值（去游戏化），改为 上传/获赞/分享/关注 行为统计
 */
public record UserProfileDTO(
        Long id,
        String name,
        String avatarColor,
        Stats stats
) {
    public record Stats(
            long uploads,     // 上传歌曲数
            long likes,       // 获赞数（队列点赞）
            long moments,     // 图片分享数
            long following    // 关注数
    ) {}
}
