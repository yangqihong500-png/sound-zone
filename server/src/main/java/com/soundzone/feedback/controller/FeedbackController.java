package com.soundzone.feedback.controller;

import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.*;
import com.soundzone.feedback.service.FeedbackService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedback;
    private final CurrentUser current;

    @PutMapping("/zones/{zoneId}/collection")
    public Result<?> collect(@PathVariable Long zoneId, @RequestBody CollectionRequest req) {
        if (req.itemId() == null) throw new BizException(ResultCode.PARAM_INVALID);
        return Result.ok(feedback.collect(zoneId, req.itemId(), current.id(), req.active()));
    }

    @PostMapping("/zones/{zoneId}/reports")
    public Result<?> report(@PathVariable Long zoneId, @RequestBody ReportRequest req) {
        return Result.ok(feedback.report(zoneId, current.id(), req.reason()));
    }

    @DeleteMapping("/users/me/collections/{trackId}")
    public Result<?> remove(@PathVariable Long trackId) {
        feedback.removeCollection(trackId, current.id());
        return Result.ok();
    }

    public record CollectionRequest(Long itemId, boolean active) {}

    public record ReportRequest(String reason) {}
    // 旧战报聚合不再对外开放，历史事件及 DTO 保留，不扩建战报。
}
