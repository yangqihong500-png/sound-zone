package com.soundzone.queue.repository;

import com.soundzone.queue.entity.QueueItem;
import com.soundzone.queue.entity.QueueStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueItemRepository extends JpaRepository<QueueItem, Long> {

    /** 域的待播队列：FIFO = 按上传时间升序（2026-09-24 决议 D3） */
    List<QueueItem> findByZoneIdAndStatusOrderByCreatedAtAscIdAsc(Long zoneId, QueueStatus status);

    /** 当前播放条目（每域至多一条 PLAYING） */
    Optional<QueueItem> findFirstByZoneIdAndStatus(Long zoneId, QueueStatus status);

    /** 上传冷却判定（2026-09-24 决议 D4）：查该用户在该域最近的上传条目 */
    Optional<QueueItem> findFirstByZoneIdAndRequesterIdOrderByCreatedAtDescIdDesc(
            Long zoneId, Long requesterId);

    /** 用户上传总数 / 被点赞统计（个人主页与战报） */
    long countByRequesterId(Long requesterId);

    long countByZoneIdAndRequesterId(Long zoneId, Long requesterId);

    @Query("select coalesce(sum(q.likes), 0) from QueueItem q where q.requester.id = :userId")
    long sumLikesByRequesterId(Long userId);

    List<QueueItem> findByRequesterIdOrderByCreatedAtDescIdDesc(Long userId);

    List<QueueItem> findByZoneIdAndStatusIn(Long zoneId, List<QueueStatus> statuses);

    List<QueueItem> findByStartedAtGreaterThanEqualAndStartedAtLessThan(
            LocalDateTime start, LocalDateTime end);

    boolean existsByZoneIdAndTrackIdAndRequesterId(Long zoneId, Long trackId, Long requesterId);

    List<QueueItem> findByZoneIdAndRequesterIdOrderByCreatedAtAscIdAsc(
            Long zoneId, Long requesterId);
}
