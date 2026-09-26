package com.soundzone.queue.dto;

import com.soundzone.common.Times;
import com.soundzone.queue.entity.QueueItem;

public record QueueItemDTO(
        Long itemId,
        Integer rank,
        Long trackId,
        String title,
        String artist,
        Integer likes,
        String by,
        Long userId,
        String coverUrl,
        String source,
        String attribution,
        String status,
        boolean liked,
        long createdAt) {
    public static QueueItemDTO from(QueueItem q, Integer rank, boolean liked) {
        return new QueueItemDTO(
                q.getId(),
                rank,
                q.getTrack().getId(),
                q.getTrack().getTitle(),
                q.getTrack().getArtist(),
                q.getLikes(),
                q.getRequester().getName(),
                q.getRequester().getId(),
                q.getTrack().getCoverUrl(),
                q.getTrack().getSource(),
                q.getTrack().getAttribution(),
                q.getStatus().name(),
                liked,
                Times.millis(q.getCreatedAt()));
    }
}
