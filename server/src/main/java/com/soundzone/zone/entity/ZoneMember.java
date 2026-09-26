package com.soundzone.zone.entity;

import com.soundzone.user.entity.User;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

/** 域成员（2026-09-24 决议 D2） 用于：同频人数统计、全员退出自动消失的判定 【假设】Demo 用显式 join/leave 模拟在线态；正式版由 WS 连接生命周期驱动 */
@Data
@Entity
@Table(
        name = "sz_zone_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"zone_id", "user_id"}))
public class ZoneMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    /** 可空以兼容旧数据；首次清理时给予一个心跳宽限期。 */
    private LocalDateTime lastSeenAt;
}
