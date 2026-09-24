package com.soundzone.feedback.controller;

import com.soundzone.common.Result;
import com.soundzone.feedback.dto.HeartRequest;
import com.soundzone.feedback.dto.ZoneReportDTO;
import com.soundzone.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 反馈控制器（v2：2026-09-24 决议 D7：收藏/点赞/emoji + 域后战报保留）
 */
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /** 红心/收藏当前播放歌曲 → 归属点歌人（实时特效数据源） */
    @PostMapping("/zones/{zoneId}/heart")
    public Result<Void> heart(@PathVariable Long zoneId,
                              @Valid @RequestBody HeartRequest req) {
        feedbackService.heart(zoneId, req);
        return Result.ok();
    }

    /** 域后个人战报 */
    @GetMapping("/zones/{zoneId}/report")
    public Result<ZoneReportDTO> report(@PathVariable Long zoneId) {
        return Result.ok(feedbackService.report(zoneId));
    }
}
