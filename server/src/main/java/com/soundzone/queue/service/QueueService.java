package com.soundzone.queue.service;

import com.soundzone.activity.service.ActivityService;
import com.soundzone.common.*;
import com.soundzone.queue.dto.*;
import com.soundzone.notification.service.NotificationService;
import com.soundzone.queue.entity.*;
import com.soundzone.queue.repository.*;
import com.soundzone.realtime.ZoneEvent;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.track.service.TrackDurationPolicy;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.entity.*;
import com.soundzone.zone.service.PomodoroService;
import com.soundzone.zone.service.ZoneAccess;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class QueueService {
    private static final int UPLOAD_COOLDOWN_MINUTES = 5;
    private static final long UPLOAD_COOLDOWN_SECONDS =
            Duration.ofMinutes(UPLOAD_COOLDOWN_MINUTES).toSeconds();

    private final QueueItemRepository queue;
    private final QueueLikeRepository likes;
    private final ZoneAccess access;
    private final TrackRepository tracks;
    private final UserRepository users;
    private final PomodoroService pomodoro;
    private final TagPolicy policy;
    private final TrackDurationPolicy durations;
    private final PlaybackService playback;
    private final ActivityService activity;
    private final ApplicationEventPublisher events;
    private final NotificationService notifications;
    private final Clock clock;

    public QueueItemDTO requestSong(Long zoneId, SongRequest req, Long userId) {
        Zone zone = access.lock(zoneId);
        access.member(zoneId, userId);
        // 先结算已经自然播完的歌曲。若结算后已无当前曲和待播曲，立即开放补歌，
        // 避免最后一首提前结束后仍被个人冷却时间卡住。
        playback.advanceLocked(zone);
        long remain = cooldownRemainSecondsLocked(zoneId, userId);
        if (remain > 0)
            throw new BizException(
                    ResultCode.UPLOAD_COOLDOWN,
                    "请等待冷却结束",
                    new CooldownDTO(remain, UPLOAD_COOLDOWN_MINUTES));
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
        if (!pomodoro.allowsNow(zone, track)) item.setStatus(QueueStatus.PRESET);
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
            if (!userId.equals(item.getRequester().getId())) {
                notifications.trackLiked(item, like.getUser());
                events.publishEvent(
                        new ZoneEvent(zoneId, "GLOW", item.getRequester().getId(), itemId));
            }
        } else if (!active && previous.isPresent()) {
            likes.delete(previous.get());
            item.setLikes(Math.max(0, item.getLikes() - 1));
            playback.changed(zone);
        }
        return QueueItemDTO.from(item, null, active);
    }

    public void withdraw(Long zoneId, Long itemId, Long userId) {
        Zone zone = access.lock(zoneId);
        access.member(zoneId, userId);
        QueueItem item =
                queue.findById(itemId)
                        .orElseThrow(() -> new BizException(ResultCode.QUEUE_ITEM_NOT_FOUND));
        if (!item.getZone().getId().equals(zoneId)) throw new BizException(ResultCode.FORBIDDEN);
        if (!item.getRequester().getId().equals(userId))
            throw new BizException(ResultCode.NOT_RESOURCE_OWNER);
        if (!List.of(QueueStatus.QUEUED, QueueStatus.PRESET).contains(item.getStatus()))
            throw new BizException(ResultCode.PARAM_INVALID, "只能撤回尚未播放的歌曲");
        item.setStatus(QueueStatus.REMOVED);
        playback.changed(zone);
    }

    public long cooldownRemainSeconds(Long zoneId, Long userId) {
        Zone zone = access.lock(zoneId);
        access.member(zoneId, userId);
        playback.advanceLocked(zone);
        return cooldownRemainSecondsLocked(zoneId, userId);
    }

    private long cooldownRemainSecondsLocked(Long zoneId, Long userId) {
        if (!queue.existsByZoneIdAndStatusIn(
                zoneId, List.of(QueueStatus.PLAYING, QueueStatus.QUEUED))) return 0L;
        return queue.findFirstByZoneIdAndRequesterIdOrderByCreatedAtDescIdDesc(zoneId, userId)
                .map(
                        q ->
                                Math.max(
                                        0,
                                        UPLOAD_COOLDOWN_SECONDS
                                                - Duration.between(
                                                                q.getCreatedAt(),
                                                                LocalDateTime.now(clock))
                                                        .getSeconds()))
                .orElse(0L);
    }

    public CooldownDTO cooldown(Long zoneId, Long userId) {
        return new CooldownDTO(
                cooldownRemainSeconds(zoneId, userId), UPLOAD_COOLDOWN_MINUTES);
    }
}
