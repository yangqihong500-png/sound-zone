package com.soundzone.queue.controller;

import com.soundzone.common.Result;
import com.soundzone.queue.dto.CooldownDTO;
import com.soundzone.queue.dto.QueueItemDTO;
import com.soundzone.queue.dto.SongRequest;
import com.soundzone.queue.service.QueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 队列控制器（docs/02 第 3 步：上传歌曲 + 点赞 + 冷却查询，v2）
 */
@RestController
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    /** 上传歌曲：冷却 → 域级过滤 → 时段白名单 → 入队尾（FIFO） */
    @PostMapping("/zones/{zoneId}/queue")
    public Result<QueueItemDTO> request(@PathVariable Long zoneId,
                                        @Valid @RequestBody SongRequest req) {
        return Result.ok(queueService.requestSong(zoneId, req));
    }

    /** 点赞队列条目（仅互动信号，不影响 FIFO 播放顺序） */
    @PostMapping("/queue/{itemId}/like")
    public Result<QueueItemDTO> like(@PathVariable Long itemId,
                                     @RequestParam Long userId) {
        return Result.ok(queueService.like(itemId, userId));
    }

    /** 上传冷却状态：前端按钮置灰与倒计时（决议 D4） */
    @GetMapping("/zones/{zoneId}/cooldown")
    public Result<CooldownDTO> cooldown(@PathVariable Long zoneId, @RequestParam Long userId) {
        return Result.ok(new CooldownDTO(queueService.cooldownRemainSeconds(zoneId, userId), 10));
    }
}
