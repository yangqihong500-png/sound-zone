package com.soundzone.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 历史反馈请求结构；新接口通过 Bearer 身份与具体条目处理互动。 */
public record HeartRequest(
        @NotNull(message = "用户 ID 不能为空") Long userId,
        @NotNull(message = "歌曲 ID 不能为空") Long trackId,
        @NotBlank(message = "反馈类型不能为空") String type   // COLLECT / LIKE / EMOJI_HEART / EMOJI_LAUGH
) {}
