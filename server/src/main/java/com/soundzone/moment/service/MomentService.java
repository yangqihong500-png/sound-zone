package com.soundzone.moment.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.moment.dto.MomentCreateRequest;
import com.soundzone.moment.dto.MomentDTO;
import com.soundzone.moment.entity.Moment;
import com.soundzone.moment.entity.MomentStatus;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.repository.TrackRepository;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.entity.ZoneStatus;
import com.soundzone.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片分享服务（docs/02 第 4 步，v2：2026-09-24 会议）
 * 绑定规则：优先绑用户指定的（本人上传的）歌曲，回退绑当前播放歌曲
 * 每条分享 = (场景, 图片, 关联歌曲) 三元组（docs/03 数据飞轮源头）
 * 支持撤回（仅本人）与半小时图片流
 */
@Service
@RequiredArgsConstructor
public class MomentService {

    /** 动态详情页时间窗口（决议 D6：半小时内图片流） */
    private static final int FEED_WINDOW_MINUTES = 30;

    private final MomentRepository momentRepository;
    private final ZoneRepository zoneRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final QueueItemRepository queueItemRepository;

    /** 发布图片分享：绑定关联歌曲（决议 D6） */
    @Transactional
    public MomentDTO create(Long zoneId, MomentCreateRequest req) {
        var zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new BizException(ResultCode.ZONE_NOT_FOUND));
        if (zone.getStatus() == ZoneStatus.ENDED) {
            throw new BizException(ResultCode.ZONE_ALREADY_ENDED);
        }
        var user = userRepository.findById(req.userId())
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        if ((req.text() == null || req.text().isBlank()) && (req.imageUrl() == null || req.imageUrl().isBlank()) && (req.color() == null || req.color().isBlank())) {
            throw new BizException(ResultCode.PARAM_INVALID, "图片与文案至少填一项");
        }

        Moment moment = new Moment();
        moment.setZone(zone);
        moment.setUser(user);
        moment.setText(req.text());
        moment.setImageUrl(req.imageUrl());
        moment.setColor(req.color());
        moment.setTrack(resolveBindTrack(zoneId, req));
        return MomentDTO.from(momentRepository.save(moment));
    }

    /**
     * 关联歌曲解析（v2 绑定规则）：
     * ① 指定 trackId → 校验该曲确为本人在本域上传过（防乱绑）
     * ② 未指定 → 回退当前播放歌曲（可为空）
     */
    private com.soundzone.track.entity.Track resolveBindTrack(Long zoneId, MomentCreateRequest req) {
        if (req.trackId() != null) {
            var track = trackRepository.findById(req.trackId())
                    .orElseThrow(() -> new BizException(ResultCode.TRACK_NOT_FOUND));
            // 校验：本人在本域有上传记录才可绑定（防乱绑他人歌曲）
            boolean anyUpload = queueItemRepository.countByZoneIdAndRequesterId(zoneId, req.userId()) > 0;
            if (!anyUpload) {
                throw new BizException(ResultCode.PARAM_INVALID, "仅可绑定本人在本域上传的歌曲");
            }
            return track;
        }
        return queueItemRepository.findFirstByZoneIdAndStatus(zoneId, QueueStatus.PLAYING)
                .map(q -> q.getTrack())
                .orElse(null);
    }

    /** 动态详情页：半小时内图片流（时间倒序，决议 D6） */
    public List<MomentDTO> feed(Long zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new BizException(ResultCode.ZONE_NOT_FOUND);
        }
        return momentRepository
                .findByZoneIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        zoneId, MomentStatus.NORMAL, LocalDateTime.now().minusMinutes(FEED_WINDOW_MINUTES))
                .stream().map(MomentDTO::from).toList();
    }

    /** 撤回（决议 D6）：仅本人可撤回，软删除（状态置 WITHDRAWN，训练数据同步剔除见 docs/03） */
    @Transactional
    public void withdraw(Long momentId, Long userId) {
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new BizException(ResultCode.MOMENT_NOT_FOUND));
        if (!moment.getUser().getId().equals(userId)) {
            throw new BizException(ResultCode.NOT_RESOURCE_OWNER, "仅本人可撤回自己的图片");
        }
        moment.setStatus(MomentStatus.WITHDRAWN);
        momentRepository.save(moment);
    }
}
