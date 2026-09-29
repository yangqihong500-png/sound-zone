package com.soundzone.auth.entity;

import com.soundzone.user.entity.User;

import jakarta.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

/** 独立 App 账号凭据。登录名只用于认证，展示昵称仍由 User 维护。 */
@Data
@Entity
@Table(name = "sz_user_credential")
public class UserCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 32)
    private String loginName;

    @Column(nullable = false, length = 256)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
