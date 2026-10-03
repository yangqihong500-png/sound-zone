package com.soundzone.notification.dto;

import com.soundzone.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        String type,
        String title,
        String body,
        Integer eventCount,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long actorId,
        String actorName,
        String actorAvatarColor,
        Long zoneId,
        String zoneName,
        String inviteCode,
        Long queueItemId,
        String coverUrl) {
    public static NotificationDTO from(Notification notification) {
        var actor = notification.getActor();
        var zone = notification.getZone();
        var item = notification.getQueueItem();
        return new NotificationDTO(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getEventCount(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getName(),
                actor == null ? null : actor.getAvatarColor(),
                zone == null ? null : zone.getId(),
                zone == null ? null : zone.getName(),
                notification.getType() == com.soundzone.notification.entity.NotificationType.ZONE_INVITE
                                && zone != null
                        ? zone.getInviteCode()
                        : null,
                item == null ? null : item.getId(),
                item == null ? (zone == null ? null : zone.getCoverUrl()) : item.getTrack().getCoverUrl());
    }
}
