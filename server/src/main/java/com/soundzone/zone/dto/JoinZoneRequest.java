package com.soundzone.zone.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 进入域请求（公开域直接进；私密域需密码或邀请码，决议 D2） */
public record JoinZoneRequest(
        @NotNull(message = "用户 ID 不能为空") Long userId,
        @Size(max = 32) String password,
        @Size(max = 32) String inviteCode
) {}
