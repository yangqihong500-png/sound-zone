package com.soundzone.user.entity;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

/** 用户表（v2：2026-09-24 会议） v2 变更：移除歌品值字段 tasteScore（决议 D7 去游戏化：不做积分/等级/勋章） */
@Data
@Entity
@Table(name = "sz_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 公开用户 ID，全局唯一；注册账号同时用作登录名。 */
    @Column(nullable = false, unique = true, length = 32)
    private String name;

    /** 宿主身份的稳定标识；游客为空，不接受客户端自报用户 ID。 */
    @Column(unique = true, length = 128)
    private String hostSubject;

    /** 未上传头像时使用的占位色。 */
    @Column(nullable = false, length = 16)
    private String avatarColor = "#8C9BAB";

    /** 用户主动上传的公开头像；为空时继续显示占位色与名字首字。 */
    @Column(length = 512)
    private String avatarUrl;

    /** 用户主动上传的公开主页背景；未上传时前端显示默认唱片画面。 */
    @Column(length = 512)
    private String coverUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
