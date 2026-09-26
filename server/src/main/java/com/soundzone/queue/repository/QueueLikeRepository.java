package com.soundzone.queue.repository;

import com.soundzone.queue.entity.QueueLike;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface QueueLikeRepository extends JpaRepository<QueueLike, Long> {
    Optional<QueueLike> findByUserIdAndItemId(Long userId, Long itemId);

    boolean existsByUserIdAndItemId(Long userId, Long itemId);
}
