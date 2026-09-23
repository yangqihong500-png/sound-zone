package com.soundzone.moment.entity;

import com.soundzone.track.entity.Track;
import com.soundzone.user.entity.User;
import com.soundzone.zone.entity.Zone;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 碎片（docs/01 碎片墙 / docs/03 数据飞轮源头）
 * 每条碎片 = (场景, 图/文, 配乐) 三元组：
 * scene 由 zone 冗余，track 在发布时自动绑定域内当前播放歌曲（docs/02 第 4 步）
 */
@Data
@Entity
@Table(name = "sz_moment", indexes = {
        @Index(name = "idx_moment_zone", columnList = "zone_id")
})
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

    /** 文案（与图片至少其一非空，服务层校验） */
    @Column(length = 500)
    private String text;

    /** 图片 URL（Demo 阶段可空，用封面色占位；正式版为 COS 地址） */
    @Column(length = 512)
    private String imageUrl;

    /** 配图占位色（Demo） */
    @Column(length = 16)
    private String color;

    /** 发布时域内正在播放的歌 —— 自动绑定，用户无需选择 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "track_id")
    private Track track;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
