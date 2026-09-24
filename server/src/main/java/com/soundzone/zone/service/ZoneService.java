package com.soundzone.zone.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.moment.dto.MomentDTO;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.dto.QueueItemDTO;
import com.soundzone.queue.entity.QueueItem;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.dto.*;
import com.soundzone.zone.entity.*;
import com.soundzone.zone.repository.ZoneMemberRepository;
import com.soundzone.zone.repository.ZonePeriodRepository;
import com.soundzone.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 域服务（v2：2026-09-24 会议）
 * 创建域（公开/私密 + 过滤双模式）/ 活跃公开域列表 / 进入（私密鉴权）/ 退出（全员退出自动消失）/ 域详情
 */
@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final ZonePeriodRepository periodRepository;
    private final ZoneMemberRepository memberRepository;
    private final QueueItemRepository queueItemRepository;
    private final MomentRepository momentRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;

    /**
     * 场景名归一化映射（docs/02 第 1 步）
     * 【假设】Demo 用静态映射表；正式版为文本聚类模型（docs/03 场景归一化）
     */
        private static final Map<String, String> SCENE_ALIASES = Map.ofEntries(
            Map.entry("自习室", "自习"), Map.entry("图书馆刷题", "自习"), Map.entry("考研", "自习"),
            Map.entry("夜跑", "健身"), Map.entry("健身房", "健身"), Map.entry("铁馆", "健身"),
            Map.entry("solo trip", "旅行"), Map.entry("旅游", "旅行"),
            Map.entry("拼豆", "手工"), Map.entry("手作", "手工"),
            Map.entry("写代码", "工作"), Map.entry("加班", "工作"),
            Map.entry("深夜", "深夜"), Map.entry("睡前", "深夜")
        );

    /** 首页/发现页：只推活跃的公开域（决议 D2：私密域不参与分发），按同频人数排序 */
    public List<ZoneSummaryDTO> listActive(String scene) {
        List<Zone> zones = (scene == null || scene.isBlank() || "全部".equals(scene))
                ? zoneRepository.findByStatusAndVisibilityOrderByListenerCountDesc(ZoneStatus.ACTIVE, ZoneVisibility.PUBLIC)
                : zoneRepository.findByStatusAndVisibilityAndSceneOrderByListenerCountDesc(ZoneStatus.ACTIVE, ZoneVisibility.PUBLIC, normalizeScene(scene));
        return zones.stream().map(this::toSummary).toList();
    }

    /** 创建域：≥3 首歌 + 场景命名 + 可见性 + 过滤双模式 + 可选番茄钟 */
    @Transactional
    public ZoneDetailDTO createZone(ZoneCreateRequest req) {
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
        // @ElementCollection 字段须写入可变集合（request record 传入的是不可变集合）
        if (req.tags() != null) zone.getTags().addAll(req.tags());
        if (req.filterTags() != null) zone.getFilterTags().addAll(req.filterTags());
        if (req.filterMode() != null) zone.setFilterMode(FilterMode.valueOf(req.filterMode().toUpperCase()));

        // 可见性（决议 D2）：私密域需要密码或自动生成邀请码
        if (req.visibility() != null && "PRIVATE".equalsIgnoreCase(req.visibility())) {
            zone.setVisibility(ZoneVisibility.PRIVATE);
            if (req.password() != null && !req.password().isBlank()) {
                zone.setPassword(req.password());
            }
            // 无论是否设密码都生成邀请码，保证邀请链接可用
            zone.setInviteCode(UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        }
        zone = zoneRepository.save(zone);

        // 域主自动成为成员（同频人数从 1 开始）
        joinInternal(zone, host);

        // 番茄钟时段配置（2026-09-23 决议 D3）
        if (req.periods() != null) {
            Zone finalZone = zone;
            List<ZonePeriod> periods = req.periods().stream().map(p -> {
                ZonePeriod period = new ZonePeriod();
                period.setZone(finalZone);
                period.setOrderIndex(p.orderIndex());
                period.setDurationMin(p.durationMin());
                period.setType(PeriodType.valueOf(p.type().toUpperCase()));
                if (p.allowedTags() != null) period.getAllowedTags().addAll(p.allowedTags());
                return period;
            }).toList();
            periodRepository.saveAll(periods);
        }

        // 初始歌单进队列：第 1 首直接播放，其余按上传顺序 FIFO 等待
        for (int i = 0; i < tracks.size(); i++) {
            QueueItem item = new QueueItem();
            item.setZone(zone);
            item.setTrack(tracks.get(i));
            item.setRequester(host);
            if (i == 0) {
                item.setStatus(QueueStatus.PLAYING);
                item.setStartedAt(LocalDateTime.now());
            }
            queueItemRepository.save(item);
        }
        return getDetail(zone.getId());
    }

    /**
     * 进入域（决议 D2）：
     * 公开域直接进入；私密域校验密码或邀请码；
     * 加入即成为成员，同频人数 +1
     */
    @Transactional
    public ZoneDetailDTO joinZone(Long zoneId, JoinZoneRequest req) {
        Zone zone = getZone(zoneId);
        if (zone.getStatus() == ZoneStatus.ENDED) {
            throw new BizException(ResultCode.ZONE_ALREADY_ENDED);
        }
        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        if (zone.getVisibility() == ZoneVisibility.PRIVATE) {
            boolean codeOk = req.inviteCode() != null && req.inviteCode().equals(zone.getInviteCode());
            boolean pwdOk = zone.getPassword() != null && zone.getPassword().equals(req.password());
            if (!codeOk && !pwdOk) {
                boolean needAuth = req.password() == null && req.inviteCode() == null;
                throw new BizException(needAuth ? ResultCode.ZONE_PRIVATE_NEED_AUTH : ResultCode.ZONE_PASSWORD_WRONG);
            }
        }

        // 幂等：已在域内直接返回详情
        if (memberRepository.findByZoneIdAndUserId(zoneId, user.getId()).isEmpty()) {
            joinInternal(zone, user);
        }
        return getDetail(zoneId);
    }

    /**
     * 退出域（决议 D2）：移除成员，同频人数 -1；
     * **全员退出后域自动消失**（状态置 ENDED 并归档当前播放）
     */
    @Transactional
    public void leaveZone(Long zoneId, Long userId) {
        Zone zone = getZone(zoneId);
        memberRepository.deleteByZoneIdAndUserId(zoneId, userId);
        long remaining = memberRepository.countByZoneId(zoneId);
        zone.setListenerCount((int) remaining);
        zoneRepository.save(zone);
        if (remaining == 0) {
            endZone(zoneId);
        }
    }

    /** 域详情：当前播放 + FIFO 队列 + 动态区 */
    public ZoneDetailDTO getDetail(Long zoneId) {
        Zone zone = getZone(zoneId);
        NowPlayingDTO nowPlaying = currentPlaying(zoneId);

        // FIFO：按上传时间升序，rank 即等待位次（决议 D3）
        AtomicInteger rank = new AtomicInteger(1);
        List<QueueItemDTO> queue = queueItemRepository
                .findByZoneIdAndStatusOrderByCreatedAtAsc(zoneId, QueueStatus.QUEUED)
                .stream()
                .map(q -> new QueueItemDTO(q.getId(), rank.getAndIncrement(),
                        q.getTrack().getTitle(), q.getTrack().getArtist(),
                        q.getLikes(), q.getRequester().getName(), q.getStatus().name()))
                .toList();

        List<MomentDTO> moments = momentRepository
                .findTop2ByZoneIdAndStatusOrderByCreatedAtDesc(zoneId, com.soundzone.moment.entity.MomentStatus.NORMAL)
                .stream().map(MomentDTO::from).toList();

        return new ZoneDetailDTO(zone.getId(), zone.getName(), zone.getScene(),
                zone.getListenerCount(), zone.getHost().getName(), zone.getCoverColor(),
                zone.getVisibility().name(), zone.getInviteCode(),
                zone.getTags(), zone.getFilterMode().name(), zone.getFilterTags(),
                nowPlaying, queue, moments);
    }

    /** 结束域：状态置为 ENDED，当前播放归档（战报由 FeedbackService 聚合，保留） */
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

    /** 成员加入内部方法：写成员关系并维护同频人数 */
    private void joinInternal(Zone zone, User user) {
        ZoneMember member = new ZoneMember();
        member.setZone(zone);
        member.setUser(user);
        memberRepository.save(member);
        zone.setListenerCount((int) memberRepository.countByZoneId(zone.getId()));
        zoneRepository.save(zone);
    }

    private ZoneSummaryDTO toSummary(Zone z) {
        return new ZoneSummaryDTO(z.getId(), z.getName(), z.getScene(),
                z.getListenerCount(), z.getHost().getName(), z.getCoverColor(),
                z.getVisibility().name(), z.getTags(), currentPlaying(z.getId()));
    }
}
