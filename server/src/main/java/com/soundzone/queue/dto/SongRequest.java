package com.soundzone.queue.dto;

import jakarta.validation.constraints.NotNull;

/** 点歌请求 */
public record SongRequest(
        @NotNull(message = "歌曲 ID 不能为空") Long trackId,
        @NotNull(message = "用户 ID 不能为空") Long userId
) {}
