package com.soundzone.zone.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 旧番茄钟时段配置，保留历史数据与上传准入兼容。
 * 本轮不开放新配置，不执行预存歌曲自动转正。
 */
@Data
@Entity
@Table(name = "sz_zone_period")
public class ZonePeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    /** 轮转顺序，从 0 开始 */
    @Column(nullable = false)
    private Integer orderIndex;

    /** 时段时长（分钟） */
    @Column(nullable = false)
    private Integer durationMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PeriodType type;

    /** 本时段允许的曲风标签（白名单）；为空表示不限制 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sz_zone_period_tags", joinColumns = @JoinColumn(name = "period_id"))
    @Column(name = "tag", length = 32)
    private Set<String> allowedTags = new HashSet<>();
}
