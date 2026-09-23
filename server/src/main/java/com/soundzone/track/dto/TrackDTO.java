package com.soundzone.track.dto;

import com.soundzone.track.entity.Track;

import java.util.Set;

/** 曲目响应对象 */
public record TrackDTO(
        Long id,
        String title,
        String artist,
        String coverColor,
        Integer durationSec,
        Set<String> tags
) {
    public static TrackDTO from(Track t) {
        return new TrackDTO(t.getId(), t.getTitle(), t.getArtist(),
                t.getCoverColor(), t.getDurationSec(), t.getTags());
    }
}
