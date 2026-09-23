package com.soundzone.queue.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.queue.dto.QueueItemDTO;
import com.soundzone.queue.dto.SongRequest;
import com.soundzone.queue.entity.QueueItem;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.entity.ZonePeriod;
import com.soundzone.zone.entity.ZoneStatus;
import com.soundzone.zone.repository.ZonePeriodRepository;
import com.soundzone.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 队列服务（docs/02 第 3 步，核心机制）
 *
 * 队列得分公式：score = 点赞数 × 2 + 域主加成 × 3 − 该用户近 1 小时已播点歌数 × 1.5
 * 准入校验：曲风黑名单（决议 D1）→ 直接拒绝；
 *           番茄钟时段白名单（决议 D3）→ 不符合则入预存队列
 */
@Service
@RequiredArgsConstructor
public class QueueService {

    private static final int LIKE_WEIGHT = 2;
    private static final int HOST_BONUS_WEIGHT = 3;
    private static final double RECENT_PLAYED_PENALTY = 1.5;

    private final QueueItemRepository queueItemRepository;
    private final ZoneRepository zoneRepository;
    private final ZonePeriodRepository periodRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    /** 点歌：黑名单校验 → 时段白名单校验 → 入队并计算初始得分 */
    @Transactional
    public QueueItemDTO requestSong(Long zoneId, SongRequest req) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new BizException(ResultCode.ZONE_NOT_FOUND));
        if (zone.getStatus() == ZoneStatus.ENDED) {
            throw new BizException(ResultCode.ZONE_ALREADY_ENDED);
        }
        Track track = trackRepository.findById(req.trackId())
                .orElseThrow(() -> new BizException(ResultCode.TRACK_NOT_FOUND));
        User requester = userRepository.findById(req.userId())
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        // ① 曲风黑名单校验（docs/02 决议 D1）：命中即拒绝
        Set<String> banned = zone.getBannedTags();
        if (banned != null && track.getTags().stream().anyMatch(banned::contains)) {
            throw new BizException(ResultCode.SONG_BANNED_BY_ZONE,
                    "曲目「" + track.getTitle() + "」标签 " + track.getTags() + " 命中本域黑名单");
        }

        QueueItem item = new QueueItem();
        item.setZone(zone);
        item.setTrack(track);
        item.setRequester(requester);
        boolean isHost = zone.getHost().getId().equals(requester.getId());
        item.setHostBonus(isHost ? 1 : 0);

        // ② 番茄钟时段白名单校验（决议 D3）：不符合当前时段 → 预存队列
        if (!allowedInCurrentPeriod(zone, track)) {
            item.setStatus(QueueStatus.PRESET);
        }

        item.setScore(calcScore(item, zoneId, requester.getId()));
        item = queueItemRepository.save(item);
        return toDTO(item, null);
    }

    /** 点赞：+1 赞并重算得分（正式版同步广播 queue_version+1） */
    @Transactional
    public QueueItemDTO like(Long itemId, Long userId) {
        QueueItem item = queueItemRepository.findById(itemId)
                .orElseThrow(() -> new BizException(ResultCode.QUEUE_ITEM_NOT_FOUND));
        item.setLikes(item.getLikes() + 1);
        item.setScore(calcScore(item, item.getZone().getId(), item.getRequester().getId()));
        queueItemRepository.save(item);
        return toDTO(item, null);
    }

    /** 队列得分（公式见类注释） */
    private double calcScore(QueueItem item, Long zoneId, Long requesterId) {
        long recentPlayed = queueItemRepository.countByZoneIdAndRequesterIdAndStatusAndPlayedAtAfter(
                zoneId, requesterId, QueueStatus.PLAYED, LocalDateTime.now().minusHours(1));
        return item.getLikes() * LIKE_WEIGHT
                + item.getHostBonus() * HOST_BONUS_WEIGHT
                - recentPlayed * RECENT_PLAYED_PENALTY;
    }

    /**
     * 判断曲目是否符合"当前时段"白名单
     * 无时段配置 / 当前时段白名单为空 → 放行；
     * 【假设】当前时段按 域创建至今的分钟数 对各时段循环取模推算；
     *        正式版由服务端权威时钟统一推进并广播时段切换事件
     */
    private boolean allowedInCurrentPeriod(Zone zone, Track track) {
        List<ZonePeriod> periods = periodRepository.findByZoneIdOrderByOrderIndexAsc(zone.getId());
        if (periods.isEmpty()) return true;

        long cycleMin = periods.stream().mapToLong(ZonePeriod::getDurationMin).sum();
        long elapsed = Duration.between(zone.getCreatedAt(), LocalDateTime.now()).toMinutes() % cycleMin;

        long acc = 0;
        for (ZonePeriod p : periods) {
            acc += p.getDurationMin();
            if (elapsed < acc) {
                Set<String> allowed = p.getAllowedTags();
                if (allowed == null || allowed.isEmpty()) return true;
                return track.getTags().stream().anyMatch(allowed::contains);
            }
        }
        return true;
    }

    private QueueItemDTO toDTO(QueueItem q, Integer rank) {
        return new QueueItemDTO(q.getId(), rank, q.getTrack().getTitle(),
                q.getTrack().getArtist(), q.getLikes(),
                q.getRequester().getName(), q.getStatus().name());
    }
}
