package com.soundzone.auth.repository;

import com.soundzone.auth.entity.AuthSession;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.*;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByTokenHashAndExpiresAtAfter(String hash, LocalDateTime now);
}
