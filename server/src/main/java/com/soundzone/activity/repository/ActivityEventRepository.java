package com.soundzone.activity.repository;

import com.soundzone.activity.entity.ActivityEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {
    List<ActivityEvent> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start, LocalDateTime end);

    boolean existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
            Long userId, String type, LocalDateTime start);

    @Query(
            """
            select coalesce(sum(e.durationSeconds), 0)
            from ActivityEvent e
            where e.user.id = :userId and e.type = 'LISTEN'
            """)
    long sumListeningSeconds(@Param("userId") Long userId);

    List<ActivityEvent> findByUserIdAndTypeAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            Long userId, String type, LocalDateTime start);
}
