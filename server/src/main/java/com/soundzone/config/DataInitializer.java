package com.soundzone.config;

import com.soundzone.moment.entity.*;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.entity.*;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.track.service.MusicProperties;
import com.soundzone.track.service.TrackDurationPolicy;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.entity.*;
import com.soundzone.zone.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/** 幂等写入常驻演示用户和域；稳定标识避免重复，不影响普通用户创建的数据。 */
@Component
@RequiredArgsConstructor
@Order(10)
@ConditionalOnProperty(
        name = "soundzone.demo-data-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {
    private static final List<DemoUser> USERS =
            List.of(
                    new DemoUser("study-host", "白桃乌龙", "#A8B8C8"),
                    new DemoUser("run-host", "配速430", "#A9C4B5"),
                    new DemoUser("travel-host", "城市漫游者", "#D9CFB8"),
                    new DemoUser("night-host", "午夜留声机", "#C3B8D9"),
                    new DemoUser("listener-1", "橘子海", "#E3C9CD"),
                    new DemoUser("listener-2", "蓝色耳机", "#B8CDD9"),
                    new DemoUser("listener-3", "风从窗边来", "#C9D4C5"),
                    new DemoUser("listener-4", "今晚不熬夜", "#D7C8B5"));

    private static final List<DemoZone> ZONES =
            List.of(
                    new DemoZone(
                            "study-host",
                            "考研自习室",
                            "自习",
                            "#A8B8C8",
                            Set.of("舒缓"),
                            "写完这一页再休息 ☕"),
                    new DemoZone(
                            "run-host",
                            "夜跑俱乐部",
                            "健身",
                            "#A9C4B5",
                            Set.of("亢奋"),
                            "今晚的五公里完成！"),
                    new DemoZone(
                            "travel-host",
                            "城市漫游电台",
                            "旅行",
                            "#D9CFB8",
                            Set.of("舒缓"),
                            "把路上的风景分享给你"),
                    new DemoZone(
                            "night-host",
                            "下班后的客厅",
                            "深夜",
                            "#C3B8D9",
                            Set.of("流行", "说唱"),
                            "今天辛苦了，坐下来听一会儿"));

    private final UserRepository users;
    private final TrackRepository tracks;
    private final ZoneRepository zones;
    private final ZoneMemberRepository members;
    private final QueueItemRepository queue;
    private final MomentRepository moments;
    private final Clock clock;
    private final MusicProperties musicProperties;
    private final TrackDurationPolicy durations;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, User> cast = new LinkedHashMap<>();
        for (DemoUser spec : USERS) cast.put(spec.key(), upsertUser(spec));

        List<Track> localCatalog =
                tracks.findTop50BySourceOrderByIdAsc("LOCAL_LICENSED").stream()
                        .filter(t -> t.getExternalId() != null && !t.getTags().isEmpty())
                        .filter(durations::isAllowed)
                        .toList();
        List<Track> audiusCatalog =
                tracks.findTop50BySourceOrderByIdAsc("AUDIUS").stream()
                        .filter(t -> t.getExternalId() != null && !t.getTags().isEmpty())
                        .filter(durations::isAllowed)
                        .toList();
        List<Track> catalog;
        if (localCatalog.size() >= 3) catalog = localCatalog;
        else if (musicProperties.getAudius().isEnabled() && audiusCatalog.size() >= 3)
            catalog = audiusCatalog;
        else {
            LinkedHashMap<Long, Track> combined = new LinkedHashMap<>();
            localCatalog.forEach(t -> combined.put(t.getId(), t));
            if (musicProperties.getAudius().isEnabled())
                audiusCatalog.forEach(t -> combined.putIfAbsent(t.getId(), t));
            catalog = combined.size() >= 3 ? new ArrayList<>(combined.values()) : fallbackCatalog();
        }

        List<User> listeners = new ArrayList<>(cast.values());
        for (int i = 0; i < ZONES.size(); i++) {
            DemoZone spec = ZONES.get(i);
            User host = cast.get(spec.hostKey());
            List<Track> playlist = playlist(catalog, spec.preferredTags(), i * 2);
            Zone zone = upsertZone(spec, host, playlist);
            ensureMembers(zone, host, listeners, i);
            ensurePlaylist(zone, host, playlist);
            ensureMoment(zone, host, playlist.get(0), spec.momentText(), i);
        }
    }

    private User upsertUser(DemoUser spec) {
        String subject = "demo:" + spec.key();
        User user =
                users.findByHostSubject(subject)
                        .orElseGet(
                                () ->
                                        users.findByName(spec.name())
                                                .filter(u -> u.getHostSubject() == null)
                                                .orElseGet(User::new));
        user.setName(spec.name());
        user.setHostSubject(subject);
        user.setAvatarColor(spec.color());
        return users.save(user);
    }

    private Zone upsertZone(DemoZone spec, User host, List<Track> playlist) {
        Zone zone =
                zones.findFirstByHostIdAndDemoResidentTrue(host.getId()).orElseGet(Zone::new);
        zone.setName(spec.name());
        zone.setScene(spec.scene());
        zone.setHost(host);
        zone.setCoverColor(spec.color());
        zone.setVisibility(ZoneVisibility.PUBLIC);
        zone.setFilterMode(FilterMode.ALLOW);
        zone.setDemoResident(true);
        zone.setStatus(ZoneStatus.ACTIVE);
        zone.setEndedAt(null);
        zone.setLastActivityAt(LocalDateTime.now(clock));
        Set<String> allowed = new LinkedHashSet<>(spec.preferredTags());
        for (Track track : playlist) allowed.addAll(track.getTags());
        zone.getFilterTags().clear();
        zone.getFilterTags().addAll(allowed);
        zone.getTags().clear();
        zone.getTags().addAll(allowed);
        return zones.save(zone);
    }

    private void ensureMembers(Zone zone, User host, List<User> cast, int offset) {
        LinkedHashSet<User> selected = new LinkedHashSet<>();
        selected.add(host);
        for (int i = 0; i < 4 + offset; i++) selected.add(cast.get((i + offset) % cast.size()));
        LocalDateTime now = LocalDateTime.now(clock);
        for (User user : selected) {
            ZoneMember member =
                    members.findByZoneIdAndUserId(zone.getId(), user.getId())
                            .orElseGet(ZoneMember::new);
            member.setZone(zone);
            member.setUser(user);
            member.setLastSeenAt(now);
            members.save(member);
        }
        zone.setListenerCount((int) members.countByZoneId(zone.getId()));
    }

    private void ensurePlaylist(Zone zone, User host, List<Track> playlist) {
        Set<Long> selectedIds = playlist.stream().map(Track::getId).collect(java.util.stream.Collectors.toSet());
        for (QueueItem old :
                queue.findByZoneIdAndRequesterIdOrderByCreatedAtAscIdAsc(
                        zone.getId(), host.getId())) {
            if (!selectedIds.contains(old.getTrack().getId())) old.setStatus(QueueStatus.REMOVED);
            else if (old.getStatus() == QueueStatus.REMOVED) old.setStatus(QueueStatus.QUEUED);
        }
        for (Track track : playlist) {
            if (queue.existsByZoneIdAndTrackIdAndRequesterId(
                    zone.getId(), track.getId(), host.getId())) continue;
            QueueItem item = new QueueItem();
            item.setZone(zone);
            item.setTrack(track);
            item.setRequester(host);
            item.setStatus(QueueStatus.QUEUED);
            queue.save(item);
        }
        if (queue.findFirstByZoneIdAndStatus(zone.getId(), QueueStatus.PLAYING).isEmpty()) {
            List<QueueItem> waiting =
                    queue.findByZoneIdAndStatusOrderByCreatedAtAscIdAsc(
                            zone.getId(), QueueStatus.QUEUED);
            if (waiting.isEmpty()) {
                waiting =
                        queue.findByZoneIdAndStatusOrderByCreatedAtAscIdAsc(
                                zone.getId(), QueueStatus.PLAYED);
                for (QueueItem item : waiting) {
                    item.setStatus(QueueStatus.QUEUED);
                    item.setStartedAt(null);
                    item.setPlayedAt(null);
                }
                queue.saveAllAndFlush(waiting);
            }
            if (!waiting.isEmpty()) {
                QueueItem first = waiting.get(0);
                first.setStatus(QueueStatus.PLAYING);
                first.setStartedAt(LocalDateTime.now(clock).minusSeconds(15));
                queue.save(first);
            }
        }
    }

    private void ensureMoment(
            Zone zone, User host, Track track, String text, int minuteOffset) {
        Moment moment =
                moments.findFirstByZoneIdAndText(zone.getId(), text).orElseGet(Moment::new);
        moment.setZone(zone);
        moment.setUser(host);
        moment.setTrack(track);
        moment.setText(text);
        moment.setColor(zone.getCoverColor());
        if (moment.getId() == null)
            moment.setCreatedAt(LocalDateTime.now(clock).minusMinutes(3L + minuteOffset));
        moment.setModerationStatus(ModerationStatus.APPROVED);
        moments.save(moment);
    }

    private List<Track> playlist(List<Track> catalog, Set<String> preferred, int offset) {
        List<Track> matches =
                catalog.stream()
                        .filter(t -> t.getTags().stream().anyMatch(preferred::contains))
                        .toList();
        List<Track> source = matches.size() >= 3 ? matches : catalog;
        List<Track> selected = new ArrayList<>();
        for (int i = 0; i < Math.min(4, source.size()); i++)
            selected.add(source.get((offset + i) % source.size()));
        return selected;
    }

    private List<Track> fallbackCatalog() {
        return List.of(
                fallback("calm-sky", "Calm Sky", "SoundZone Demo", Set.of("舒缓", "电子")),
                fallback("soft-focus", "Soft Focus", "SoundZone Demo", Set.of("舒缓", "专注")),
                fallback("night-run", "Night Run", "SoundZone Demo", Set.of("亢奋", "电子")),
                fallback("city-lights", "City Lights", "SoundZone Demo", Set.of("流行", "电子")),
                fallback("slow-trip", "Slow Trip", "SoundZone Demo", Set.of("旅行", "舒缓")),
                fallback("after-work", "After Work", "SoundZone Demo", Set.of("说唱", "流行")));
    }

    private Track fallback(String key, String title, String artist, Set<String> tags) {
        Track track =
                tracks.findFirstBySourceAndExternalId("MOCK", "demo:" + key)
                        .orElseGet(Track::new);
        track.setTitle(title);
        track.setArtist(artist);
        track.setSource("MOCK");
        track.setExternalId("demo:" + key);
        track.setAttribution("SoundZone 演示元数据");
        track.setDurationSec(210);
        track.getTags().clear();
        track.getTags().addAll(tags);
        return tracks.save(track);
    }

    private record DemoUser(String key, String name, String color) {}

    private record DemoZone(
            String hostKey,
            String name,
            String scene,
            String color,
            Set<String> preferredTags,
            String momentText) {}
}
