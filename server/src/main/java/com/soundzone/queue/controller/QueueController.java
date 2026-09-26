package com.soundzone.queue.controller;

import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.Result;
import com.soundzone.queue.dto.*;
import com.soundzone.queue.service.QueueService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QueueController {
    private final com.soundzone.common.ResourceLocator locator;
    private final QueueService queue;
    private final CurrentUser current;

    @PostMapping("/zones/{zoneId}/queue")
    public Result<?> request(@PathVariable Long zoneId, @Valid @RequestBody SongRequest req) {
        return Result.ok(queue.requestSong(zoneId, req, current.id()));
    }

    @PutMapping("/queue/{itemId}/like")
    public Result<?> like(@PathVariable Long itemId, @RequestBody LikeRequest req) {
        return Result.ok(queue.like(locator.queueZone(itemId), itemId, current.id(), req.active()));
    }

    @PostMapping("/queue/{itemId}/like")
    public Result<?> likeLegacy(@PathVariable Long itemId) {
        return Result.ok(queue.like(locator.queueZone(itemId), itemId, current.id(), true));
    }

    @GetMapping("/zones/{zoneId}/cooldown")
    public Result<?> cooldown(@PathVariable Long zoneId) {
        return Result.ok(new CooldownDTO(queue.cooldownRemainSeconds(zoneId, current.id()), 10));
    }

    public record LikeRequest(boolean active) {}
}
