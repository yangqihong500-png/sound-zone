package com.soundzone.auth.repository;

import com.soundzone.auth.entity.UserCredential;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByLoginName(String loginName);

    boolean existsByLoginName(String loginName);

    boolean existsByUserId(Long userId);
}
