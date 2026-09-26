package com.soundzone.activity.repository;

import com.soundzone.activity.entity.ActivityEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.*;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {
    List<ActivityEvent> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start, LocalDateTime end);

    boolean existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
            Long userId, String type, LocalDateTime start);
}
