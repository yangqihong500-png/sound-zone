package com.soundzone.notification.entity;

import com.soundzone.queue.entity.QueueItem;
import com.soundzone.user.entity.User;
import com.soundzone.zone.entity.Zone;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户站内通知。未读私信和点赞按业务对象聚合，减少重复提醒。 */
@Data
@Entity
@Table(
        name = "sz_notification",
        indexes = {
            @Index(
                    name = "idx_notification_recipient_unread",
                    columnList = "recipient_id,read_at,updated_at,id"),
            @Index(
                    name = "idx_notification_aggregate",
                    columnList = "recipient_id,type,queue_item_id,actor_id,read_at")
        })
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 300)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_item_id")
    private QueueItem queueItem;

    /** 仅保存最近一条私信 ID，跳转仍按对方用户打开完整会话。 */
    private Long messageId;

    @Column(nullable = false)
    private Integer eventCount = 1;

    private LocalDateTime readAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
