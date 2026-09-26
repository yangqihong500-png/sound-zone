package com.soundzone.zone.service;

import com.soundzone.common.*;
import com.soundzone.zone.entity.*;
import com.soundzone.zone.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ZoneAccess {
    private final ZoneRepository zones;
    private final ZoneMemberRepository members;

    public Zone lock(Long zoneId) {
        return zones.lockById(zoneId)
                .orElseThrow(() -> new BizException(ResultCode.ZONE_NOT_FOUND));
    }

    public Zone active(Long zoneId) {
        Zone z =
                zones.findById(zoneId)
                        .orElseThrow(() -> new BizException(ResultCode.ZONE_NOT_FOUND));
        active(z);
        return z;
    }

    public void active(Zone z) {
        if (z.getStatus() != ZoneStatus.ACTIVE)
            throw new BizException(ResultCode.ZONE_ALREADY_ENDED);
    }

    @Transactional(readOnly = true)
    public Zone member(Long zoneId, Long userId) {
        Zone z = active(zoneId);
        if (!members.existsByZoneIdAndUserId(zoneId, userId))
            throw new BizException(ResultCode.FORBIDDEN, "请先进入该域");
        return z;
    }

    public Zone host(Long zoneId, Long userId) {
        Zone z = member(zoneId, userId);
        if (!z.getHost().getId().equals(userId))
            throw new BizException(ResultCode.NOT_RESOURCE_OWNER);
        return z;
    }
}
