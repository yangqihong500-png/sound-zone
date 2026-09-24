package com.soundzone.moment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发布图片分享请求（v2：2026-09-24 会议）
 * 关联歌曲规则：传 trackId 则绑定该曲（须为本人上传的歌曲）；
 * 不传则回退绑定域内当前播放歌曲
 */
public record MomentCreateRequest(
        @NotNull(message = "用户 ID 不能为空") Long userId,

        @Size(max = 500, message = "文案最长 500 字符")
        String text,

        /** Demo 阶段可传占位色；正式版为 COS 图片 URL */
        @Size(max = 512) String imageUrl,
        @Size(max = 16) String color,

        /** 关联歌曲 ID（可选）：图片与歌曲关联（决议 D6） */
        Long trackId
) {}
