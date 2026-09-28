package com.soundzone.message.dto;

import com.soundzone.message.entity.DirectMessage;

import java.time.LocalDateTime;

public record DirectMessageDTO(
        Long id, Long fromUserId, Long toUserId, String body, LocalDateTime createdAt) {
    public static DirectMessageDTO from(DirectMessage message) {
        return new DirectMessageDTO(
                message.getId(),
                message.getSender().getId(),
                message.getRecipient().getId(),
                message.getBody(),
                message.getCreatedAt());
    }
}
