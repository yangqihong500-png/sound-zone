package com.soundzone.moment.repository;

import com.soundzone.moment.entity.Moment;
import com.soundzone.moment.entity.MomentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MomentRepository extends JpaRepository<Moment, Long> {

    /** 主界面动态区：最新 1-2 张（排除已撤回，决议 D6） */
    List<Moment> findTop2ByZoneIdAndStatusOrderByCreatedAtDesc(Long zoneId, MomentStatus status);

    /** 动态详情页：半小时内图片流（时间倒序，决议 D6） */
    List<Moment> findByZoneIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(Long zoneId, MomentStatus status, LocalDateTime after);

    long countByUserId(Long userId);
}
