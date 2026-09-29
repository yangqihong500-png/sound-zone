package com.soundzone.zone.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/** 创建域请求：域名、场景与恰好三首初始歌曲必填；隐私和标签过滤有默认值。 */
public record ZoneCreateRequest(
        @NotBlank(message = "域名不能为空")
                @Size(max = 64, message = "域名最长 64 字符")
                String name,
        @NotBlank(message = "请选择场景") @Size(max = 32, message = "场景最长 32 字符") String scene,
        Long hostId, // 兼容旧请求，实际身份由登录态决定

        /** 初始歌单：恰好 3 首，按选择顺序进入 FIFO。 */
        @NotNull(message = "初始歌单不能为空") @Size(min = 3, max = 3, message = "初始歌单必须选择 3 首歌")
                List<@NotNull Long> trackIds,

        /** 可见性：PUBLIC（默认）/ PRIVATE（决议 D2） */
        String visibility,

        /** 私密域密码（visibility=PRIVATE 时与 inviteCode 至少其一，服务层校验） */
        @Size(max = 32) String password,

        /** 过滤模式：NONE=不限制（默认）/ BAN=禁止含 / ALLOW=仅允许含 */
        String filterMode,

        /** BAN／ALLOW 时至少选择一个；NONE 时必须为空。 */
        Set<@Size(max = 32) String> filterTags,

        /** 兼容旧客户端字段；新建域的展示标签由过滤模式及 filterTags 推导 */
        Set<@Size(max = 32) String> tags,
        @Size(max = 16) String coverColor,

        /** 番茄钟时段配置（决议 D3），可空表示不分段 */
        @Valid List<PeriodConfig> periods) {
    /** 单个时段配置 */
    public record PeriodConfig(
            @NotNull Integer orderIndex,
            @NotNull @jakarta.validation.constraints.Min(1) Integer durationMin,
            @NotBlank String type, // FOCUS / BREAK
            Set<String> allowedTags // 为空 = 不限制
            ) {}
}
