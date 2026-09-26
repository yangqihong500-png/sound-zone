package com.soundzone.queue.service;

import com.soundzone.activity.service.ActivityService;
import com.soundzone.common.*;
import com.soundzone.queue.dto.*;
import com.soundzone.queue.entity.*;
import com.soundzone.queue.repository.*;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.track.service.TrackDurationPolicy;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.entity.*;
import com.soundzone.zone.repository.ZonePeriodRepository;
import com.soundzone.zone.service.ZoneAccess;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class QueueService {
    private final QueueItemRepository queue;
    private final QueueLikeRepository likes;
    private final ZoneAccess access;
    private final TrackRepository tracks;
    private final UserRepository users;
    private final ZonePeriodRepository periods;
    private final TagPolicy policy;
    private final TrackDurationPolicy durations;
    private final PlaybackService playback;
    private final ActivityService activity;
    private final Clock clock;

    public QueueItemDTO requestSong(Long zoneId, SongRequest req, Long userId) {
        Zone zone = access.lock(zoneId);
        access.member(zoneId, userId);
        long remain = cooldownRemainSeconds(zoneId, userId);
        if (remain > 0)
            throw new BizException(
                    ResultCode.UPLOAD_COOLDOWN, "请等待冷却结束", new CooldownDTO(remain, 10));
        var track =
                tracks.findById(req.trackId())
                        .orElseThrow(() -> new BizException(ResultCode.TRACK_NOT_FOUND));
        durations.check(track);
        policy.check(zone, track);
        QueueItem item = new QueueItem();
        item.setZone(zone);
        item.setTrack(track);
        item.setRequester(users.getReferenceById(userId));
        item.setCreatedAt(LocalDateTime.now(clock));
        // 旧域时段配置继续约束准入，保留 PRESET；新建时段在 MVP 中关闭。
        var schedule = periods.findByZoneIdOrderByOrderIndexAsc(zoneId);
        if (!schedule.isEmpty()) {
            long cycle = schedule.stream().mapToLong(p -> Math.max(1, p.getDurationMin())).sum();
            long offset =
                    Math.floorMod(
                            Duration.between(zone.getCreatedAt(), LocalDateTime.now(clock))
                                    .toMinutes(),
                            cycle);
            for (var p : schedule) {
                if (offset < Math.max(1, p.getDurationMin())) {
                    if (!p.getAllowedTags().isEmpty()
                            && track.getTags().stream().noneMatch(p.getAllowedTags()::contains))
                        item.setStatus(QueueStatus.PRESET);
                    break;
                }
                offset -= Math.max(1, p.getDurationMin());
            }
        }
        queue.saveAndFlush(item);
        zone.setLastActivityAt(LocalDateTime.now(clock));
        activity.record(userId, zoneId, item.getId(), "UPLOAD", 0);
        playback.changed(zone);
        playback.advanceLocked(zone);
        return QueueItemDTO.from(item, null, false);
    }

    public QueueItemDTO like(Long zoneId, Long itemId, Long userId, boolean active) {
        Zone zone = access.lock(zoneId);
        access.member(zoneId, userId);
        QueueItem item = queue.findById(itemId).orElseThrow();
        if (!item.getZone().getId().equals(zoneId)) throw new BizException(ResultCode.FORBIDDEN);
        var previous = likes.findByUserIdAndItemId(userId, itemId);
        if (active && previous.isEmpty()) {
            QueueLike like = new QueueLike();
            like.setUser(users.getReferenceById(userId));
            like.setItem(item);
            likes.save(like);
            item.setLikes(item.getLikes() + 1);
            activity.record(userId, zoneId, itemId, "LIKE", 0);
            playback.changed(zone);
        } else if (!active && previous.isPresent()) {
            likes.delete(previous.get());
            item.setLikes(Math.max(0, item.getLikes() - 1));
            playback.changed(zone);
        }
        return QueueItemDTO.from(item, null, active);
    }

    public long cooldownRemainSeconds(Long zoneId, Long userId) {
        access.member(zoneId, userId);
        return queue.findFirstByZoneIdAndRequesterIdOrderByCreatedAtDescIdDesc(zoneId, userId)
                .map(
                        q ->
                                Math.max(
                                        0,
                                        600
                                                - Duration.between(
                                                                q.getCreatedAt(),
                                                                LocalDateTime.now(clock))
                                                        .getSeconds()))
                .orElse(0L);
    }
}
