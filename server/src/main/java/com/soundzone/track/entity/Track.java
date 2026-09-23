package com.soundzone.track.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 曲目表（音源抽象层的本地映射，docs/05 §1 约束 1）
 * source 标识音源：DEMO 阶段为本地 mock 曲目；
 * 接入 Audius/Jamendo 后以 externalId 关联外部曲目
 */
@Data
@Entity
@Table(name = "sz_track")
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(nullable = false, length = 64)
    private String artist;

    /** 封面占位色（Demo），正式版为封面图 URL */
    @Column(length = 16)
    private String coverColor = "#9FE1CB";

    /** 时长（秒），用于播放进度计算 */
    @Column(nullable = false)
    private Integer durationSec = 240;

    /** 音源标识：MOCK / AUDIUS / JAMENDO / TME（预留） */
    @Column(nullable = false, length = 16)
    private String source = "MOCK";

    /** 外部音源曲目 ID（MOCK 时为空） */
    @Column(length = 64)
    private String externalId;

    /**
     * 曲风标签（多维）：流行 / 抖音热曲 / 情歌 / 舒缓 / 电子 / 摇滚 / 纯音乐 / City Pop / Lo-Fi…
     * 曲风黑名单与番茄钟白名单的判定基础（docs/02 决议 D1/D3）
     * 存储为关联表 sz_track_tags（tag 为元素集合）
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sz_track_tags", joinColumns = @JoinColumn(name = "track_id"))
    @Column(name = "tag", length = 32)
    private Set<String> tags = new HashSet<>();
}
