package com.soundzone.zone.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.BizException;
import com.soundzone.common.Result;
import com.soundzone.common.ResultCode;
import com.soundzone.notification.dto.ZoneInviteRequest;
import com.soundzone.notification.service.NotificationService;
import com.soundzone.zone.dto.*;
import com.soundzone.zone.service.ZoneService;

import jakarta.validation.Valid;
import jakarta.validation.Validator;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/zones")
@RequiredArgsConstructor
public class ZoneController {
    private final ZoneService zones;
    private final CurrentUser current;
    private final ObjectMapper json;
    private final Validator validator;
    private final NotificationService notifications;

    @GetMapping("/active")
    public Result<?> active(
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String keyword) {
        return Result.ok(zones.listActive(scene, keyword));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<?> create(@Valid @RequestBody ZoneCreateRequest req) {
        return Result.ok(zones.createZone(req, current.id()));
    }

    /** 自定义封面与创建事务一次提交，避免先上传后放弃造成孤立文件。 */
    @PostMapping(value = "/with-cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> createWithCover(
            @RequestParam("payload") String payload, @RequestParam("file") MultipartFile file) {
        ZoneCreateRequest req;
        try {
            req = json.readValue(payload, ZoneCreateRequest.class);
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.PARAM_INVALID, "创建域参数无效");
        }
        var violations = validator.validate(req);
        if (!violations.isEmpty())
            throw new BizException(
                    ResultCode.PARAM_INVALID,
                    violations.stream()
                            .map(v -> v.getMessage())
                            .sorted()
                            .collect(Collectors.joining("；")));
        return Result.ok(zones.createZone(req, current.id(), file));
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

    @PostMapping("/{id}/invites")
    public Result<?> sendInvite(
            @PathVariable Long id, @Valid @RequestBody ZoneInviteRequest request) {
        return Result.ok(notifications.invite(id, current.id(), request.recipientId()));
    }

    @PostMapping("/{id}/heartbeat")
    public Result<?> heartbeat(@PathVariable Long id, @RequestBody Heartbeat req) {
        zones.heartbeat(id, current.id(), req.itemId(), req.playing());
        return Result.ok();
    }

    public record Heartbeat(Long itemId, boolean playing) {}
    // 不开放手动结束域，结束由全员离开或心跳过期驱动。
}
