package com.soundzone.moment.dto;

import com.soundzone.track.entity.Track;

public record MomentTrackDTO(
        Long id,
        String title,
        String artist,
        String coverUrl,
        String coverColor,
        Integer durationSec,
        String source,
        String attribution) {
    public static MomentTrackDTO from(Track track) {
        if (track == null) return null;
        return new MomentTrackDTO(
                track.getId(),
                track.getTitle(),
                track.getArtist(),
                track.getCoverUrl(),
                track.getCoverColor(),
                track.getDurationSec(),
                track.getSource(),
                track.getAttribution());
    }
}
