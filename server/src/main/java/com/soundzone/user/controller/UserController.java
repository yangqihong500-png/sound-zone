package com.soundzone.user.controller;

import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.*;
import com.soundzone.moment.service.MomentService;
import com.soundzone.user.dto.UpdateProfileRequest;
import com.soundzone.user.service.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/me/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> updateCover(@RequestParam("file") MultipartFile file) {
        return Result.ok(users.updateCover(current.id(), file));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> updateAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(users.updateAvatar(current.id(), file));
    }

    @PutMapping("/me/profile")
    public Result<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(users.updateName(current.id(), request.name()));
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

    @GetMapping("/me/listening-summary")
    public Result<?> listeningSummary() {
        return Result.ok(users.listeningSummary(current.id()));
    }
}
