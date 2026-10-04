package com.soundzone.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 修改公开用户 ID；已注册账号会同步更新登录名。 */
public record UpdateProfileRequest(
        @NotBlank(message = "用户 ID 不能为空")
                @Size(min = 2, max = 32, message = "用户 ID 需为 2 到 32 个字符")
                @Pattern(
                        regexp = "^[\\p{L}\\p{N}_.-]+$",
                        message = "用户 ID 只能包含文字、数字、点、横线和下划线")
                String name) {}
