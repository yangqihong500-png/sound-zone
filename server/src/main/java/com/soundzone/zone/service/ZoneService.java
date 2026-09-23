package com.soundzone.zone.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.queue.entity.QueueItem;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.dto.*;
import com.soundzone.zone.entity.*;
import com.soundzone.zone.repository.ZonePeriodRepository;
import com.soundzone.zone.repository.ZoneRepository;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.moment.dto.MomentDTO;
import com.soundzone.queue.dto.QueueItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 域服务：创建域 / 活跃域列表 / 域详情 / 结束域
 * 对应 docs/02 第 1、2、5 步
 */
@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final ZonePeriodRepository periodRepository;
    private final QueueItemRepository queueItemRepository;
    private final MomentRepository momentRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;

    /**
     * 场景名归一化映射（docs/02 第 1 步：避免流量过度分散）
     * 【假设】Demo 用静态映射表；正式版为文本聚类模型（docs/03 场景归一化）
     */
    private static final Map<String, String> SCENE_ALIASES = Map.of(
            "自习室", "自习", "图书馆刷题", "自习", "考研", "自习",
            "夜跑", "健身", "健身房", "健身", "铁馆", "健身",
            "solo trip", "旅行", "旅游", "旅行",
            "拼豆", "手工", "手作", "手工",
            "写代码", "工作", "加班", "工作",
            "深夜", "深夜", "睡前", "深夜"
    );

    /** 首页/发现页：只推活跃域，按同频人数排序（docs/02 第 2 步） */
    public List<ZoneSummaryDTO> listActive(String scene) {
        List<Zone> zones = (scene == null || scene.isBlank() || "全部".equals(scene))
                ? zoneRepository.findByStatusOrderByListenerCountDesc(ZoneStatus.ACTIVE)
                : zoneRepository.findByStatusAndSceneOrderByListenerCountDesc(ZoneStatus.ACTIVE, normalizeScene(scene));
        return zones.stream().map(this::toSummary).toList();
    }

    /** 创建域：≥3 首歌 + 场景命名 + 可选黑名单/番茄钟（docs/02 第 1 步） */
    @Transactional
    public ZoneDetailDTO createZone(ZoneCreateRequest req) {
        // 建域门槛（注解 @Size(min=3) 已兜底，这里再校验一次给出业务错误码）
        if (req.trackIds() == null || req.trackIds().size() < 3) {
            throw new BizException(ResultCode.ZONE_CREATE_TRACKS_NOT_ENOUGH);
        }
        User host = userRepository.findById(req.hostId())
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        List<Track> tracks = trackRepository.findAllById(req.trackIds());
        if (tracks.size() < 3) {
            throw new BizException(ResultCode.TRACK_NOT_FOUND, "初始歌单中存在无效曲目");
        }

        Zone zone = new Zone();
        zone.setName(req.name().trim());
        zone.setScene(normalizeScene(req.scene()));
        zone.setHost(host);
        if (req.coverColor() != null) zone.setCoverColor(req.coverColor());
        if (req.tags() != null) zone.setTags(req.tags());
        if (req.bannedTags() != null) zone.setBannedTags(req.bannedTags());
        zone = zoneRepository.save(zone);

        // 番茄钟时段配置（docs/02 决议 D3）
        if (req.periods() != null) {
            Zone finalZone = zone;
            List<ZonePeriod> periods = req.periods().stream().map(p -> {
                ZonePeriod period = new ZonePeriod();
                period.setZone(finalZone);
                period.setOrderIndex(p.orderIndex());
                period.setDurationMin(p.durationMin());
                period.setType(PeriodType.valueOf(p.type().toUpperCase()));
                if (p.allowedTags() != null) period.setAllowedTags(p.allowedTags());
                return period;
            }).toList();
            periodRepository.saveAll(periods);
        }

        // 初始歌单进队列：第 1 首直接播放，其余按创建顺序入活跃队列
        for (int i = 0; i < tracks.size(); i++) {
            QueueItem item = new QueueItem();
            item.setZone(zone);
            item.setTrack(tracks.get(i));
            item.setRequester(host);
            item.setHostBonus(1); // 域主加成（docs/02 队列公式）
            item.setScore(3.0);   // 初始分 = 0×2 + 1×3 − 0
            if (i == 0) {
                item.setStatus(QueueStatus.PLAYING);
                item.setStartedAt(LocalDateTime.now());
            }
            queueItemRepository.save(item);
        }
        return getDetail(zone.getId());
    }

    /** 域详情：三件套组装（播放中 + 队列 + 碎片墙） */
    public ZoneDetailDTO getDetail(Long zoneId) {
        Zone zone = getZone(zoneId);
        NowPlayingDTO nowPlaying = currentPlaying(zoneId);

        // 活跃队列按得分排序，rank 按位次生成
        AtomicInteger rank = new AtomicInteger(1);
        List<QueueItemDTO> queue = queueItemRepository
                .findByZoneIdAndStatusOrderByScoreDesc(zoneId, QueueStatus.QUEUED)
                .stream()
                .map(q -> new QueueItemDTO(q.getId(), rank.getAndIncrement(),
                        q.getTrack().getTitle(), q.getTrack().getArtist(),
                        q.getLikes(), q.getRequester().getName(), q.getStatus().name()))
                .toList();

        List<MomentDTO> moments = momentRepository.findByZoneIdOrderByCreatedAtDesc(zoneId)
                .stream().map(MomentDTO::from).toList();

        return new ZoneDetailDTO(zone.getId(), zone.getName(), zone.getScene(),
                zone.getListenerCount(), zone.getHost().getName(), zone.getCoverColor(),
                zone.getTags(), zone.getBannedTags(), nowPlaying, queue, moments);
    }

    /** 结束域：状态置为 ENDED，剩余队列归档（战报由 FeedbackService 聚合） */
    @Transactional
    public void endZone(Long zoneId) {
        Zone zone = getZone(zoneId);
        zone.setStatus(ZoneStatus.ENDED);
        zone.setEndedAt(LocalDateTime.now());
        zoneRepository.save(zone);
        queueItemRepository.findFirstByZoneIdAndStatus(zoneId, QueueStatus.PLAYING)
                .ifPresent(q -> {
                    q.setStatus(QueueStatus.PLAYED);
                    q.setPlayedAt(LocalDateTime.now());
                    queueItemRepository.save(q);
                });
    }

    public Zone getZone(Long zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new BizException(ResultCode.ZONE_NOT_FOUND));
    }

    /** 当前播放信息：进度 = 已播时长 / 曲目时长（服务端权威时钟的 Demo 简化版） */
    public NowPlayingDTO currentPlaying(Long zoneId) {
        return queueItemRepository.findFirstByZoneIdAndStatus(zoneId, QueueStatus.PLAYING)
                .map(q -> {
                    int progress = 0;
                    if (q.getStartedAt() != null) {
                        long elapsed = Duration.between(q.getStartedAt(), LocalDateTime.now()).getSeconds();
                        progress = (int) Math.min(99, elapsed * 100 / Math.max(1, q.getTrack().getDurationSec()));
                    }
                    return new NowPlayingDTO(q.getTrack().getId(), q.getTrack().getTitle(),
                            q.getTrack().getArtist(), q.getRequester().getName(), progress);
                })
                .orElse(null);
    }

    /** 场景名归一化：命中别名表则映射，否则原样保留 */
    public String normalizeScene(String raw) {
        if (raw == null) return "其他";
        String trimmed = raw.trim();
        return SCENE_ALIASES.getOrDefault(trimmed, trimmed);
    }

    private ZoneSummaryDTO toSummary(Zone z) {
        return new ZoneSummaryDTO(z.getId(), z.getName(), z.getScene(),
                z.getListenerCount(), z.getHost().getName(), z.getCoverColor(),
                z.getTags(), currentPlaying(z.getId()));
    }
}
