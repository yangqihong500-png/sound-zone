package com.soundzone.track.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.track.dto.TrackDTO;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;

/** 曲目服务：点歌搜曲 + 曲目查询 曲风打标（docs/04 风险项）当前为人工/种子数据预置， 自动打标模型上线后在曲目入库管线中调用（docs/03 算法模块） */
@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackRepository trackRepository;
    private final AudiusClient audius;
    private final TrackCatalogImporter importer;
    private final TrackDurationPolicy durations;

    public Track getById(Long id) {
        return trackRepository
                .findById(id)
                .orElseThrow(() -> new BizException(ResultCode.TRACK_NOT_FOUND));
    }

    /** 点歌搜曲：歌名或歌手模糊匹配 */
    public List<TrackDTO> search(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.length() > 100)
            throw new BizException(ResultCode.PARAM_INVALID, "搜索词不能超过 100 个字符");
        List<Track> local =
                normalized.isBlank()
                        ? trackRepository.findTop50ByOrderByIdAsc()
                        : trackRepository
                                .findTop20ByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(
                                        normalized, normalized);
        List<Track> remote = importer.importAudius(audius.search(normalized));
        LinkedHashMap<Long, TrackDTO> merged = new LinkedHashMap<>();
        remote.stream()
                .filter(durations::isAllowed)
                .forEach(t -> merged.put(t.getId(), TrackDTO.from(t)));
        local.stream()
                .filter(durations::isAllowed)
                .forEach(t -> merged.putIfAbsent(t.getId(), TrackDTO.from(t)));
        return merged.values().stream().limit(50).toList();
    }
}
