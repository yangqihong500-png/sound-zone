package com.soundzone.auth.controller;

import com.soundzone.activity.service.ActivityService;
import com.soundzone.auth.service.*;
import com.soundzone.common.Result;
import com.soundzone.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {
    private final SessionService sessions;
    private final CurrentUser current;
    private final UserService users;
    private final ActivityService activity;

    @GetMapping("/config")
    public Result<?> config() {
        return Result.ok(
                Map.of(
                        "guest", sessions.guestEnabled(),
                        "host", sessions.hostEnabled(),
                        "demo", sessions.guestEnabled())); // demo 字段兼容旧前端，后续版本可移除。
    }

    @PostMapping("/guest")
    public Result<?> guest() {
        return Result.ok(sessions.guest());
    }

    @PostMapping("/host")
    public Result<?> host(@RequestBody Map<String, String> req) {
        return Result.ok(sessions.host(req.get("code")));
    }

    @GetMapping("/me")
    public Result<?> me() {
        Long id = current.id();
        activity.visit(id);
        return Result.ok(users.getProfile(id, id));
    }
}
