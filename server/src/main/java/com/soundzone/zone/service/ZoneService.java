package com.soundzone.zone.service;

import com.soundzone.activity.service.ActivityService;
import com.soundzone.auth.service.PasswordService;
import com.soundzone.common.*;
import com.soundzone.feedback.repository.TrackCollectionRepository;
import com.soundzone.moment.service.MomentService;
import com.soundzone.queue.dto.QueueItemDTO;
import com.soundzone.queue.entity.*;
import com.soundzone.queue.repository.*;
import com.soundzone.queue.service.*;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.track.service.TrackDurationPolicy;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.dto.*;
import com.soundzone.zone.entity.*;
import com.soundzone.zone.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ZoneService {
    private final ZoneRepository zones;
    private final ZoneMemberRepository members;
    private final QueueItemRepository queue;
    private final QueueLikeRepository likes;
    private final TrackCollectionRepository collections;
    private final TrackRepository tracks;
    private final UserRepository users;
    private final ZoneAccess access;
    private final TagPolicy tags;
    private final TrackDurationPolicy durations;
    private final PasswordService passwords;
    private final PlaybackService playback;
    private final MomentService moments;
    private final ActivityService activity;
    private final Clock clock;

    @Value("${soundzone.active-window-minutes:30}")
    private int activeWindow;

    public List<ZoneSummaryDTO> listActive(String scene, String keyword) {
        String filter = normalizeScene(scene);
        String kw = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        LocalDateTime after = LocalDateTime.now(clock).minusMinutes(activeWindow);
        return zones
                .findByStatusAndVisibilityOrderByListenerCountDesc(
                        ZoneStatus.ACTIVE, ZoneVisibility.PUBLIC)
                .stream()
                .filter(
                        z ->
                                z.getListenerCount() > 0
                                        && z.getLastActivityAt() != null
                                        && !z.getLastActivityAt().isBefore(after))
                .filter(
                        z ->
                                scene == null
                                        || scene.isBlank()
                                        || "全部".equals(scene)
                                        || z.getScene().equals(filter)
                                        || positiveTag(z, scene)
                                        || ("日系".equals(scene) && positiveTag(z, "日语")))
                .map(this::summary)
                .filter(z -> z.nowPlaying() != null)
                .filter(
                        z ->
                                kw.isEmpty()
                                        || (z.name()
                                                        + z.scene()
                                                        + z.tags().stream().filter(tag -> !isBannedTag(z.filterMode(), z.filterTags(), tag)).toList()
                                                        + z.nowPlaying().title()
                                                        + z.nowPlaying().artist())
                                                .toLowerCase(Locale.ROOT)
                                                .contains(kw))
                .toList();
    }

    public ZoneDetailDTO createZone(ZoneCreateRequest req, Long userId) {
        if (req.trackIds() == null
                || req.trackIds().size() < 3
                || new HashSet<>(req.trackIds()).size() != req.trackIds().size())
            throw new BizException(ResultCode.PARAM_INVALID, "初始歌单至少三首且不能重复");
        if (req.periods() != null && !req.periods().isEmpty())
            throw new BizException(ResultCode.PARAM_INVALID, "番茄钟将在后续阶段开放");
        Zone zone = new Zone();
        zone.setName(req.name().strip());
        zone.setScene(normalizeScene(req.scene()));
        zone.setHost(
                users.findById(userId)
                        .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND)));
        zone.setFilterMode(
                req.filterMode() == null
                        ? FilterMode.BAN
                        : FilterMode.valueOf(req.filterMode().toUpperCase(Locale.ROOT)));
        zone.setVisibility(
                req.visibility() == null
                        ? ZoneVisibility.PUBLIC
                        : ZoneVisibility.valueOf(req.visibility().toUpperCase(Locale.ROOT)));
        if (req.filterTags() != null) zone.getFilterTags().addAll(req.filterTags());
        tags.validateForCreation(
                zone.getFilterMode(),
                zone.getFilterTags(),
                req.tags() == null ? Set.of() : req.tags());
        // 新建域只有一组标签：允许模式用于正向展示；禁止模式不能作为推荐标签。
        if (zone.getFilterMode() == FilterMode.ALLOW)
            zone.getTags().addAll(zone.getFilterTags());
        zone.setCoverColor(themeColor(zone.getScene()));
        List<Track> initial = new ArrayList<>();
        for (Long id : req.trackIds()) { // 查询后按用户所选顺序入队，不依赖 IN 查询返回顺序
            Track track =
                    tracks.findById(id)
                            .orElseThrow(() -> new BizException(ResultCode.TRACK_NOT_FOUND));
            durations.check(track);
            tags.check(zone, track);
            initial.add(track);
        }
        if (zone.getVisibility() == ZoneVisibility.PRIVATE) {
            if (req.password() != null && !req.password().isBlank())
                zone.setPasswordHash(passwords.hash(req.password()));
            zone.setInviteCode(UUID.randomUUID().toString().replace("-", ""));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        zone.setCreatedAt(now);
        zone.setLastActivityAt(now);
        zones.saveAndFlush(zone);
        addMember(zone, userId);
        for (Track track : initial) {
            QueueItem item = new QueueItem();
            item.setZone(zone);
            item.setTrack(track);
            item.setRequester(zone.getHost());
            item.setCreatedAt(now);
            queue.save(item); // 同一时间以自增 ID 保证稳定顺序
        }
        queue.flush();
        playback.advanceLocked(zone);
        activity.record(userId, zone.getId(), null, "CREATE", 0);
        return detail(zone, userId);
    }

    public ZoneDetailDTO joinZone(Long zoneId, JoinZoneRequest req, Long userId) {
        Zone zone = access.lock(zoneId);
        access.active(zone);
        boolean already = members.existsByZoneIdAndUserId(zoneId, userId);
        if (!already
                && zone.getVisibility() == ZoneVisibility.PRIVATE
                && !zone.getHost().getId().equals(userId)) {
            // 旧密码原地摘要迁移；不再新增明文。历史列保留以便分批迁移。
            migratePassword(zone);
            boolean codeOk =
                    req.inviteCode() != null && req.inviteCode().equals(zone.getInviteCode());
            boolean pwdOk = passwords.matches(req.password(), zone.getPasswordHash());
            if (!codeOk && !pwdOk)
                throw new BizException(
                        req.password() == null && req.inviteCode() == null
                                ? ResultCode.ZONE_PRIVATE_NEED_AUTH
                                : ResultCode.ZONE_PASSWORD_WRONG);
        }
        if (!already) addMember(zone, userId);
        else
            members.findByZoneIdAndUserId(zoneId, userId)
                    .orElseThrow()
                    .setLastSeenAt(LocalDateTime.now(clock));
        playback.advanceLocked(zone);
        return detail(zone, userId);
    }

    public void leaveZone(Long zoneId, Long userId) {
        Zone zone = access.lock(zoneId);
        if (!members.existsByZoneIdAndUserId(zoneId, userId)) return;
        members.deleteByZoneIdAndUserId(zoneId, userId);
        members.flush();
        zone.setListenerCount((int) members.countByZoneId(zoneId));
        activity.record(userId, zoneId, null, "LEAVE", 0);
        if (zone.getListenerCount() == 0) playback.closeLocked(zone);
        else playback.changed(zone);
    }

    public void heartbeat(Long zoneId, Long userId, Long itemId, boolean playing) {
        Zone zone = access.lock(zoneId);
        access.member(zoneId, userId);
        var member = members.findByZoneIdAndUserId(zoneId, userId).orElseThrow();
        var now = LocalDateTime.now(clock);
        long seconds =
                member.getLastSeenAt() == null
                        ? 0
                        : Math.max(0, Duration.between(member.getLastSeenAt(), now).getSeconds());
        if (playing
                && itemId != null
                && seconds <= 30
                && queue.findFirstByZoneIdAndStatus(zoneId, QueueStatus.PLAYING)
                        .map(q -> q.getId().equals(itemId))
                        .orElse(false)) activity.record(userId, zoneId, itemId, "LISTEN", seconds);
        member.setLastSeenAt(now);
        playback.advanceLocked(zone);
    }

    public ZoneDetailDTO getDetail(Long zoneId, Long userId) {
        Zone zone = access.lock(zoneId);
        access.member(zoneId, userId);
        playback.advanceLocked(zone);
        return detail(zone, userId);
    }

    public String invite(Long zoneId, Long userId) {
        access.lock(zoneId);
        Zone zone = access.host(zoneId, userId);
        if (zone.getVisibility() != ZoneVisibility.PRIVATE) return "";
        return zone.getInviteCode();
    }

    public ZoneDetailDTO update(Long zoneId, Long userId, ZoneUpdateRequest req) {
        Zone zone = access.lock(zoneId);
        access.host(zoneId, userId);
        Set<String> display = req.tags() == null ? zone.getTags() : req.tags();
        tags.validate(zone.getFilterMode(), zone.getFilterTags(), display);
        zone.setName(req.name().strip());
        zone.setScene(normalizeScene(req.scene()));
        if (req.tags() != null) {
            zone.getTags().clear();
            zone.getTags().addAll(display);
        }
        zone.setCoverColor(themeColor(zone.getScene()));
        playback.changed(zone);
        return detail(zone, userId);
    }

    private void addMember(Zone zone, Long userId) {
        ZoneMember m = new ZoneMember();
        m.setZone(zone);
        m.setUser(users.getReferenceById(userId));
        m.setJoinedAt(LocalDateTime.now(clock));
        m.setLastSeenAt(m.getJoinedAt());
        members.saveAndFlush(m);
        zone.setListenerCount((int) members.countByZoneId(zone.getId()));
        playback.changed(zone);
        activity.record(userId, zone.getId(), null, "JOIN", 0);
    }

    private ZoneDetailDTO detail(Zone zone, Long userId) {
        var queued =
                queue.findByZoneIdAndStatusOrderByCreatedAtAscIdAsc(
                        zone.getId(), QueueStatus.QUEUED);
        List<QueueItemDTO> items = new ArrayList<>();
        for (int i = 0; i < queued.size(); i++)
            items.add(
                    QueueItemDTO.from(
                            queued.get(i),
                            i + 1,
                            likes.existsByUserIdAndItemId(userId, queued.get(i).getId())));
        QueueItemDTO candidate =
                queue.findFirstByZoneIdAndRequesterIdOrderByCreatedAtDescIdDesc(
                                zone.getId(), userId)
                        .filter(
                                q ->
                                        !q.getCreatedAt()
                                                .isBefore(
                                                        LocalDateTime.now(clock).minusMinutes(10)))
                        .filter(q -> !moments.hasImage(q.getId()))
                        .map(q -> QueueItemDTO.from(q, null, false))
                        .orElse(null);
        return new ZoneDetailDTO(
                zone.getId(),
                zone.getName(),
                zone.getScene(),
                zone.getListenerCount(),
                zone.getHost().getName(),
                zone.getHost().getId(),
                zone.getCoverColor(),
                zone.getVisibility().name(),
                zone.getTags(),
                zone.getFilterMode().name(),
                zone.getFilterTags(),
                currentPlaying(zone.getId(), userId),
                items,
                moments.feed(zone.getId(), userId).stream().limit(2).toList(),
                clock.millis(),
                zone.getStateVersion(),
                zone.getStatus().name(),
                candidate);
    }

    public NowPlayingDTO currentPlaying(Long zoneId, Long userId) {
        return queue.findFirstByZoneIdAndStatus(zoneId, QueueStatus.PLAYING)
                .map(
                        q -> {
                            var t = q.getTrack();
                            long start =
                                    Times.millis(
                                            q.getStartedAt() == null
                                                    ? LocalDateTime.now(clock)
                                                    : q.getStartedAt());
                            int progress =
                                    (int)
                                            Math.max(
                                                    0,
                                                    Math.min(
                                                            100,
                                                            (clock.millis() - start)
                                                                    * 100
                                                                    / (Math.max(
                                                                                    1,
                                                                                    t
                                                                                            .getDurationSec())
                                                                            * 1000L)));
                            return new NowPlayingDTO(
                                    q.getId(),
                                    t.getId(),
                                    t.getTitle(),
                                    t.getArtist(),
                                    q.getRequester().getName(),
                                    q.getRequester().getId(),
                                    progress,
                                    start,
                                    t.getDurationSec(),
                                    t.getCoverUrl(),
                                    t.getSource(),
                                    t.getAttribution(),
                                    q.getLikes(),
                                    userId != null
                                            && likes.existsByUserIdAndItemId(userId, q.getId()),
                                    userId != null
                                            && collections.existsByUserIdAndTrackId(
                                                    userId, t.getId()));
                        })
                .orElse(null);
    }

    public ZoneSummaryDTO summary(Zone z) {
        return new ZoneSummaryDTO(
                z.getId(),
                z.getName(),
                z.getScene(),
                z.getListenerCount(),
                z.getHost().getName(),
                z.getCoverColor(),
                z.getVisibility().name(),
                z.getTags(),
                z.getFilterMode().name(),
                z.getFilterTags(),
                currentPlaying(z.getId(), null));
    }

    private boolean positiveTag(Zone zone, String tag) {
        return zone.getTags().contains(tag)
                && !isBannedTag(zone.getFilterMode().name(), zone.getFilterTags(), tag);
    }

    private boolean isBannedTag(String mode, Set<String> filterTags, String tag) {
        return "BAN".equals(mode) && filterTags.contains(tag);
    }

    private void migratePassword(Zone zone) {
        if (zone.getPasswordHash() == null && zone.getPassword() != null) {
            zone.setPasswordHash(passwords.hash(zone.getPassword()));
            // 过渡期保留旧列以便应用回滚；清理明文另行迁移。
        }
    }

    public String normalizeScene(String raw) {
        if (raw == null || raw.isBlank()) return "其他";
        String s = raw.strip();
        if (s.matches(".*(学习|自习|考研|刷题|图书馆).*")) return "自习";
        if (s.matches(".*(夜跑|健身|铁馆).*")) return "健身";
        if (s.toLowerCase(Locale.ROOT).contains("trip") || s.matches(".*(旅游|旅行).*")) return "旅行";
        if (s.matches(".*(拼豆|手作|手工).*")) return "手工";
        if (s.matches(".*(写代码|加班|工作).*")) return "工作";
        return s;
    }

    private String themeColor(String scene) {
        return switch (scene) {
            case "自习" -> "#A8B8C8";
            case "健身" -> "#A9C4B5";
            case "旅行" -> "#D9CFB8";
            case "日系" -> "#E3C9CD";
            case "深夜" -> "#C3B8D9";
            default -> "#A8B8C8";
        };
    }
}
