package com.soundzone.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 红心/收藏请求（作用于当前播放歌曲） */
public record HeartRequest(
        @NotNull(message = "用户 ID 不能为空") Long userId,
        @NotNull(message = "歌曲 ID 不能为空") Long trackId,
        @NotBlank(message = "反馈类型不能为空") String type   // HEART / COLLECT
) {}
