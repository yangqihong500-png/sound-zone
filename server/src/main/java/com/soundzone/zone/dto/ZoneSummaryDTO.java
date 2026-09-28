package com.soundzone.zone.dto;

import java.util.Set;

/** 域摘要（首页/发现页卡片），字段与前端 zone-card 组件一一对应 */
public record ZoneSummaryDTO(
        Long id,
        String name,
        String scene,
        Integer listeners,
        String host,
        String coverColor,
        String visibility, // 全局首页／搜索只返回 PUBLIC；详情 DTO 继续区分私密域
        Set<String> tags,
        String filterMode,
        Set<String> filterTags,
        NowPlayingDTO nowPlaying,
        ZoneSearchMatchDTO searchMatch) {}
