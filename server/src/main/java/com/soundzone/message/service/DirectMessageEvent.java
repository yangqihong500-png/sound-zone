package com.soundzone.message.service;

public record DirectMessageEvent(Long messageId, Long senderId, Long recipientId) {}
