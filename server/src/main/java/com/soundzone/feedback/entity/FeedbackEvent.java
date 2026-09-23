package com.soundzone.feedback.entity;

import com.soundzone.track.entity.Track;
import com.soundzone.user.entity.User;
import com.soundzone.zone.entity.Zone;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反馈事件（docs/02 决议 D2）
 * 听众对"当前播放的歌曲"点红心/收藏时记录：
 * - toUser = 该曲的点歌人 → 触发其实时特效（正式版走 WS 定向推送）
 * - 事件聚合 → 域后个人战报 + 歌品值计算
 */
@Data
@Entity
@Table(name = "sz_feedback_event", indexes = {
        @Index(name = "idx_feedback_zone", columnList = "zone_id"),
        @Index(name = "idx_feedback_to_user", columnList = "to_user_id")
})
public class FeedbackEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    /** 被反馈的歌曲（通常为当前播放曲） */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "track_id")
    private Track track;

    /** 反馈发起者（听众） */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "from_user_id")
    private User fromUser;

    /** 反馈归属者（该曲的点歌人，特效接收方） */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "to_user_id")
    private User toUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FeedbackType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
