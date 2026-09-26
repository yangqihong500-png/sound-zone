package com.soundzone.track.service;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

@Data
@Component
@ConfigurationProperties(prefix = "soundzone.music")
public class MusicProperties {
    /** 单曲最长播放时长；避免超长混音、播客或数小时音频占满 FIFO 队列。 */
    private int maxTrackDurationSeconds = 600;

    /** 服务端受控配置：本地曲目 ID -> 已获授权的 HTTPS 音源；禁止用户上传音频。 */
    private Map<Long, String> streams = new HashMap<>();

    private Audius audius = new Audius();

    /** 已取得在线播放许可的小曲库。音频地址只由服务端配置，不接受用户提交。 */
    private List<LocalTrack> localCatalog = new ArrayList<>();

    @Data
    public static class Audius {
        private boolean enabled = true;
        private String baseUrl = "https://api.audius.co/v1";
        private String appName = "SoundZone";
        private String apiKey = "";
        private String bearerToken = "";
        private int searchLimit = 20;
        private int connectTimeoutSeconds = 5;
        private int readTimeoutSeconds = 10;
    }

    @Data
    public static class LocalTrack {
        /** 配置内稳定且唯一的标识，例如 artist-title-v1。 */
        private String key;
        private String title;
        private String artist;
        private String streamUrl;
        /** 放在 soundzone.audio-directory 下的本地音频文件名；与 streamUrl 二选一。 */
        private String audioFile;
        private String coverUrl;
        private String coverColor = "#9FE1CB";
        private int durationSec = 240;
        private Set<String> tags = new HashSet<>();
        private String attribution;
        private String licenseReference;
    }
}
