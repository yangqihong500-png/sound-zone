package com.soundzone.activity.service;

import com.soundzone.activity.entity.ActivityEvent;
import com.soundzone.activity.repository.ActivityEventRepository;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.repository.QueueItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricsService {
    private final ActivityEventRepository events;
    private final QueueItemRepository queue;
    private final MomentRepository moments;
    private final Clock clock;

    public Map<String, Object> daily(LocalDate day) {
        var rows =
                events.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        var next =
                events.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        day.plusDays(1).atStartOfDay(), day.plusDays(2).atStartOfDay());
        Set<Long> active = rows.stream().map(e -> e.getUser().getId()).collect(Collectors.toSet());
        Set<Long> tomorrow =
                next.stream().map(e -> e.getUser().getId()).collect(Collectors.toSet());
        Set<Long> uploaders =
                rows.stream()
                        .filter(e -> "UPLOAD".equals(e.getType()))
                        .map(e -> e.getUser().getId())
                        .collect(Collectors.toSet());
        var uploaded =
                rows.stream()
                        .filter(e -> "UPLOAD".equals(e.getType()))
                        .map(ActivityEvent::getItemId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        long withImage = uploaded.stream().filter(moments::existsByQueueItemId).count();
        Set<Long> collected =
                rows.stream()
                        .filter(e -> "COLLECT".equals(e.getType()))
                        .map(ActivityEvent::getItemId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        var played =
                queue.findByStartedAtGreaterThanEqualAndStartedAtLessThan(
                        day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        long collectedPlays = played.stream().filter(q -> collected.contains(q.getId())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", day.toString());
        result.put("activeUsers", active.size());
        result.put(
                "listeningSeconds",
                rows.stream().mapToLong(ActivityEvent::getDurationSeconds).sum());
        result.put("uploadParticipationRate", rate(uploaders.size(), active.size()));
        result.put("imageShareRate", rate(withImage, uploaded.size()));
        result.put("collectionRate", rate(collectedPlays, played.size()));
        result.put(
                "nextDayRetention",
                day.plusDays(2).isAfter(LocalDate.now(clock))
                        ? null
                        : rate(active.stream().filter(tomorrow::contains).count(), active.size()));
        return result;
    }

    private Double rate(long count, long total) {
        return total == 0 ? null : (double) count / total;
    }
}
