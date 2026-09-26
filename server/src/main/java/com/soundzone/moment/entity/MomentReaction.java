package com.soundzone.moment.entity;

import com.soundzone.user.entity.User;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "sz_moment_reaction",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "moment_id"}))
public class MomentReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moment_id")
    private Moment moment;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
