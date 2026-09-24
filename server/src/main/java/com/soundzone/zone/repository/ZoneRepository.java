package com.soundzone.zone.repository;

import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.entity.ZoneStatus;
import com.soundzone.zone.entity.ZoneVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    /** 首页/发现页：只推活跃的公开域（docs/02 第 2 步 + 2026-09-24 决议 D2） */
    List<Zone> findByStatusAndVisibilityOrderByListenerCountDesc(ZoneStatus status, ZoneVisibility visibility);

    /** 按归一化场景标签筛选活跃公开域 */
    List<Zone> findByStatusAndVisibilityAndSceneOrderByListenerCountDesc(ZoneStatus status, ZoneVisibility visibility, String scene);
}
