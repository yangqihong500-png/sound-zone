package com.soundzone.user.repository;

import com.soundzone.user.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    /** 我的关注列表 */
    List<Follow> findByFollowerIdOrderByCreatedAtDesc(Long followerId);

    long countByFollowerId(Long followerId);

    long countByFolloweeId(Long followeeId);

    void deleteByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
}
