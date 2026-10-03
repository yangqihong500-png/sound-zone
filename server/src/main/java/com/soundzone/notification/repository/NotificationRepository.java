package com.soundzone.notification.repository;

import com.soundzone.notification.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop100ByRecipientIdOrderByUpdatedAtDescIdDesc(Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query(
            "update Notification n set n.readAt = :readAt "
                    + "where n.recipient.id = :recipientId and n.readAt is null")
    int markAllRead(
            @Param("recipientId") Long recipientId, @Param("readAt") LocalDateTime readAt);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    Optional<Notification>
            findFirstByRecipientIdAndActorIdAndTypeAndReadAtIsNullOrderByUpdatedAtDescIdDesc(
                    Long recipientId, Long actorId, NotificationType type);

    Optional<Notification>
            findFirstByRecipientIdAndZoneIdAndTypeAndReadAtIsNullOrderByUpdatedAtDescIdDesc(
                    Long recipientId, Long zoneId, NotificationType type);

    Optional<Notification>
            findFirstByRecipientIdAndQueueItemIdAndTypeAndReadAtIsNullOrderByUpdatedAtDescIdDesc(
                    Long recipientId, Long queueItemId, NotificationType type);
}
