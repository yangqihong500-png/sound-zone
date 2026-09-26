package com.soundzone.zone.controller;

import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.Result;
import com.soundzone.zone.dto.*;
import com.soundzone.zone.service.ZoneService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/zones")
@RequiredArgsConstructor
public class ZoneController {
    private final ZoneService zones;
    private final CurrentUser current;

    @GetMapping("/active")
    public Result<?> active(
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String keyword) {
        return Result.ok(zones.listActive(scene, keyword));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody ZoneCreateRequest req) {
        return Result.ok(zones.createZone(req, current.id()));
    }

    @PostMapping("/{id}/join")
    public Result<?> join(@PathVariable Long id, @Valid @RequestBody JoinZoneRequest req) {
        return Result.ok(zones.joinZone(id, req, current.id()));
    }

    @PostMapping("/{id}/leave")
    public Result<?> leave(@PathVariable Long id) {
        zones.leaveZone(id, current.id());
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(zones.getDetail(id, current.id()));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ZoneUpdateRequest req) {
        return Result.ok(zones.update(id, current.id(), req));
    }

    @GetMapping("/{id}/invite")
    public Result<?> invite(@PathVariable Long id) {
        return Result.ok(Map.of("inviteCode", zones.invite(id, current.id())));
    }

    @PostMapping("/{id}/heartbeat")
    public Result<?> heartbeat(@PathVariable Long id, @RequestBody Heartbeat req) {
        zones.heartbeat(id, current.id(), req.itemId(), req.playing());
        return Result.ok();
    }

    public record Heartbeat(Long itemId, boolean playing) {}
    // 不开放手动结束域，结束由全员离开或心跳过期驱动。
}
