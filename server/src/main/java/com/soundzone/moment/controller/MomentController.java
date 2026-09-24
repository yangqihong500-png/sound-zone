package com.soundzone.moment.controller;

import com.soundzone.common.Result;
import com.soundzone.moment.dto.MomentCreateRequest;
import com.soundzone.moment.dto.MomentDTO;
import com.soundzone.moment.service.MomentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图片分享控制器（docs/02 第 4 步，v2：绑定歌曲 / 半小时图片流 / 撤回）
 */
@RestController
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;

    /** 发布图片分享（绑定关联歌曲：指定 trackId 或回退当前播放） */
    @PostMapping("/zones/{zoneId}/moments")
    public Result<MomentDTO> create(@PathVariable Long zoneId,
                                    @Valid @RequestBody MomentCreateRequest req) {
        return Result.ok(momentService.create(zoneId, req));
    }

    /** 动态详情页：半小时内图片流（时间倒序） */
    @GetMapping("/zones/{zoneId}/moments/feed")
    public Result<List<MomentDTO>> feed(@PathVariable Long zoneId) {
        return Result.ok(momentService.feed(zoneId));
    }

    /** 撤回图片分享（仅本人） */
    @DeleteMapping("/moments/{momentId}")
    public Result<Void> withdraw(@PathVariable Long momentId, @RequestParam Long userId) {
        momentService.withdraw(momentId, userId);
        return Result.ok();
    }
}
