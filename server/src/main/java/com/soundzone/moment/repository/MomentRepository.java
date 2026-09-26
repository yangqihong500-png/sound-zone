package com.soundzone.moment.repository;

import com.soundzone.moment.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.*;

public interface MomentRepository extends JpaRepository<Moment, Long> {
    List<Moment> findByZoneIdAndStatusAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
            Long zoneId, MomentStatus status, LocalDateTime after);

    List<Moment> findByZoneIdAndStatusOrderByCreatedAtDescIdDesc(
            Long zoneId, MomentStatus status);

    Optional<Moment> findFirstByZoneIdAndText(Long zoneId, String text);

    boolean existsByQueueItemId(Long queueItemId);

    long countByUserIdAndStatus(Long userId, MomentStatus status);

    List<Moment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Moment> findByTrainingConsentTrueAndStatusAndModerationStatus(
            MomentStatus status, ModerationStatus moderationStatus);
}
