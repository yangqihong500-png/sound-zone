package com.soundzone.zone.repository;

import com.soundzone.zone.entity.ZoneMember;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ZoneMemberRepository extends JpaRepository<ZoneMember, Long> {

    Optional<ZoneMember> findByZoneIdAndUserId(Long zoneId, Long userId);

    /** 域内当前成员数（同频人数与"全员退出"判定） */
    long countByZoneId(Long zoneId);

    void deleteByZoneIdAndUserId(Long zoneId, Long userId);

    List<ZoneMember> findByZoneId(Long zoneId);

    boolean existsByZoneIdAndUserId(Long zoneId, Long userId);
}
