package com.soundzone.user.controller;

import com.soundzone.common.Result;
import com.soundzone.user.dto.UserProfileDTO;
import com.soundzone.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器（我的页 + 关注体系，v2）
 * 【假设】Demo 无登录体系，userId 显式传递；正式版从登录态解析
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 我的页信息（功能型主页：上传/获赞/分享/关注） */
    @GetMapping("/{userId}/profile")
    public Result<UserProfileDTO> profile(@PathVariable Long userId) {
        return Result.ok(userService.getProfile(userId));
    }

    /** 关注上传者（决议 D7，幂等） */
    @PostMapping("/{userId}/follow")
    public Result<Void> follow(@PathVariable Long userId, @RequestParam Long fromUserId) {
        userService.follow(fromUserId, userId);
        return Result.ok();
    }

    /** 取消关注 */
    @DeleteMapping("/{userId}/follow")
    public Result<Void> unfollow(@PathVariable Long userId, @RequestParam Long fromUserId) {
        userService.unfollow(fromUserId, userId);
        return Result.ok();
    }

    /** 我的关注列表 */
    @GetMapping("/{userId}/following")
    public Result<List<String>> following(@PathVariable Long userId) {
        return Result.ok(userService.following(userId));
    }
}
