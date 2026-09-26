package com.soundzone.track.service;

import com.soundzone.track.entity.Track;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ConfiguredMusicProvider implements MusicProvider {
    private final MusicProperties properties;

    @Override
    public PlaybackSource resolve(Track track) {
        if ("AUDIUS".equals(track.getSource()) && track.getExternalId() != null) {
            String url = AudiusClient.streamUrl(properties, track.getExternalId());
            if (url != null)
                return new PlaybackSource("STREAM", "AUDIUS", track.getExternalId(), url, null);
        }
        if ("LOCAL_LICENSED".equals(track.getSource()) && track.getExternalId() != null) {
            MusicProperties.LocalTrack configured =
                    properties.getLocalCatalog().stream()
                            .filter(t -> track.getExternalId().equals(t.getKey()))
                            .findFirst()
                            .orElse(null);
            String url = configured == null ? null : configured.getStreamUrl();
            if (!isHttps(url) && configured != null && StringUtils.hasText(configured.getAudioFile()))
                url =
                        "/media/audio/"
                                + UriUtils.encodePathSegment(
                                        configured.getAudioFile(), StandardCharsets.UTF_8);
            if (url != null)
                return new PlaybackSource(
                        "STREAM", "LOCAL_LICENSED", track.getExternalId(), url, null);
        }
        String url = properties.getStreams().get(track.getId());
        if (isHttps(url))
            return new PlaybackSource(
                    "STREAM", track.getSource(), track.getExternalId(), url, null);
        if (("TME".equals(track.getSource()) || "HOST".equals(track.getSource()))
                && track.getExternalId() != null)
            return new PlaybackSource("HOST", track.getSource(), track.getExternalId(), null, null);
        return new PlaybackSource(
                "UNAVAILABLE", track.getSource(), track.getExternalId(), null, "该曲目尚未接入可播放音源");
    }

    private static boolean isHttps(String url) {
        return url != null && url.startsWith("https://");
    }
}
