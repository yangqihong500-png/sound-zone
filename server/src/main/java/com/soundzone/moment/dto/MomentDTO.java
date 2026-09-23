package com.soundzone.moment.dto;

import com.soundzone.moment.entity.Moment;

import java.time.format.DateTimeFormatter;

/** 碎片响应（track 为自动绑定的配乐 —— 三元组的关键一环） */
public record MomentDTO(
        Long id,
        String text,
        String imageUrl,
        String color,
        String track,
        String by,
        String time
) {
    public static MomentDTO from(Moment m) {
        return new MomentDTO(
                m.getId(),
                m.getText(),
                m.getImageUrl(),
                m.getColor(),
                m.getTrack() != null ? m.getTrack().getTitle() : null,
                m.getUser().getName(),
                m.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm"))
        );
    }
}
