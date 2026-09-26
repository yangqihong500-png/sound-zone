package com.soundzone.zone.dto;

/** 所有客户端使用 startedAt + serverTime 对齐；进度百分比只用于展示。 */
public record NowPlayingDTO(
        Long itemId,
        Long trackId,
        String title,
        String artist,
        String by,
        Long userId,
        Integer progress,
        long startedAt,
        int durationSec,
        String coverUrl,
        String source,
        String attribution,
        int likes,
        boolean liked,
        boolean collected) {}
