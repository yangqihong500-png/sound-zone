package com.soundzone.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表（v2：2026-09-24 会议）
 * v2 变更：移除歌品值字段 tasteScore（决议 D7 去游戏化：不做积分/等级/勋章）
 */
@Data
@Entity
@Table(name = "sz_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 昵称，全局唯一 */
    @Column(nullable = false, unique = true, length = 32)
    private String name;

    /** 头像占位色（Demo 阶段与前端一致，正式版替换为 COS 头像 URL） */
    @Column(nullable = false, length = 16)
    private String avatarColor = "#8C9BAB";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
