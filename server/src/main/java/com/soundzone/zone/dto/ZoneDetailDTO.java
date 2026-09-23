package com.soundzone.zone.dto;

import com.soundzone.moment.dto.MomentDTO;
import com.soundzone.queue.dto.QueueItemDTO;

import java.util.List;
import java.util.Set;

/** 域详情（docs/01 三件套：同频电台 + 点歌台 + 碎片墙） */
public record ZoneDetailDTO(
        Long id,
        String name,
        String scene,
        Integer listeners,
        String host,
        String coverColor,
        Set<String> tags,
        Set<String> bannedTags,
        NowPlayingDTO nowPlaying,
        List<QueueItemDTO> queue,
        List<MomentDTO> moments
) {}
