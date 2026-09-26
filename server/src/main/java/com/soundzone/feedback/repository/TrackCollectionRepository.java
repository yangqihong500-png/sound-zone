package com.soundzone.feedback.repository;

import com.soundzone.feedback.entity.TrackCollection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface TrackCollectionRepository extends JpaRepository<TrackCollection, Long> {
    Optional<TrackCollection> findByUserIdAndTrackId(Long userId, Long trackId);

    boolean existsByUserIdAndTrackId(Long userId, Long trackId);

    List<TrackCollection> findByUserIdOrderByCreatedAtDesc(Long userId);
}
