package com.soundzone.zone.dto;

/** 正在播放信息（progress 为百分比 0~100，由服务端权威时钟推算） */
public record NowPlayingDTO(
        Long trackId,
        String title,
        String artist,
        String by,
        Integer progress
) {}
