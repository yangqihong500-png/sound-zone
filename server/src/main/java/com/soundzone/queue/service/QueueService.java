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
import com.soundzone.zone.entity.FilterMode;
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
 * 队列服务（docs/02 第 3 步，v2：2026-09-24 会议）
 *
 * 播放顺序：按上传顺序 FIFO（v1 得分公式已废除）
 * 上传准入：① 10 分钟冷却 → ② 域级标签过滤（BAN/ALLOW 双模式）→ ③ 番茄钟时段白名单（不符入预存）
 * 点赞：仅互动信号，不改变播放顺序
 */
@Service
@RequiredArgsConstructor
public class QueueService {

    /** 上传冷却时长（决议 D4）：单用户单域 10 分钟 1 首 */
    private static final int COOLDOWN_MINUTES = 10;

    private final QueueItemRepository queueItemRepository;
    private final ZoneRepository zoneRepository;
    private final ZonePeriodRepository periodRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    /** 上传歌曲：冷却 → 过滤 → 时段白名单 → 入队尾（FIFO） */
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

        // ① 上传冷却（决议 D4）：10 分钟内已上传 → 拒绝并告知剩余秒数
        long cooldownRemain = cooldownRemainSeconds(zoneId, requester.getId());
        if (cooldownRemain > 0) {
            throw new BizException(ResultCode.UPLOAD_COOLDOWN,
                    "冷却剩余 " + cooldownRemain + " 秒");
        }

        // ② 域级标签过滤（决议 D5 双模式）
        checkZoneFilter(zone, track);

        QueueItem item = new QueueItem();
        item.setZone(zone);
        item.setTrack(track);
        item.setRequester(requester);

        // ③ 番茄钟时段白名单（与域级过滤叠加）：不符合当前时段 → 预存队列
        if (!allowedInCurrentPeriod(zone, track)) {
            item.setStatus(QueueStatus.PRESET);
        }
        // ④ 入队尾：FIFO 由 createdAt 升序保证，无需任何得分字段
        item = queueItemRepository.save(item);
        return toDTO(item, null);
    }

    /** 点赞：+1 赞（仅互动信号，不影响 FIFO 播放顺序） */
    @Transactional
    public QueueItemDTO like(Long itemId, Long userId) {
        QueueItem item = queueItemRepository.findById(itemId)
                .orElseThrow(() -> new BizException(ResultCode.QUEUE_ITEM_NOT_FOUND));
        item.setLikes(item.getLikes() + 1);
        queueItemRepository.save(item);
        return toDTO(item, null);
    }

    /** 冷却剩余秒数（0 = 可上传）；前端据此渲染按钮置灰与倒计时（决议 D4） */
    public long cooldownRemainSeconds(Long zoneId, Long userId) {
        return queueItemRepository
                .findFirstByZoneIdAndRequesterIdOrderByCreatedAtDesc(zoneId, userId)
                .map(last -> {
                    long elapsed = Duration.between(last.getCreatedAt(), LocalDateTime.now()).getSeconds();
                    return Math.max(0, COOLDOWN_MINUTES * 60L - elapsed);
                })
                .orElse(0L);
    }

    /**
     * 域级标签过滤（决议 D5）：
     * BAN 模式：歌曲标签命中 filterTags → 拒绝（3002）
     * ALLOW 模式：歌曲标签与 filterTags 无交集 → 拒绝（3009）
     * filterTags 为空时两种模式均放行
     */
    private void checkZoneFilter(Zone zone, Track track) {
        Set<String> filterTags = zone.getFilterTags();
        if (filterTags == null || filterTags.isEmpty()) return;
        boolean hit = track.getTags().stream().anyMatch(filterTags::contains);
        if (zone.getFilterMode() == FilterMode.BAN && hit) {
            throw new BizException(ResultCode.SONG_BANNED_BY_ZONE,
                    "曲目「" + track.getTitle() + "」标签 " + track.getTags() + " 被本域禁止");
        }
        if (zone.getFilterMode() == FilterMode.ALLOW && !hit) {
            throw new BizException(ResultCode.SONG_FILTERED_BY_ZONE,
                    "曲目「" + track.getTitle() + "」不在本域允许的标签 " + filterTags + " 范围内");
        }
    }

    /**
     * 判断曲目是否符合"当前时段"白名单（番茄钟，docs/02）
     * 无时段配置 / 当前时段白名单为空 → 放行
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
