package com.soundzone.moment.entity;

import com.soundzone.queue.entity.QueueItem;
import com.soundzone.track.entity.Track;
import com.soundzone.user.entity.User;
import com.soundzone.zone.entity.Zone;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图片分享（docs/02 第 4 步，v2：2026-09-24 会议） 上传歌曲时可选附图，图片与歌曲关联 —— 每条分享 = (场景, 图片, 关联歌曲) 三元组 用户可撤回自己的分享（决议
 * D6）
 */
@Data
@Entity
@Table(
        name = "sz_moment",
        indexes = {@Index(name = "idx_moment_zone", columnList = "zone_id")})
public class Moment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** 文案（可空，v2 以图片为主） */
    @Column(length = 500)
    private String text;

    /** 图片 URL（正式版为 COS 地址；Demo 可为空，用色块占位） */
    @Column(length = 512)
    private String imageUrl;

    /** 配图占位色（Demo） */
    @Column(length = 16)
    private String color;

    /** 关联歌曲（v2 绑定规则）： 由 queueItem 确定本次上传歌曲；旧 track_id 保留用于历史展示 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "track_id")
    private Track track;

    /** 一次上传最多附一张图片；历史数据无法可靠回填时保留 NULL。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_item_id", unique = true)
    private QueueItem queueItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModerationStatus moderationStatus = ModerationStatus.APPROVED;

    @Column(nullable = false)
    private boolean trainingConsent = false;

    private LocalDateTime consentedAt;
    private LocalDateTime withdrawnAt;

    /** 状态：NORMAL / WITHDRAWN（撤回后列表排除） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MomentStatus status = MomentStatus.NORMAL;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
