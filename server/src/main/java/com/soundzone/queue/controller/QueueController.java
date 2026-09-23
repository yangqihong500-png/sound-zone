package com.soundzone.queue.controller;

import com.soundzone.common.Result;
import com.soundzone.queue.dto.QueueItemDTO;
import com.soundzone.queue.dto.SongRequest;
import com.soundzone.queue.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 队列控制器（docs/02 第 3 步：点歌 + 点赞）
 */
@RestController
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    /** 点歌：黑名单校验 → 时段白名单 → 入活跃/预存队列 */
    @PostMapping("/zones/{zoneId}/queue")
    public Result<QueueItemDTO> request(@PathVariable Long zoneId,
                                        @Valid @RequestBody SongRequest req) {
        return Result.ok(queueService.requestSong(zoneId, req));
    }

    /** 点赞队列条目（+1 赞并重算得分） */
    @PostMapping("/queue/{itemId}/like")
    public Result<QueueItemDTO> like(@PathVariable Long itemId,
                                     @RequestParam Long userId) {
        return Result.ok(queueService.like(itemId, userId));
    }
}
