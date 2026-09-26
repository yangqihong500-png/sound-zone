package com.soundzone.zone.entity;

import com.soundzone.user.entity.User;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 域表（docs/01 核心概念，v2：2026-09-24 会议） 域 = 场景容器：名称即标签；风格治理 = 标签过滤（禁止含/仅允许含，决议 D5）； 可见性分公开/私密（决议
 * D2），全员退出自动消失
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

    /** 主题色（莫兰迪低饱和色，2026-09-24 视觉规范） */
    @Column(length = 16)
    private String coverColor = "#A8B8C8";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ZoneStatus status = ZoneStatus.ACTIVE;

    /** 可见性：PUBLIC 参与分发 / PRIVATE 仅邀请链接或密码进入（决议 D2） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ZoneVisibility visibility = ZoneVisibility.PUBLIC;

    /** 私密域密码（与邀请码二选一或并存，可空） */
    @Column(length = 32)
    private String password;

    /** PBKDF2 密码摘要；password 仅用于历史兼容，过渡期保留以便回滚。 */
    @Column(length = 256)
    private String passwordHash;

    @Column(nullable = false)
    private long stateVersion = 0;

    /** 演示环境常驻域：保持活跃并循环预置歌单；普通用户域始终为 false。 */
    @Column(nullable = false)
    private boolean demoResident = false;

    /** 最近上传／图片行为时间，用于活跃域筛选。 */
    private LocalDateTime lastActivityAt;

    /** 私密域邀请码（分享链接中的凭证） */
    @Column(length = 32)
    private String inviteCode;

    /** 标签过滤模式：BAN=禁止含 / ALLOW=仅允许含（决议 D5） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FilterMode filterMode = FilterMode.BAN;

    /** 域风格标签（展示用，由域主勾选） */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sz_zone_tags", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "tag", length = 32)
    private Set<String> tags = new HashSet<>();

    /** 过滤标签集合： filterMode=BAN 时为黑名单（命中拒绝）； filterMode=ALLOW 时为白名单（不含拒绝） */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sz_zone_filter_tags", joinColumns = @JoinColumn(name = "zone_id"))
    @Column(name = "tag", length = 32)
    private Set<String> filterTags = new HashSet<>();

    /** 同频人数（听众数）= 当前域内成员数 由加入/退出实时维护（v2 起取代 v1 的冗余假设字段） */
    @Column(nullable = false)
    private Integer listenerCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 结束时间（全员退出自动消失或手动结束） */
    private LocalDateTime endedAt;
}
