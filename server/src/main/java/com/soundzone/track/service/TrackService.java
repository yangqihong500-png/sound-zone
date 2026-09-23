package com.soundzone.track.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.track.dto.TrackDTO;
import com.soundzone.track.entity.Track;
import com.soundzone.track.repository.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 曲目服务：点歌搜曲 + 曲目查询
 * 曲风打标（docs/04 风险项）当前为人工/种子数据预置，
 * 自动打标模型上线后在曲目入库管线中调用（docs/03 算法模块）
 */
@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackRepository trackRepository;

    public Track getById(Long id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.TRACK_NOT_FOUND));
    }

    /** 点歌搜曲：歌名或歌手模糊匹配 */
    public List<TrackDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return trackRepository
                .findTop20ByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(keyword, keyword)
                .stream().map(TrackDTO::from).toList();
    }
}
