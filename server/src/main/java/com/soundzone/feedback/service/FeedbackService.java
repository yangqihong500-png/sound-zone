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
 * 反馈服务（v2：2026-09-24 决议 D7）
 * 收藏/点赞/emoji → 归属该曲上传者；收藏触发"微光提示"（正式版 WS 定向推送）
 * 域后战报保留（用户确认）；v1 歌品值体系已取消，不再计算点赞率资产
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
     * 对当前播放歌曲反馈（COLLECT / LIKE / EMOJI_HEART / EMOJI_LAUGH）
     * 归属规则：该曲在域内"当前播放条目"的上传者；
     * 仅 COLLECT 产生微光提示（决议 D7 克制的审美反馈）
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
        // 微光提示推送：WS 接入后在此定向推送给 to（docs/05 反馈服务）
    }

    /** 域后个人战报（保留）：按归属用户聚合 收藏/点赞/emoji */
    public ZoneReportDTO report(Long zoneId) {
        var zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new BizException(ResultCode.ZONE_NOT_FOUND));
        List<FeedbackEvent> events = feedbackRepository.findByZoneId(zoneId);

        Map<Long, long[]> agg = new LinkedHashMap<>();  // userId -> [collect, like, emoji]
        Map<Long, String> names = new LinkedHashMap<>();
        for (FeedbackEvent e : events) {
            long[] counts = agg.computeIfAbsent(e.getToUser().getId(), k -> new long[3]);
            names.putIfAbsent(e.getToUser().getId(), e.getToUser().getName());
            switch (e.getType()) {
                case COLLECT -> counts[0]++;
                case LIKE -> counts[1]++;
                case EMOJI_HEART, EMOJI_LAUGH -> counts[2]++;
            }
        }
        List<ZoneReportDTO.Entry> entries = agg.entrySet().stream()
                .map(en -> new ZoneReportDTO.Entry(
                        en.getKey(), names.get(en.getKey()),
                        queueItemRepository.countByZoneIdAndRequesterId(zoneId, en.getKey()),
                        en.getValue()[0], en.getValue()[1], en.getValue()[2]))
                .toList();
        return new ZoneReportDTO(zone.getId(), zone.getName(), entries);
    }
}
