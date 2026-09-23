package com.soundzone.zone.repository;

import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.entity.ZoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    /** 首页/发现页：只推活跃域（docs/02 第 2 步，规避空房间的第一道闸门） */
    List<Zone> findByStatusOrderByListenerCountDesc(ZoneStatus status);

    /** 按归一化场景标签筛选活跃域 */
    List<Zone> findByStatusAndSceneOrderByListenerCountDesc(ZoneStatus status, String scene);
}
