package com.soundzone.zone.controller;

import com.soundzone.common.Result;
import com.soundzone.zone.dto.JoinZoneRequest;
import com.soundzone.zone.dto.ZoneCreateRequest;
import com.soundzone.zone.dto.ZoneDetailDTO;
import com.soundzone.zone.dto.ZoneSummaryDTO;
import com.soundzone.zone.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 域控制器（docs/02 生命周期：创建/分发/进入/退出/详情/结束，v2）
 * 路径与前端 api 层约定一致
 */
@RestController
@RequestMapping("/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    /** 活跃公开域列表（首页/发现页）；scene 为空或「全部」时不筛选；私密域不参与分发 */
    @GetMapping("/active")
    public Result<List<ZoneSummaryDTO>> active(@RequestParam(required = false) String scene) {
        return Result.ok(zoneService.listActive(scene));
    }

    /** 创建域（≥3 首歌 + 公开/私密 + 过滤双模式 + 可选番茄钟） */
    @PostMapping
    public Result<ZoneDetailDTO> create(@Valid @RequestBody ZoneCreateRequest req) {
        return Result.ok(zoneService.createZone(req));
    }

    /** 进入域（公开域直接进；私密域需密码或邀请码，决议 D2） */
    @PostMapping("/{id}/join")
    public Result<ZoneDetailDTO> join(@PathVariable Long id,
                                      @Valid @RequestBody JoinZoneRequest req) {
        return Result.ok(zoneService.joinZone(id, req));
    }

    /** 退出域（全员退出后域自动消失，决议 D2） */
    @PostMapping("/{id}/leave")
    public Result<Void> leave(@PathVariable Long id, @RequestParam Long userId) {
        zoneService.leaveZone(id, userId);
        return Result.ok();
    }

    /** 域详情（当前播放 + FIFO 队列 + 动态区） */
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
