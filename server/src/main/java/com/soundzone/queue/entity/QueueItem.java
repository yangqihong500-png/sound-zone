package com.soundzone.queue.entity;

import com.soundzone.track.entity.Track;
import com.soundzone.user.entity.User;
import com.soundzone.zone.entity.Zone;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 上传队列条目（docs/02 第 3 步，v2：2026-09-24 会议）
 *
 * <p>v2 关键变更： - 播放顺序 = 按上传顺序 FIFO（createdAt 升序），v1 的队列得分公式已废除 - 点赞保留，但仅作互动信号，不影响播放顺序 - 上传秩序由 10
 * 分钟冷却 + 标签过滤维持（取代 v1 刷屏惩罚）
 */
@Data
@Entity
@Table(
        name = "sz_queue_item",
        indexes = {
            @Index(name = "idx_queue_zone_status", columnList = "zone_id,status"),
            @Index(name = "idx_queue_fifo", columnList = "zone_id,status,created_at,id"),
            @Index(name = "idx_queue_cooldown", columnList = "zone_id,user_id,created_at,id")
        })
public class QueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "track_id")
    private Track track;

    /** 上传者（审美反馈与图片关联的归属用户） */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User requester;

    /** 收到的点赞数（互动信号，不影响 FIFO 播放顺序） */
    @Column(nullable = false)
    private Integer likes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QueueStatus status = QueueStatus.QUEUED;

    /** 上传时间：FIFO 的排序依据 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 开始播放时间（PLAYING 后用于进度对齐；正式版为服务端权威时钟字段） */
    private LocalDateTime startedAt;

    /** 播放完成时间 */
    private LocalDateTime playedAt;
}
