package com.soundzone.queue.entity;

import com.soundzone.track.entity.Track;
import com.soundzone.user.entity.User;
import com.soundzone.zone.entity.Zone;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点歌队列条目（docs/02 第 3 步）
 * 队列得分 = 点赞数 × 2 + 域主加成 × 3 − 该用户近 1 小时已播点歌数 × 1.5
 * 得分由 QueueService 在点赞/播放事件后重算并落库（冗余存储便于排序查询）
 */
@Data
@Entity
@Table(name = "sz_queue_item", indexes = {
        @Index(name = "idx_queue_zone_status", columnList = "zone_id,status")
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

    /** 点歌人（审美反馈与歌品值的归属用户） */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User requester;

    /** 收到的点赞数 */
    @Column(nullable = false)
    private Integer likes = 0;

    /** 域主加成：点歌人即域主时为 1，否则 0（公式中 ×3） */
    @Column(nullable = false)
    private Integer hostBonus = 0;

    /** 队列得分（见类注释公式） */
    @Column(nullable = false)
    private Double score = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QueueStatus status = QueueStatus.QUEUED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 开始播放时间（PLAYING 后用于进度对齐；正式版为服务端权威时钟字段） */
    private LocalDateTime startedAt;

    /** 播放完成时间（近 1 小时已播惩罚的统计窗口） */
    private LocalDateTime playedAt;
}
