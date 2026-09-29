package com.soundzone.auth.controller;

import com.soundzone.activity.service.ActivityService;
import com.soundzone.auth.dto.AccountRequest;
import com.soundzone.auth.service.*;
import com.soundzone.common.Result;
import com.soundzone.user.service.UserService;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

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
                        "password", sessions.passwordAuthEnabled(),
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

    @PostMapping("/register")
    public Result<?> register(
            @Valid @RequestBody AccountRequest req,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(
                sessions.register(req.username(), req.password(), bearer(authorization)));
    }

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody AccountRequest req) {
        return Result.ok(sessions.login(req.username(), req.password()));
    }

    @PostMapping("/logout")
    public Result<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        sessions.logout(bearer(authorization));
        return Result.ok(null);
    }

    @GetMapping("/current")
    public Result<?> current(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(sessions.current(bearer(authorization)));
    }

    @GetMapping("/me")
    public Result<?> me() {
        Long id = current.id();
        activity.visit(id);
        return Result.ok(users.getProfile(id, id));
    }

    private String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
    }
}
