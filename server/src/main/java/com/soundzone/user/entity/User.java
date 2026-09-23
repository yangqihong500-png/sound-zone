package com.soundzone.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 * tasteScore = 歌品值（docs/02 成长体系）：点歌被点赞率，由 FeedbackService 定期重算
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
    private String avatarColor = "#31C27C";

    /** 歌品值 0~100，冗余存储避免每次实时计算 */
    @Column(nullable = false)
    private Double tasteScore = 0.0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
