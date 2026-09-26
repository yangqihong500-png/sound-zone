package com.soundzone.zone.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/** 创建域请求（docs/02 第 1 步，v2：2026-09-24 会议） 门槛：≥3 首歌 + 场景命名；可见性（公开/私密）+ 标签过滤双模式 + 可选番茄钟 */
public record ZoneCreateRequest(
        @NotBlank(message = "域名不能为空") @Size(max = 64, message = "域名最长 64 字符") String name,
        @NotBlank(message = "场景不能为空") @Size(max = 32, message = "场景最长 32 字符") String scene,
        Long hostId, // 兼容旧请求，实际身份由登录态决定

        /** 初始歌单：至少 3 首（docs/02 建域门槛） */
        @NotNull(message = "初始歌单不能为空") @Size(min = 3, max = 100, message = "初始歌单需 3–100 首歌")
                List<@NotNull Long> trackIds,

        /** 可见性：PUBLIC（默认）/ PRIVATE（决议 D2） */
        String visibility,

        /** 私密域密码（visibility=PRIVATE 时与 inviteCode 至少其一，服务层校验） */
        @Size(max = 32) String password,

        /** 过滤模式：BAN=禁止含（默认）/ ALLOW=仅允许含（决议 D5） */
        String filterMode,

        /** 唯一的标签选择（BAN 时为黑名单，ALLOW 时为白名单），至少一个 */
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
