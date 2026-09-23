package com.soundzone.track.controller;

import com.soundzone.common.Result;
import com.soundzone.track.dto.TrackDTO;
import com.soundzone.track.service.TrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 曲目控制器（点歌搜曲）
 */
@RestController
@RequestMapping("/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    /** 搜曲：歌名/歌手模糊匹配，返回带曲风标签的候选列表 */
    @GetMapping("/search")
    public Result<List<TrackDTO>> search(@RequestParam String keyword) {
        return Result.ok(trackService.search(keyword));
    }
}
