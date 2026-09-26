package com.soundzone.feedback.service;

import com.soundzone.activity.service.ActivityService;
import com.soundzone.common.*;
import com.soundzone.feedback.entity.*;
import com.soundzone.feedback.repository.*;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.queue.service.PlaybackService;
import com.soundzone.realtime.ZoneEvent;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.service.ZoneAccess;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {
    private final TrackCollectionRepository collections;
    private final FeedbackEventRepository feedback;
    private final ReportRepository reports;
    private final QueueItemRepository queue;
    private final UserRepository users;
    private final ZoneAccess access;
    private final PlaybackService playback;
    private final ApplicationEventPublisher events;
    private final ActivityService activity;

    /** 幂等设置收藏状态；首次新增且仍在播放时才通知本次上传者。 */
    public boolean collect(Long zoneId, Long itemId, Long userId, boolean active) {
        users.lockById(userId).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        var zone = access.lock(zoneId);
        access.member(zoneId, userId);
        playback.advanceLocked(zone);
        var item =
                queue.findById(itemId)
                        .orElseThrow(() -> new BizException(ResultCode.QUEUE_ITEM_NOT_FOUND));
        if (!item.getZone().getId().equals(zoneId)) throw new BizException(ResultCode.FORBIDDEN);
        var previous = collections.findByUserIdAndTrackId(userId, item.getTrack().getId());
        if (active && previous.isEmpty()) {
            if (item.getStatus() != QueueStatus.PLAYING)
                throw new BizException(ResultCode.PARAM_INVALID, "歌曲已结束，请刷新播放状态");
            TrackCollection c = new TrackCollection();
            c.setUser(users.getReferenceById(userId));
            c.setTrack(item.getTrack());
            collections.save(c);
            FeedbackEvent e = new FeedbackEvent();
            e.setZone(zone);
            e.setTrack(item.getTrack());
            e.setQueueItem(item);
            e.setFromUser(c.getUser());
            e.setToUser(item.getRequester());
            e.setType(FeedbackType.COLLECT);
            feedback.save(e);
            activity.record(userId, zoneId, itemId, "COLLECT", 0);
            events.publishEvent(new ZoneEvent(zoneId, "GLOW", item.getRequester().getId(), itemId));
            playback.changed(zone);
        } else if (!active && previous.isPresent()) {
            collections.delete(previous.get());
            playback.changed(zone);
        }
        return active;
    }

    public void removeCollection(Long trackId, Long userId) {
        users.lockById(userId).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        collections.findByUserIdAndTrackId(userId, trackId).ifPresent(collections::delete);
    }

    public Long report(Long zoneId, Long userId, String reason) {
        var zone = access.member(zoneId, userId);
        if (reason == null || reason.isBlank() || reason.length() > 500)
            throw new BizException(ResultCode.PARAM_INVALID, "请填写 1–500 字举报原因");
        Report report = new Report();
        report.setZone(zone);
        report.setUser(users.getReferenceById(userId));
        report.setReason(reason.strip());
        return reports.save(report).getId();
    }
}
