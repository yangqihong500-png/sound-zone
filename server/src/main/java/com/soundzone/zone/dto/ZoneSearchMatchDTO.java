package com.soundzone.zone.dto;

/** 首页搜索命中的歌曲；position 仅用于待播／预存队列，均从 1 开始。 */
public record ZoneSearchMatchDTO(
        String type, // PLAYING / QUEUED / PRESET
        String title,
        String artist,
        Integer position) {}
