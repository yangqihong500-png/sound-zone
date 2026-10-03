package com.soundzone.notification.dto;

import jakarta.validation.constraints.NotNull;

public record ZoneInviteRequest(@NotNull Long recipientId) {}
