package com.soundzone.moment.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.moment.dto.MomentCreateRequest;
import com.soundzone.moment.dto.MomentDTO;
import com.soundzone.moment.entity.Moment;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.user.repository.UserRepository;
import com.soundzone.zone.entity.ZoneStatus;
import com.soundzone.zone.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 碎片服务（docs/02 第 4 步）
 * 关键设计：发布时自动绑定域内当前播放歌曲 —— 用户无需选择配乐，
 * 每条碎片天然构成 (场景, 图/文, 配乐) 三元组（docs/03 数据飞轮源头）
 */
@Service
@RequiredArgsConstructor
public class MomentService {

    private final MomentRepository momentRepository;
    private final ZoneRepository zoneRepository;
    private final UserRepository userRepository;
    private final QueueItemRepository queueItemRepository;

    /** 发布碎片：自动挂当前播放曲目 */
    @Transactional
    public MomentDTO create(Long zoneId, MomentCreateRequest req) {
        var zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new BizException(ResultCode.ZONE_NOT_FOUND));
        if (zone.getStatus() == ZoneStatus.ENDED) {
            throw new BizException(ResultCode.ZONE_ALREADY_ENDED);
        }
        var user = userRepository.findById(req.userId())
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        if ((req.text() == null || req.text().isBlank()) && (req.imageUrl() == null || req.imageUrl().isBlank())) {
            throw new BizException(ResultCode.PARAM_INVALID, "文案与图片至少填一项");
        }

        Moment moment = new Moment();
        moment.setZone(zone);
        moment.setUser(user);
        moment.setText(req.text());
        moment.setImageUrl(req.imageUrl());
        moment.setColor(req.color());

        // 自动绑定当前播放（docs/02 第 4 步）；无播放中曲目时可为空
        queueItemRepository.findFirstByZoneIdAndStatus(zoneId, QueueStatus.PLAYING)
                .ifPresent(q -> moment.setTrack(q.getTrack()));

        return MomentDTO.from(momentRepository.save(moment));
    }

    /** 碎片墙列表（发布时间倒序） */
    public List<MomentDTO> listByZone(Long zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new BizException(ResultCode.ZONE_NOT_FOUND);
        }
        return momentRepository.findByZoneIdOrderByCreatedAtDesc(zoneId)
                .stream().map(MomentDTO::from).toList();
    }
}
