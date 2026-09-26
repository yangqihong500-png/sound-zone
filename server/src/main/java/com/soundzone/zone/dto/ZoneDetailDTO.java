package com.soundzone.zone.dto;

import com.soundzone.moment.dto.MomentDTO;
import com.soundzone.queue.dto.QueueItemDTO;

import java.util.*;

/** 普通详情不返回密码或邀请码。 */
public record ZoneDetailDTO(
        Long id,
        String name,
        String scene,
        Integer listeners,
        String host,
        Long hostId,
        String coverColor,
        String visibility,
        Set<String> tags,
        String filterMode,
        Set<String> filterTags,
        NowPlayingDTO nowPlaying,
        List<QueueItemDTO> queue,
        List<MomentDTO> moments,
        long serverTime,
        long stateVersion,
        String status,
        QueueItemDTO imageCandidate) {}
