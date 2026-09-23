package com.soundzone.user.controller;

import com.soundzone.common.Result;
import com.soundzone.user.dto.UserProfileDTO;
import com.soundzone.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器（我的页）
 * 【假设】Demo 无登录体系，userId 显式传递；正式版从登录态解析
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 我的页信息（歌品值 + 点歌/获赞/碎片统计） */
    @GetMapping("/{userId}/profile")
    public Result<UserProfileDTO> profile(@PathVariable Long userId) {
        return Result.ok(userService.getProfile(userId));
    }
}
