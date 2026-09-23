package com.soundzone.moment.repository;

import com.soundzone.moment.entity.Moment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MomentRepository extends JpaRepository<Moment, Long> {

    /** 碎片墙：按发布时间倒序 */
    List<Moment> findByZoneIdOrderByCreatedAtDesc(Long zoneId);

    long countByUserId(Long userId);
}
