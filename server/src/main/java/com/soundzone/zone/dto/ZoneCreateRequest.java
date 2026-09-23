package com.soundzone.zone.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/**
 * 创建域请求（docs/02 第 1 步）
 * 门槛：≥3 首歌 + 场景命名；可附带曲风黑名单与番茄钟配置
 */
public record ZoneCreateRequest(

        @NotBlank(message = "域名不能为空")
        @Size(max = 64, message = "域名最长 64 字符")
        String name,

        @NotBlank(message = "场景不能为空")
        @Size(max = 32, message = "场景最长 32 字符")
        String scene,

        @NotNull(message = "域主 ID 不能为空")
        Long hostId,

        /** 初始歌单：至少 3 首（docs/02 建域门槛） */
        @NotNull(message = "初始歌单不能为空")
        @Size(min = 3, message = "创建域至少需要 3 首歌")
        List<@NotNull Long> trackIds,

        /** 曲风黑名单（决议 D1），可空 */
        Set<@Size(max = 32) String> bannedTags,

        /** 域风格标签（展示用），可空 */
        Set<@Size(max = 32) String> tags,

        @Size(max = 16)
        String coverColor,

        /** 番茄钟时段配置（决议 D3），可空表示不分段 */
        @Valid
        List<PeriodConfig> periods
) {
    /** 单个时段配置 */
    public record PeriodConfig(
            @NotNull Integer orderIndex,
            @NotNull Integer durationMin,
            @NotBlank String type,          // FOCUS / BREAK
            Set<String> allowedTags         // 为空 = 不限制
    ) {}
}
