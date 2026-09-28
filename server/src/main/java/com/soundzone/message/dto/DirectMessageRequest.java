package com.soundzone.message.dto;

import jakarta.validation.constraints.*;

public record DirectMessageRequest(
        @NotBlank(message = "消息不能为空")
                @Size(max = 500, message = "消息最多 500 字")
                String body) {}
