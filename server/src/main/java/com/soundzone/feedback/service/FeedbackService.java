package com.soundzone.feedback.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.feedback.dto.HeartRequest;
import com.soundzone.feedback.dto.ZoneReportDTO;
import com.soundzone.feedback.entity.FeedbackEvent;
import com.soundzone.feedback.entity.FeedbackType;
import com.soundzone.feedback.repository.FeedbackEventRepository;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 反馈服务（docs/02 决议 D2 审美反馈）
 * 红心/收藏 → 记录事件并归属给该曲点歌人（实时特效的接收方）；
 * 域结束 → 事件聚合为个人战报；同时驱动歌品值（docs/02 成长体系）
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackEventRepository feedbackRepository;
    private final QueueItemRepository queueItemRepository;
    private final ZoneRepository zoneRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    /**
     * 红心/收藏当前播放歌曲
     * 【假设】归属规则：优先取该曲在域内"当前播放条目"的点歌人；
     *        无播放条目时取该曲最近一次点歌的点歌人
     * 正式版此处同步 WS 定向推送特效消息给 toUser（docs/05 反馈服务）
     */
    @Transactional
    public void heart(Long zoneId, HeartRequest req) {
        var zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new BizException(ResultCode.ZONE_NOT_FOUND));
        var track = trackRepository.findById(req.trackId())
                .orElseThrow(() -> new BizException(ResultCode.TRACK_NOT_FOUND));
        User from = userRepository.findById(req.userId())
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        User to = queueItemRepository
                .findFirstByZoneIdAndStatus(zoneId, QueueStatus.PLAYING)
                .filter(q -> q.getTrack().getId().equals(track.getId()))
                .map(q -> q.getRequester())
                .orElseThrow(() -> new BizException(ResultCode.PARAM_INVALID,
                        "该歌曲不是当前播放曲目，无法反馈"));

        FeedbackEvent event = new FeedbackEvent();
        event.setZone(zone);
        event.setTrack(track);
        event.setFromUser(from);
        event.setToUser(to);
        event.setType(FeedbackType.valueOf(req.type().toUpperCase()));
        feedbackRepository.save(event);

        // 被认可即时反映到歌品值（点赞率的正向事件）
        recalcTasteScore(to.getId());
    }

    /** 域后个人战报：按归属用户聚合红心眼/收藏/特效次数 */
    public ZoneReportDTO report(Long zoneId) {
        var zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new BizException(ResultCode.ZONE_NOT_FOUND));
        List<FeedbackEvent> events = feedbackRepository.findByZoneId(zoneId);

        Map<Long, ZoneReportDTO.Entry> agg = new LinkedHashMap<>();
        for (FeedbackEvent e : events) {
            ZoneReportDTO.Entry cur = agg.get(e.getToUser().getId());
            long hearts = (cur != null ? cur.heartCount() : 0) + (e.getType() == FeedbackType.HEART ? 1 : 0);
            long collects = (cur != null ? cur.collectCount() : 0) + (e.getType() == FeedbackType.COLLECT ? 1 : 0);
            long requested = queueItemRepository.countByRequesterId(e.getToUser().getId());
            agg.put(e.getToUser().getId(), new ZoneReportDTO.Entry(
                    e.getToUser().getId(), e.getToUser().getName(),
                    requested, hearts, collects, hearts + collects));
        }
        return new ZoneReportDTO(zone.getId(), zone.getName(), List.copyOf(agg.values()));
    }

    /**
     * 歌品值 = 点歌被点赞率 × 100（docs/02 成长体系）
     * 【假设】Demo 简化为：获赞总数 / 点歌总数（无点歌时为 0），上限 100
     */
    @Transactional
    public double recalcTasteScore(Long userId) {
        long requests = queueItemRepository.countByRequesterId(userId);
        long likes = queueItemRepository.sumLikesByRequesterId(userId);
        double score = requests == 0 ? 0 : Math.min(100.0, likes * 100.0 / requests);
        userRepository.findById(userId).ifPresent(u -> {
            u.setTasteScore(Math.round(score * 10) / 10.0);
            userRepository.save(u);
        });
        return score;
    }
}
