package com.soundzone.track.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import lombok.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 汇总 application.yml 与 data/catalog.json 中的本地授权曲库。
 * 音频和封面只允许引用各自目录根级文件，避免清单暴露服务器上的任意文件。
 */
@Component
public class LocalCatalogRegistry {
    private final MusicProperties properties;
    private final ObjectMapper json;
    private final Path catalogFile;
    private final Path audioDirectory;
    private final Path coverDirectory;
    private volatile Map<String, MusicProperties.LocalTrack> catalog = Map.of();

    public LocalCatalogRegistry(
            MusicProperties properties,
            ObjectMapper json,
            @Value("${soundzone.local-catalog-file:./data/catalog.json}") String catalogFile,
            @Value("${soundzone.audio-directory:./data/audio}") String audioDirectory,
            @Value("${soundzone.cover-directory:./data/covers}") String coverDirectory) {
        this.properties = properties;
        this.json = json;
        this.catalogFile = Path.of(catalogFile).toAbsolutePath().normalize();
        this.audioDirectory = Path.of(audioDirectory).toAbsolutePath().normalize();
        this.coverDirectory = Path.of(coverDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    public synchronized void reload() {
        try {
            Files.createDirectories(audioDirectory);
            Files.createDirectories(coverDirectory);
            List<MusicProperties.LocalTrack> merged = new ArrayList<>(properties.getLocalCatalog());
            if (Files.isRegularFile(catalogFile)) {
                CatalogManifest manifest = json.readValue(catalogFile.toFile(), CatalogManifest.class);
                if (manifest == null || manifest.getTracks() == null)
                    throw new IllegalStateException("本地曲库清单缺少 tracks：" + catalogFile);
                if (manifest.getVersion() != 1)
                    throw new IllegalStateException("不支持的本地曲库清单版本：" + manifest.getVersion());
                merged.addAll(manifest.getTracks());
            }
            LinkedHashMap<String, MusicProperties.LocalTrack> indexed = new LinkedHashMap<>();
            for (MusicProperties.LocalTrack item : merged) {
                String key = item == null ? null : item.getKey();
                if (!StringUtils.hasText(key))
                    throw new IllegalStateException("本地曲库存在未填写 key 的曲目");
                key = key.trim();
                item.setKey(key);
                if (indexed.putIfAbsent(key, item) != null)
                    throw new IllegalStateException("本地曲库 key 重复：" + key);
            }
            catalog = Collections.unmodifiableMap(indexed);
        } catch (IOException e) {
            throw new IllegalStateException("无法读取本地曲库清单：" + catalogFile, e);
        }
    }

    public Collection<MusicProperties.LocalTrack> entries() {
        return catalog.values();
    }

    public Optional<MusicProperties.LocalTrack> find(String key) {
        return Optional.ofNullable(catalog.get(key));
    }

    public boolean hasAudioFile(MusicProperties.LocalTrack item, Set<String> allowedExtensions) {
        return validFile(item.getAudioFile(), audioDirectory, allowedExtensions);
    }

    public boolean hasCoverFile(MusicProperties.LocalTrack item, Set<String> allowedExtensions) {
        return validFile(item.getCoverFile(), coverDirectory, allowedExtensions);
    }

    public String audioUrl(MusicProperties.LocalTrack item) {
        return StringUtils.hasText(item.getAudioFile())
                ? "/media/audio/"
                        + UriUtils.encodePathSegment(item.getAudioFile(), StandardCharsets.UTF_8)
                : null;
    }

    public String coverUrl(MusicProperties.LocalTrack item) {
        if (StringUtils.hasText(item.getCoverUrl())) return item.getCoverUrl().trim();
        return StringUtils.hasText(item.getCoverFile())
                ? "/media/covers/"
                        + UriUtils.encodePathSegment(item.getCoverFile(), StandardCharsets.UTF_8)
                : null;
    }

    private boolean validFile(String fileName, Path root, Set<String> allowedExtensions) {
        if (!StringUtils.hasText(fileName) || fileName.contains("/") || fileName.contains("\\"))
            return false;
        int dot = fileName.lastIndexOf('.');
        if (dot < 1
                || !allowedExtensions.contains(
                        fileName.substring(dot + 1).toLowerCase(Locale.ROOT))) return false;
        Path file = root.resolve(fileName).normalize();
        return file.getParent().equals(root) && Files.isRegularFile(file) && Files.isReadable(file);
    }

    @Data
    public static class CatalogManifest {
        private int version = 1;
        private List<MusicProperties.LocalTrack> tracks = new ArrayList<>();
    }
}
