package com.soundzone.track.entity;

import jakarta.persistence.*;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 曲目表（音源抽象层的本地映射，docs/05 §1 约束 1）。source 标识音源，externalId 关联外部曲目；
 * 授权说明只保存可核验的来源，不把第三方元数据误当成本地版权证明。
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

    /** 曲库封面地址，可空。 */
    @Column(length = 512)
    private String coverUrl;

    /** 时长（秒），用于播放进度计算 */
    @Column(nullable = false)
    private Integer durationSec = 240;

    /** 音源标识：MOCK / AUDIUS / JAMENDO / TME（预留） */
    @Column(nullable = false, length = 16)
    private String source = "MOCK";

    /** 外部音源曲目 ID（MOCK 时为空） */
    @Column(length = 64)
    private String externalId;

    /** 前端展示所需的来源署名，例如 Audius 或独立音乐人。 */
    @Column(length = 256)
    private String attribution;

    /** 本地授权曲目的合同、许可页面或内部凭证引用；外部平台曲目保存其公开页面。 */
    @Column(length = 512)
    private String licenseReference;

    /**
     * 曲风标签（多维）：流行 / 抖音热曲 / 情歌 / 舒缓 / 电子 / 摇滚 / 纯音乐 / City Pop / Lo-Fi… 曲风黑名单与番茄钟白名单的判定基础（docs/02 决议
     * D1/D3） 存储为关联表 sz_track_tags（tag 为元素集合）
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sz_track_tags", joinColumns = @JoinColumn(name = "track_id"))
    @Column(name = "tag", length = 32)
    private Set<String> tags = new HashSet<>();
}
