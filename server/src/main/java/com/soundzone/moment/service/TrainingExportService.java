package com.soundzone.moment.service;

import com.soundzone.common.*;
import com.soundzone.moment.entity.*;
import com.soundzone.moment.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainingExportService {
    private final MomentRepository moments;
    private final TrainingExportItemRepository exports;
    private final ImageStorage storage;

    /** 显式运营操作才生成清单；不含用户标识和图片文案，不自动训练。 */
    public Map<String, Object> export() {
        String batch = UUID.randomUUID().toString();
        var eligible =
                moments
                        .findByTrainingConsentTrueAndStatusAndModerationStatus(
                                MomentStatus.NORMAL, ModerationStatus.APPROVED)
                        .stream()
                        .filter(m -> m.getQueueItem() != null && m.getImageUrl() != null)
                        .toList();
        for (var moment : eligible) {
            var e = new TrainingExportItem();
            e.setBatchId(batch);
            e.setMoment(moment);
            exports.save(e);
        }
        var items =
                eligible.stream()
                        .map(
                                m ->
                                        Map.<String, Object>of(
                                                "momentId",
                                                m.getId(),
                                                "scene",
                                                m.getZone().getScene(),
                                                "trackId",
                                                m.getTrack().getId(),
                                                "tags",
                                                m.getTrack().getTags(),
                                                "imagePath",
                                                "/internal/training/images/" + m.getId()))
                        .toList();
        return Map.of("batchId", batch, "items", items);
    }

    public List<Long> revocations(String batch) {
        return exports.findByBatchId(batch).stream()
                .map(TrainingExportItem::getMoment)
                .filter(m -> !eligible(m))
                .map(Moment::getId)
                .toList();
    }

    public Path image(Long id) {
        var m =
                moments.findById(id)
                        .orElseThrow(() -> new BizException(ResultCode.MOMENT_NOT_FOUND));
        if (!eligible(m)) throw new BizException(ResultCode.FORBIDDEN, "图片已撤回或未授权训练");
        return storage.resolve(m.getImageUrl());
    }

    private boolean eligible(Moment m) {
        return m.isTrainingConsent()
                && m.getStatus() == MomentStatus.NORMAL
                && m.getModerationStatus() == ModerationStatus.APPROVED;
    }
}
