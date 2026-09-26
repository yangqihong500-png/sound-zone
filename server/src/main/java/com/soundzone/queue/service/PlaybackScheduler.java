package com.soundzone.queue.service;

import com.soundzone.zone.entity.ZoneStatus;
import com.soundzone.zone.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "soundzone.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PlaybackScheduler {
    private final ZoneRepository zones;
    private final PlaybackService playback;

    @Scheduled(fixedDelayString = "${soundzone.tick-millis:1000}")
    public void tick() {
        for (var zone : zones.findByStatus(ZoneStatus.ACTIVE)) {
            try {
                playback.tick(zone.getId());
            } catch (Exception e) {
                log.warn("域 {} 推进失败，下一轮重试", zone.getId(), e);
            }
        }
    }
}
