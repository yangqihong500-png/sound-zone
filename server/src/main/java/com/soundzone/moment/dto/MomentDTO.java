package com.soundzone.moment.dto;

import com.soundzone.common.Times;
import com.soundzone.moment.entity.Moment;

import java.time.format.DateTimeFormatter;

public record MomentDTO(
        Long id,
        Long userId,
        String text,
        String imageUrl,
        String color,
        String track,
        String by,
        String time,
        long createdAt,
        Long queueItemId,
        String moderationStatus,
        String reaction) {
    public static MomentDTO from(Moment m, String reaction) {
        return new MomentDTO(
                m.getId(),
                m.getUser().getId(),
                m.getText(),
                m.getImageUrl() == null ? null : "/moments/" + m.getId() + "/image",
                m.getColor(),
                m.getTrack() == null ? null : m.getTrack().getTitle(),
                m.getUser().getName(),
                m.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")),
                Times.millis(m.getCreatedAt()),
                m.getQueueItem() == null ? null : m.getQueueItem().getId(),
                m.getModerationStatus().name(),
                reaction);
    }
}
