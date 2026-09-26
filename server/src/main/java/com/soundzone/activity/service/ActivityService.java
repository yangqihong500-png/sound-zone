package com.soundzone.activity.service;

import com.soundzone.activity.entity.ActivityEvent;
import com.soundzone.activity.repository.ActivityEventRepository;
import com.soundzone.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

/** 仅记录业务必要字段；不采集图片内容、密码、令牌、设备标识。 */
@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityEventRepository events;
    private final UserRepository users;
    private final Clock clock;

    @Transactional
    public void record(Long userId, Long zoneId, Long itemId, String type, long seconds) {
        ActivityEvent e = new ActivityEvent();
        e.setUser(users.getReferenceById(userId));
        e.setZoneId(zoneId);
        e.setItemId(itemId);
        e.setType(type);
        e.setDurationSeconds(seconds);
        e.setCreatedAt(LocalDateTime.now(clock));
        events.save(e);
    }

    @Transactional
    public void visit(Long userId) {
        if (!events.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                userId, "VISIT", LocalDate.now(clock).atStartOfDay()))
            record(userId, null, null, "VISIT", 0);
    }
}
