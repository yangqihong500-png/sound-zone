package com.soundzone.track.service;

import com.soundzone.track.entity.Track;

/** 曲库播放适配：独立 App 使用 Audius 或已授权流地址，宿主桥只作为后续可选扩展。 */
public interface MusicProvider {
    PlaybackSource resolve(Track track);

    record PlaybackSource(
            String kind, String source, String externalId, String streamUrl, String message) {}
}
