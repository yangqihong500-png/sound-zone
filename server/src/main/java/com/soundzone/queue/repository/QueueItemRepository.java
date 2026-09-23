package com.soundzone.queue.repository;

import com.soundzone.queue.entity.QueueItem;
import com.soundzone.queue.entity.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueItemRepository extends JpaRepository<QueueItem, Long> {

    /** 域的活跃队列：QUEUED 按得分降序（docs/02 队列排序） */
    List<QueueItem> findByZoneIdAndStatusOrderByScoreDesc(Long zoneId, QueueStatus status);

    /** 域的预存队列（按点歌时间先后） */
    List<QueueItem> findByZoneIdAndStatusOrderByCreatedAtAsc(Long zoneId, QueueStatus status);

    /** 当前播放条目（每域至多一条 PLAYING） */
    Optional<QueueItem> findFirstByZoneIdAndStatus(Long zoneId, QueueStatus status);

    /** 近 1 小时某用户在该域已播的点歌数（刷屏惩罚项） */
    long countByZoneIdAndRequesterIdAndStatusAndPlayedAtAfter(
            Long zoneId, Long requesterId, QueueStatus status, LocalDateTime after);

    /** 用户点歌总数 / 被点赞统计（歌品值计算） */
    long countByRequesterId(Long requesterId);

    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(q.likes), 0) from QueueItem q where q.requester.id = :userId")
    long sumLikesByRequesterId(Long userId);
}
