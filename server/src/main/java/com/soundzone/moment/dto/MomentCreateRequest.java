package com.soundzone.moment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 发布碎片请求（配乐由服务端自动绑定当前播放歌曲，无需传 trackId） */
public record MomentCreateRequest(
        @NotNull(message = "用户 ID 不能为空") Long userId,

        @Size(max = 500, message = "文案最长 500 字符")
        String text,

        /** Demo 阶段可传占位色；正式版为 COS 图片 URL */
        @Size(max = 512) String imageUrl,
        @Size(max = 16) String color
) {}
