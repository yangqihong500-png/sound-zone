package com.soundzone.moment.service;

import com.soundzone.activity.service.ActivityService;
import com.soundzone.common.*;
import com.soundzone.moment.dto.*;
import com.soundzone.moment.entity.*;
import com.soundzone.moment.repository.*;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.queue.service.PlaybackService;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.service.ZoneAccess;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MomentService {
    private final MomentRepository moments;
    private final MomentReactionRepository reactions;
    private final QueueItemRepository queue;
    private final ZoneAccess access;
    private final UserRepository users;
    private final ImageStorage storage;
    private final PlaybackService playback;
    private final ActivityService activity;
    private final Clock clock;

    public boolean hasImage(Long itemId) {
        return moments.existsByQueueItemId(itemId);
    }

    public MomentDTO create(Long zoneId, Long userId, MomentCreateRequest req, MultipartFile file) {
        var zone = access.lock(zoneId);
        access.member(zoneId, userId);
        var item =
                queue.findById(req.queueItemId())
                        .orElseThrow(() -> new BizException(ResultCode.QUEUE_ITEM_NOT_FOUND));
        if (!item.getZone().getId().equals(zoneId) || !item.getRequester().getId().equals(userId))
            throw new BizException(ResultCode.NOT_RESOURCE_OWNER, "只能为自己在本域上传的歌曲附图");
        if (item.getCreatedAt().isBefore(LocalDateTime.now(clock).minusMinutes(10)))
            throw new BizException(ResultCode.PARAM_INVALID, "本次上传的附图时间已结束");
        if (hasImage(item.getId())) throw new BizException(ResultCode.PARAM_INVALID, "这次上传已经附过图片");
        String key = storage.store(file);
        Moment m = new Moment();
        m.setZone(zone);
        m.setUser(users.getReferenceById(userId));
        m.setQueueItem(item);
        m.setTrack(item.getTrack());
        m.setText(req.text());
        m.setImageUrl(key);
        m.setCreatedAt(LocalDateTime.now(clock));
        m.setTrainingConsent(req.trainingConsent());
        if (req.trainingConsent()) m.setConsentedAt(m.getCreatedAt());
        // Demo 即时发布；保留历史状态字段，供旧数据与运营下架记录兼容。
        m.setModerationStatus(ModerationStatus.APPROVED);
        moments.saveAndFlush(m);
        zone.setLastActivityAt(m.getCreatedAt());
        playback.changed(zone);
        activity.record(userId, zoneId, item.getId(), "IMAGE", 0);
        return MomentDTO.from(m, null);
    }

    public List<MomentDTO> feed(Long zoneId, Long userId) {
        var zone = access.member(zoneId, userId);
        var source =
                zone.isDemoResident()
                        ? moments.findByZoneIdAndStatusOrderByCreatedAtDescIdDesc(
                                zoneId, MomentStatus.NORMAL)
                        : moments.findByZoneIdAndStatusAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                                zoneId,
                                MomentStatus.NORMAL,
                                LocalDateTime.now(clock).minusMinutes(30));
        return source
                .stream()
                .filter(m -> m.getModerationStatus() != ModerationStatus.REJECTED)
                .map(
                        m ->
                                MomentDTO.from(
                                        m,
                                        reactions
                                                .findByUserIdAndMomentId(userId, m.getId())
                                                .map(MomentReaction::getType)
                                                .orElse(null)))
                .toList();
    }

    public void withdraw(Long zoneId, Long id, Long userId) {
        var zone = access.lock(zoneId);
        var m = get(id);
        if (!m.getZone().getId().equals(zoneId)) throw new BizException(ResultCode.FORBIDDEN);
        if (!m.getUser().getId().equals(userId))
            throw new BizException(ResultCode.NOT_RESOURCE_OWNER);
        m.setStatus(MomentStatus.WITHDRAWN);
        m.setWithdrawnAt(LocalDateTime.now(clock));
        m.setTrainingConsent(false);
        playback.changed(zone); // 训练候选查询实时排除此记录；不物理删除审计记录
    }

    public String react(Long zoneId, Long id, Long userId, String type) {
        var zone = access.lock(zoneId);
        var m = get(id);
        if (!m.getZone().getId().equals(zoneId)) throw new BizException(ResultCode.FORBIDDEN);
        access.member(zone.getId(), userId);
        if (m.getStatus() != MomentStatus.NORMAL
                || m.getModerationStatus() == ModerationStatus.REJECTED)
            throw new BizException(ResultCode.MOMENT_NOT_FOUND);
        if (type != null && !Set.of("HEART", "LAUGH", "LIKE").contains(type))
            throw new BizException(ResultCode.PARAM_INVALID, "不支持的表情");
        var old = reactions.findByUserIdAndMomentId(userId, id);
        if (type == null) old.ifPresent(reactions::delete);
        else if (old.isEmpty() || !old.get().getType().equals(type)) {
            MomentReaction reaction = old.orElseGet(MomentReaction::new);
            reaction.setMoment(m);
            reaction.setUser(users.getReferenceById(userId));
            reaction.setType(type);
            reactions.save(reaction);
            activity.record(
                    userId,
                    zone.getId(),
                    m.getQueueItem() == null ? null : m.getQueueItem().getId(),
                    "REACTION",
                    0);
        }
        playback.changed(zone);
        return type;
    }

    public Path image(Long id, Long userId) {
        Moment m = get(id);
        if (m.getStatus() != MomentStatus.NORMAL)
            throw new BizException(ResultCode.MOMENT_NOT_FOUND);
        if (!m.getUser().getId().equals(userId)) {
            access.member(m.getZone().getId(), userId);
            if (m.getModerationStatus() == ModerationStatus.REJECTED)
                throw new BizException(ResultCode.FORBIDDEN);
        }
        return storage.resolve(m.getImageUrl());
    }

    public List<MomentDTO> mine(Long userId) {
        return moments.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(m -> m.getStatus() == MomentStatus.NORMAL)
                .map(m -> MomentDTO.from(m, null))
                .toList();
    }

    private Moment get(Long id) {
        return moments.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.MOMENT_NOT_FOUND));
    }
}
