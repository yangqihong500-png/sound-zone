package com.soundzone.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 互动反馈请求（作用于当前播放歌曲）：COLLECT / LIKE / EMOJI_HEART / EMOJI_LAUGH */
public record HeartRequest(
        @NotNull(message = "用户 ID 不能为空") Long userId,
        @NotNull(message = "歌曲 ID 不能为空") Long trackId,
        @NotBlank(message = "反馈类型不能为空") String type   // COLLECT / LIKE / EMOJI_HEART / EMOJI_LAUGH
) {}
