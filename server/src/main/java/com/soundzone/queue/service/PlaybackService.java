package com.soundzone.queue.service;

import com.soundzone.queue.entity.*;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.realtime.ZoneEvent;
import com.soundzone.track.service.TrackDurationPolicy;
import com.soundzone.zone.entity.*;
import com.soundzone.zone.repository.*;
import com.soundzone.zone.service.ZoneAccess;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

/** 单域数据库行锁下推进，客户端不能决定切歌。无需 Redis 或队列排序模型。 */
@Service
@RequiredArgsConstructor
public class PlaybackService {
    private final ZoneAccess access;
    private final QueueItemRepository queue;
    private final ZoneMemberRepository members;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final TrackDurationPolicy durations;

    @Value("${soundzone.presence-grace-seconds:90}")
    private long graceSeconds;

    @Transactional
    public void tick(Long zoneId) {
        Zone zone = access.lock(zoneId);
        if (zone.getStatus() != ZoneStatus.ACTIVE) return;
        LocalDateTime now = LocalDateTime.now(clock);
        boolean changed = false;
        for (ZoneMember member : members.findByZoneId(zoneId)) {
            if (zone.isDemoResident()) member.setLastSeenAt(now);
            else if (member.getLastSeenAt() == null) member.setLastSeenAt(now); // 旧成员迁移宽限
            else if (member.getLastSeenAt().isBefore(now.minusSeconds(graceSeconds))) {
                members.delete(member);
                changed = true;
            }
        }
        members.flush();
        int count = (int) members.countByZoneId(zoneId);
        if (zone.getListenerCount() != count) {
            zone.setListenerCount(count);
            changed = true;
        }
        if (count == 0 && !zone.isDemoResident()) {
            closeLocked(zone);
            return;
        }
        if (zone.isDemoResident()) {
            zone.setLastActivityAt(now);
            recycleDemoPlaylist(zone);
        }
        if (changed) changed(zone);
        advanceLocked(zone);
    }

    /** 常驻演示域在整轮播放结束后按原 FIFO 顺序循环，普通域不进入此分支。 */
    private void recycleDemoPlaylist(Zone zone) {
        if (queue.findFirstByZoneIdAndStatus(zone.getId(), QueueStatus.PLAYING).isPresent()
                || !queue.findByZoneIdAndStatusOrderByCreatedAtAscIdAsc(
                                zone.getId(), QueueStatus.QUEUED)
                        .isEmpty()) return;
        var played =
                queue.findByZoneIdAndStatusOrderByCreatedAtAscIdAsc(
                        zone.getId(), QueueStatus.PLAYED);
        for (var item : played) {
            item.setStatus(
                    durations.isAllowed(item.getTrack())
                            ? QueueStatus.QUEUED
                            : QueueStatus.REMOVED);
            item.setStartedAt(null);
            item.setPlayedAt(null);
        }
        if (!played.isEmpty()) queue.saveAllAndFlush(played);
    }

    /** 调用方必须已持有域行锁并处于事务内。延迟 tick 按自然结束时间连续补偿。 */
    public void advanceLocked(Zone zone) {
        if (zone.getStatus() != ZoneStatus.ACTIVE) return;
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nextStart = now;
        for (int i = 0; i < 1000; i++) {
            QueueItem current =
                    queue.findFirstByZoneIdAndStatus(zone.getId(), QueueStatus.PLAYING)
                            .orElse(null);
            if (current != null && !durations.isAllowed(current.getTrack())) {
                removeOverlong(current);
                current = null;
                nextStart = now;
                changed(zone);
            }
            if (current != null) {
                if (current.getStartedAt() == null) current.setStartedAt(now);
                LocalDateTime end =
                        current.getStartedAt()
                                .plusSeconds(Math.max(1, current.getTrack().getDurationSec()));
                if (end.isAfter(now)) return;
                current.setStatus(QueueStatus.PLAYED);
                current.setPlayedAt(end);
                queue.saveAndFlush(current);
                nextStart = end;
                changed(zone);
            }
            var waiting =
                    queue.findByZoneIdAndStatusOrderByCreatedAtAscIdAsc(
                            zone.getId(), QueueStatus.QUEUED);
            if (waiting.isEmpty()) return; // 静默等待，不补歌、不结束有成员的域
            QueueItem next = waiting.get(0);
            if (!durations.isAllowed(next.getTrack())) {
                removeOverlong(next);
                changed(zone);
                continue;
            }
            // 空队列恢复不能让新上传曲目从过去开始；延迟推进也不能早于上传时间。
            if (next.getCreatedAt().isAfter(nextStart)) nextStart = next.getCreatedAt();
            next.setStatus(QueueStatus.PLAYING);
            next.setStartedAt(nextStart);
            queue.saveAndFlush(next);
            changed(zone);
        }
    }

    /** 兼容限制上线前已存在的超长队列项；保留记录但不再参与播放。 */
    private void removeOverlong(QueueItem item) {
        item.setStatus(QueueStatus.REMOVED);
        item.setStartedAt(null);
        item.setPlayedAt(null);
        queue.saveAndFlush(item);
    }

    public void closeLocked(Zone zone) {
        if (zone.getStatus() == ZoneStatus.ENDED) return;
        zone.setStatus(ZoneStatus.ENDED);
        zone.setEndedAt(LocalDateTime.now(clock));
        zone.setListenerCount(0);
        queue.findFirstByZoneIdAndStatus(zone.getId(), QueueStatus.PLAYING)
                .ifPresent(
                        q -> {
                            q.setStatus(QueueStatus.STOPPED); // 提前消散不冒充播放完成
                            queue.save(q);
                        });
        zone.setStateVersion(zone.getStateVersion() + 1);
        events.publishEvent(new ZoneEvent(zone.getId(), "ENDED", null, null));
    }

    public void changed(Zone zone) {
        zone.setStateVersion(zone.getStateVersion() + 1);
        events.publishEvent(new ZoneEvent(zone.getId()));
    }
}
