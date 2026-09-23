package com.soundzone.zone.controller;

import com.soundzone.common.Result;
import com.soundzone.zone.dto.ZoneCreateRequest;
import com.soundzone.zone.dto.ZoneDetailDTO;
import com.soundzone.zone.dto.ZoneSummaryDTO;
import com.soundzone.zone.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 域控制器（docs/02 生命周期：创建/分发/详情/结束）
 * 路径与前端 mock.js 约定一致
 */
@RestController
@RequestMapping("/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    /** 活跃域列表（首页/发现页）；scene 为空或「全部」时不筛选 */
    @GetMapping("/active")
    public Result<List<ZoneSummaryDTO>> active(@RequestParam(required = false) String scene) {
        return Result.ok(zoneService.listActive(scene));
    }

    /** 创建域（≥3 首歌 + 场景命名 + 可选黑名单/番茄钟） */
    @PostMapping
    public Result<ZoneDetailDTO> create(@Valid @RequestBody ZoneCreateRequest req) {
        return Result.ok(zoneService.createZone(req));
    }

    /** 域详情（播放中 + 队列 + 碎片墙） */
    @GetMapping("/{id}")
    public Result<ZoneDetailDTO> detail(@PathVariable Long id) {
        return Result.ok(zoneService.getDetail(id));
    }

    /** 结束域（触发战报聚合与归档） */
    @PostMapping("/{id}/end")
    public Result<Void> end(@PathVariable Long id) {
        zoneService.endZone(id);
        return Result.ok();
    }
}
