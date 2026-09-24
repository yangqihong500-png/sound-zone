package com.soundzone.zone.dto;

import com.soundzone.moment.dto.MomentDTO;
import com.soundzone.queue.dto.QueueItemDTO;

import java.util.List;
import java.util.Set;

/** 域详情（docs/01 三件套：同频电台 + 歌单列表 + 动态区，v2） */
public record ZoneDetailDTO(
        Long id,
        String name,
        String scene,
        Integer listeners,
        String host,
        String coverColor,
        String visibility,        // PUBLIC / PRIVATE
        String inviteCode,        // 私密域分享凭证（公开域为 null）
        Set<String> tags,
        String filterMode,        // BAN / ALLOW
        Set<String> filterTags,
        NowPlayingDTO nowPlaying,
        List<QueueItemDTO> queue,
        List<MomentDTO> moments
) {}
