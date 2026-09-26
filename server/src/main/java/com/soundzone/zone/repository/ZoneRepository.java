package com.soundzone.zone.repository;

import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.entity.ZoneStatus;
import com.soundzone.zone.entity.ZoneVisibility;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    /** 首页/发现页：只推活跃的公开域（docs/02 第 2 步 + 2026-09-24 决议 D2） */
    List<Zone> findByStatusAndVisibilityOrderByListenerCountDesc(
            ZoneStatus status, ZoneVisibility visibility);

    /** 按归一化场景标签筛选活跃公开域 */
    List<Zone> findByStatusAndVisibilityAndSceneOrderByListenerCountDesc(
            ZoneStatus status, ZoneVisibility visibility, String scene);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select z from Zone z where z.id = :id")
    Optional<Zone> lockById(Long id);

    List<Zone> findByStatus(ZoneStatus status);

    List<Zone> findByHostIdOrderByCreatedAtDesc(Long hostId);

    Optional<Zone> findFirstByHostIdAndDemoResidentTrue(Long hostId);
}
