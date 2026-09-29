package com.soundzone.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountRequest(
        @NotBlank(message = "用户名不能为空")
                @Size(min = 2, max = 32, message = "用户名需为 2 到 32 个字符")
                @Pattern(
                        regexp = "^[\\p{L}\\p{N}_.-]+$",
                        message = "用户名只能包含文字、数字、点、横线和下划线")
                String username,
        @NotBlank(message = "密码不能为空")
                @Size(min = 8, max = 72, message = "密码需为 8 到 72 个字符")
                String password) {}
