package com.soundzone.track.service;

import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/** 启动时幂等导入已授权小曲库；配置不完整时拒绝启动，避免误把未知音源当成已授权内容。 */
@Component
@RequiredArgsConstructor
@Order(0)
public class LocalCatalogInitializer implements ApplicationRunner {
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "m4a", "aac", "ogg", "wav");
    private final MusicProperties properties;
    private final TrackRepository tracks;
    private final TrackDurationPolicy durations;

    @Value("${soundzone.audio-directory:./data/audio}")
    private String audioDirectory;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (MusicProperties.LocalTrack item : properties.getLocalCatalog()) {
            validate(item);
            Track track =
                    tracks.findFirstBySourceAndExternalId("LOCAL_LICENSED", item.getKey())
                            .orElseGet(Track::new);
            track.setSource("LOCAL_LICENSED");
            track.setExternalId(item.getKey());
            track.setTitle(item.getTitle().trim());
            track.setArtist(item.getArtist().trim());
            track.setDurationSec(item.getDurationSec());
            track.setCoverColor(item.getCoverColor());
            track.setCoverUrl(item.getCoverUrl());
            track.setAttribution(item.getAttribution());
            track.setLicenseReference(item.getLicenseReference());
            track.getTags().clear();
            track.getTags().addAll(item.getTags());
            tracks.save(track);
        }
    }

    private void validate(MusicProperties.LocalTrack item) {
        boolean remoteStream =
                StringUtils.hasText(item.getStreamUrl())
                        && item.getStreamUrl().startsWith("https://");
        boolean localFile = validLocalAudio(item.getAudioFile());
        boolean invalid =
                !StringUtils.hasText(item.getKey())
                        || item.getKey().length() > 64
                        || !StringUtils.hasText(item.getTitle())
                        || item.getTitle().length() > 128
                        || !StringUtils.hasText(item.getArtist())
                        || item.getArtist().length() > 64
                        || remoteStream == localFile
                        || (StringUtils.hasText(item.getCoverUrl())
                                && !item.getCoverUrl().startsWith("https://"))
                        || !StringUtils.hasText(item.getCoverColor())
                        || item.getCoverColor().length() > 16
                        || item.getDurationSec() < 1
                        || !durations.isAllowed(item.getDurationSec())
                        || !StringUtils.hasText(item.getAttribution())
                        || item.getAttribution().length() > 256
                        || !StringUtils.hasText(item.getLicenseReference())
                        || item.getLicenseReference().length() > 512
                        || item.getTags() == null
                        || item.getTags().stream().anyMatch(tag -> !TagCatalog.isKnown(tag));
        if (invalid) throw new IllegalStateException("本地授权曲库配置不完整或包含未知标签：" + item.getKey());
    }

    /** 本地文件只允许音频目录根级文件，避免配置把任意服务器文件暴露成静态资源。 */
    private boolean validLocalAudio(String fileName) {
        if (!StringUtils.hasText(fileName) || fileName.contains("/") || fileName.contains("\\"))
            return false;
        int dot = fileName.lastIndexOf('.');
        if (dot < 1 || !AUDIO_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT)))
            return false;
        Path root = Path.of(audioDirectory).toAbsolutePath().normalize();
        Path file = root.resolve(fileName).normalize();
        return file.getParent().equals(root) && Files.isRegularFile(file) && Files.isReadable(file);
    }
}
