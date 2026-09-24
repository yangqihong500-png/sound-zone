package com.soundzone.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系（2026-09-24 决议 D7：轻入口关注上传者，不改变播放体验）
 */
@Data
@Entity
@Table(name = "sz_follow",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followee_id"}))
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关注者 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id")
    private User follower;

    /** 被关注者（上传者） */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "followee_id")
    private User followee;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
