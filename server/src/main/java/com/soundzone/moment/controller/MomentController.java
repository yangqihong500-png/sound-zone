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
 * 碎片控制器（docs/02 第 4 步：碎片墙）
 */
@RestController
@RequestMapping("/zones/{zoneId}/moments")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;

    /** 发布碎片（自动绑定当前播放歌曲） */
    @PostMapping
    public Result<MomentDTO> create(@PathVariable Long zoneId,
                                    @Valid @RequestBody MomentCreateRequest req) {
        return Result.ok(momentService.create(zoneId, req));
    }

    /** 碎片墙列表 */
    @GetMapping
    public Result<List<MomentDTO>> list(@PathVariable Long zoneId) {
        return Result.ok(momentService.listByZone(zoneId));
    }
}
