package com.soundzone.zone.repository;

import com.soundzone.zone.entity.ZonePeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZonePeriodRepository extends JpaRepository<ZonePeriod, Long> {

    /** 按轮转顺序读取某域的番茄钟时段配置 */
    List<ZonePeriod> findByZoneIdOrderByOrderIndexAsc(Long zoneId);
}
