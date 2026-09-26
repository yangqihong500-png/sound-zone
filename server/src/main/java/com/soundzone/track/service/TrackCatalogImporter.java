package com.soundzone.track.service;

import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 把外部曲库元数据转成稳定的本地 Track ID，队列只引用本地 ID。 */
@Service
@RequiredArgsConstructor
public class TrackCatalogImporter {
    private static final List<String> COLORS =
            List.of("#9FE1CB", "#A8B8C8", "#B8CDD9", "#C3B8D9", "#D9C3B8", "#C9D4C5");

    private final TrackRepository tracks;
    private final TrackDurationPolicy durations;

    @Transactional
    public List<Track> importAudius(List<AudiusClient.RemoteTrack> source) {
        List<Track> result = new ArrayList<>();
        for (AudiusClient.RemoteTrack item : source) {
            if (!durations.isAllowed(item.durationSec())) continue;
            Track track =
                    tracks.findFirstBySourceAndExternalId("AUDIUS", item.id())
                            .orElseGet(Track::new);
            track.setSource("AUDIUS");
            track.setExternalId(item.id());
            track.setTitle(limit(item.title(), 128));
            track.setArtist(limit(item.artist(), 64));
            track.setDurationSec(item.durationSec());
            track.setCoverUrl(item.coverUrl());
            track.setCoverColor(COLORS.get(Math.floorMod(item.id().hashCode(), COLORS.size())));
            track.setAttribution(limit(item.artist() + " · Audius", 256));
            track.setLicenseReference(item.publicUrl());
            track.getTags().clear();
            track.getTags().addAll(tags(item.genre(), item.mood()));
            result.add(tracks.save(track));
        }
        return result;
    }

    static Set<String> tags(String genre, String mood) {
        Set<String> result = new LinkedHashSet<>();
        String g = Objects.toString(genre, "").toLowerCase(Locale.ROOT);
        String m = Objects.toString(mood, "").toLowerCase(Locale.ROOT);
        if (contains(g, "electronic", "edm", "dance", "techno", "house", "ambient"))
            result.add("电子");
        if (contains(g, "rock", "metal", "punk", "alternative")) result.add("摇滚");
        if (contains(g, "hip-hop", "hip hop", "rap", "trap")) result.add("说唱");
        if (contains(g, "folk", "singer-songwriter", "country")) result.add("民谣");
        if (contains(g, "jazz", "blues")) result.add("爵士");
        if (contains(g, "classical", "orchestral")) result.add("古典");
        if (contains(g, "lo-fi", "lofi")) result.add("Lo-Fi");
        if (contains(g, "pop", "r&b", "soul")) result.add("流行");
        if (contains(m, "peaceful", "relax", "calm", "easygoing")) result.add("舒缓");
        if (contains(m, "uplifting", "happy", "inspiring")) result.add("治愈");
        if (contains(m, "energ", "aggressive", "fiery")) result.add("亢奋");
        if (contains(m, "melanch", "sad", "somber")) result.add("忧郁");
        if (contains(m, "romantic", "sentimental")) result.add("情歌");
        if (contains(m, "focused", "meditative")) result.add("专注");
        return result;
    }

    private static boolean contains(String value, String... needles) {
        return Arrays.stream(needles).anyMatch(value::contains);
    }

    private static String limit(String value, int size) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.length() <= size ? normalized : normalized.substring(0, size);
    }
}
