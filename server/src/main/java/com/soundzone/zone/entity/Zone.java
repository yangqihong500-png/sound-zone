package com.soundzone.zone.entity;

import com.soundzone.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 域表（docs/01 核心概念）
 * 域 = 场景容器：名称即标签；风格治理 = 域主曲风黑名单（docs/02 决议 D1）
 */
@Data
@Entity
@Table(name = "sz_zone")
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 域名，即场景名（如「考研自习室」「日本 solo trip」） */
    @Column(nullable = false, length = 64)
    private String name;

    /** 归一化后的场景标签（自习/健身/旅行/手工/工作/深夜…），用于发现页筛选与分发 */
    @Column(nullable = false, length = 32)
    private String scene;

    /** 域主 */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "host_id")
    private User host;

    /** 主题色（Demo 封面占位） */
    @Column(length = 16)
    private String coverColor = "#9FE1CB";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ZoneStatus status = ZoneStatus.ACTIVE;

    /** 域风格标签（展示用，由域主勾选） */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sz_zone_tags", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "tag", length = 32)
    private Set<String> tags = new HashSet<>();

    /** 曲风黑名单：命中标签的歌不可点（docs/02 决议 D1） */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sz_zone_banned_tags", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "tag", length = 32)
    private Set<String> bannedTags = new HashSet<>();

    /**
     * 同频人数（听众数）
     * 【假设】Demo 阶段为冗余展示字段；正式版由 WebSocket 在线连接数实时统计（docs/05 接入层）
     */
    @Column(nullable = false)
    private Integer listenerCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 结束时间（结束后生成域歌单与战报） */
    private LocalDateTime endedAt;
}
