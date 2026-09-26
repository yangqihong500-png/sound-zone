package com.soundzone.moment.repository;

import com.soundzone.moment.entity.MomentReaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface MomentReactionRepository extends JpaRepository<MomentReaction, Long> {
    Optional<MomentReaction> findByUserIdAndMomentId(Long userId, Long momentId);
}
