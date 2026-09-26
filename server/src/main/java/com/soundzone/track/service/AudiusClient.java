package com.soundzone.track.service;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Audius 官方只读 REST 适配。只接收公开、可播放且未加访问门槛的曲目。 */
@Component
public class AudiusClient {
    private static final Logger log = LoggerFactory.getLogger(AudiusClient.class);
    private static final long CACHE_MILLIS = 60_000;
    private static final String ID_PATTERN = "[A-Za-z0-9_-]{1,64}";

    private final MusicProperties properties;
    private final TrackDurationPolicy durations;
    private final RestClient client;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public AudiusClient(
            MusicProperties properties,
            TrackDurationPolicy durations,
            RestClient.Builder builder) {
        this.properties = properties;
        this.durations = durations;
        var config = properties.getAudius();
        if (config.isEnabled() && !trimTrailingSlash(config.getBaseUrl()).startsWith("https://"))
            throw new IllegalStateException("Audius API 地址必须使用 HTTPS");
        HttpClient httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(config.getConnectTimeoutSeconds()))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(config.getReadTimeoutSeconds()));
        RestClient.Builder configured =
                builder.clone().baseUrl(trimTrailingSlash(config.getBaseUrl())).requestFactory(factory);
        if (StringUtils.hasText(config.getBearerToken()))
            configured.defaultHeader(
                    HttpHeaders.AUTHORIZATION, "Bearer " + config.getBearerToken().trim());
        this.client = configured.build();
    }

    public boolean enabled() {
        return properties.getAudius().isEnabled();
    }

    /** 空关键词返回热门曲目，便于新建域直接选择初始三首歌。 */
    public List<RemoteTrack> search(String keyword) {
        if (!enabled()) return List.of();
        String normalized = Objects.toString(keyword, "").trim();
        CachedResult cached = cache.get(normalized.toLowerCase(Locale.ROOT));
        if (cached != null && System.currentTimeMillis() - cached.createdAt() < CACHE_MILLIS)
            return cached.tracks();
        try {
            JsonNode body =
                    client.get()
                            .uri(uriBuilder -> searchUri(uriBuilder, normalized))
                            .retrieve()
                            .body(JsonNode.class);
            List<RemoteTrack> tracks = parse(body);
            cache.put(
                    normalized.toLowerCase(Locale.ROOT),
                    new CachedResult(List.copyOf(tracks), System.currentTimeMillis()));
            return tracks;
        } catch (RuntimeException e) {
            // 外部曲库故障不能拖垮已落库的小曲库搜索。
            log.warn("Audius 曲库请求失败：{}", e.getMessage());
            return List.of();
        }
    }

    private URI searchUri(org.springframework.web.util.UriBuilder builder, String keyword) {
        var config = properties.getAudius();
        builder.path(keyword.isBlank() ? "/tracks/trending" : "/tracks/search");
        if (!keyword.isBlank()) builder.queryParam("query", keyword);
        builder.queryParam("limit", Math.max(1, Math.min(50, config.getSearchLimit())));
        if (StringUtils.hasText(config.getApiKey()))
            builder.queryParam("api_key", config.getApiKey().trim());
        else if (!StringUtils.hasText(config.getBearerToken()))
            builder.queryParam("app_name", config.getAppName());
        return builder.build();
    }

    private List<RemoteTrack> parse(JsonNode body) {
        if (body == null || !body.path("data").isArray()) return List.of();
        List<RemoteTrack> result = new ArrayList<>();
        for (JsonNode node : body.path("data")) {
            String id = text(node, "id");
            String title = text(node, "title");
            String artist = node.path("user").path("name").asText("").trim();
            int durationSec = node.path("duration").asInt(240);
            if (!id.matches(ID_PATTERN) || title.isBlank() || artist.isBlank()) continue;
            if (!durations.isAllowed(durationSec)) continue;
            if (node.has("is_streamable") && !node.path("is_streamable").asBoolean(true)) continue;
            if (node.path("is_stream_gated").asBoolean(false)) continue;
            String permalink = text(node, "permalink");
            if (permalink.startsWith("/")) permalink = "https://audius.co" + permalink;
            result.add(
                    new RemoteTrack(
                            id,
                            title,
                            artist,
                            durationSec,
                            https(node.path("artwork").path("480x480").asText(null)),
                            text(node, "genre"),
                            text(node, "mood"),
                            https(permalink)));
        }
        return result;
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }

    private static String https(String value) {
        return value != null && value.startsWith("https://") ? value : null;
    }

    /** 音频组件不能携带服务端 Bearer，因此播放地址只使用可公开的 app_name 或前端安全 API key。 */
    public static String streamUrl(MusicProperties properties, String trackId) {
        if (!properties.getAudius().isEnabled()
                || trackId == null
                || !trackId.matches(ID_PATTERN)) return null;
        var config = properties.getAudius();
        if (!trimTrailingSlash(config.getBaseUrl()).startsWith("https://")) return null;
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(trimTrailingSlash(config.getBaseUrl()))
                        .pathSegment("tracks", trackId, "stream");
        if (StringUtils.hasText(config.getApiKey()))
            builder.queryParam("api_key", config.getApiKey().trim());
        else builder.queryParam("app_name", config.getAppName());
        return builder.build().encode().toUriString();
    }

    private static String trimTrailingSlash(String value) {
        String result = Objects.toString(value, "").trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    public record RemoteTrack(
            String id,
            String title,
            String artist,
            int durationSec,
            String coverUrl,
            String genre,
            String mood,
            String publicUrl) {}

    private record CachedResult(List<RemoteTrack> tracks, long createdAt) {}
}
