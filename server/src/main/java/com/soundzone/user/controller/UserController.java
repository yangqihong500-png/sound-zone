package com.soundzone.user.controller;

import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.*;
import com.soundzone.moment.service.MomentService;
import com.soundzone.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;
    private final MomentService moments;
    private final CurrentUser current;

    @GetMapping("/{id}/profile")
    public Result<?> profile(@PathVariable Long id) {
        return Result.ok(users.getProfile(id, current.id()));
    }

    @PostMapping("/{id}/follow")
    public Result<?> follow(@PathVariable Long id) {
        users.follow(current.id(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/follow")
    public Result<?> unfollow(@PathVariable Long id) {
        users.unfollow(current.id(), id);
        return Result.ok();
    }

    @GetMapping("/me/following")
    public Result<?> following() {
        return Result.ok(users.following(current.id()));
    }

    @GetMapping("/me/collections")
    public Result<?> collections() {
        return Result.ok(users.collections(current.id()));
    }

    @GetMapping("/me/uploads")
    public Result<?> uploads() {
        return Result.ok(users.uploads(current.id()));
    }

    @GetMapping("/me/zones")
    public Result<?> zones() {
        return Result.ok(users.zones(current.id()));
    }

    @GetMapping("/me/moments")
    public Result<?> moments() {
        return Result.ok(moments.mine(current.id()));
    }
}
