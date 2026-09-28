package com.soundzone.message.entity;

import com.soundzone.user.entity.User;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 关注关系中的一对一文字私信。 */
@Data
@Entity
@Table(
        name = "sz_direct_message",
        indexes = {
            @Index(name = "idx_message_sender_recipient", columnList = "sender_id,recipient_id,created_at,id"),
            @Index(name = "idx_message_recipient_sender", columnList = "recipient_id,sender_id,created_at,id")
        })
public class DirectMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
