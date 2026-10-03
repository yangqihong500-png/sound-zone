package com.soundzone.zone.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.common.Times;
import com.soundzone.queue.entity.QueueStatus;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.entity.Track;
import com.soundzone.track.service.TagCatalog;
import com.soundzone.zone.dto.PomodoroStateDTO;
import com.soundzone.zone.dto.ZoneCreateRequest;
import com.soundzone.zone.entity.PeriodType;
import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.entity.ZonePeriod;
import com.soundzone.zone.repository.ZonePeriodRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 域级共享番茄钟：负责配置校验、服务端状态计算及待播/预存队列切换。 */
@Service
@RequiredArgsConstructor
public class PomodoroService {
    private static final List<Step> CLASSIC =
            List.of(
                    new Step(25, PeriodType.FOCUS),
                    new Step(5, PeriodType.BREAK),
                    new Step(25, PeriodType.FOCUS),
                    new Step(5, PeriodType.BREAK),
                    new Step(25, PeriodType.FOCUS),
                    new Step(5, PeriodType.BREAK),
                    new Step(25, PeriodType.FOCUS),
                    new Step(15, PeriodType.BREAK));
    private static final List<Step> DEEP =
            List.of(
                    new Step(40, PeriodType.FOCUS),
                    new Step(10, PeriodType.BREAK),
                    new Step(40, PeriodType.FOCUS),
                    new Step(10, PeriodType.BREAK),
                    new Step(40, PeriodType.FOCUS),
                    new Step(20, PeriodType.BREAK));

    private final ZonePeriodRepository periods;
    private final QueueItemRepository queue;
    private final Clock clock;

    public void configure(Zone zone, List<ZoneCreateRequest.PeriodConfig> request) {
        if (request == null || request.isEmpty()) return;
        String preset = detectAndValidate(request);
        zone.setPomodoroPreset(preset);
        zone.setPomodoroStartedAt(LocalDateTime.now(clock));
        zone.setPomodoroPeriodIndex(0);
        List<ZonePeriod> entities = new ArrayList<>();
        for (ZoneCreateRequest.PeriodConfig config : request) {
            ZonePeriod period = new ZonePeriod();
            period.setZone(zone);
            period.setOrderIndex(config.orderIndex());
            period.setDurationMin(config.durationMin());
            period.setType(PeriodType.valueOf(config.type().toUpperCase(Locale.ROOT)));
            if (config.allowedTags() != null) period.getAllowedTags().addAll(config.allowedTags());
            entities.add(period);
        }
        periods.saveAllAndFlush(entities);
    }

    public PomodoroStateDTO state(Zone zone) {
        List<ZonePeriod> schedule = periods.findByZoneIdOrderByOrderIndexAsc(zone.getId());
        if (schedule.isEmpty()) return PomodoroStateDTO.disabled();
        Active active = active(zone, schedule);
        int focusRounds = (int) schedule.stream().filter(p -> p.getType() == PeriodType.FOCUS).count();
        int focusRound = 0;
        for (int i = 0; i <= active.index(); i++)
            if (schedule.get(i).getType() == PeriodType.FOCUS) focusRound++;
        if (active.period().getType() == PeriodType.BREAK && focusRound == 0) focusRound = focusRounds;
        boolean musicByPhase = schedule.stream().anyMatch(p -> !p.getAllowedTags().isEmpty());
        return new PomodoroStateDTO(
                true,
                zone.getPomodoroPreset() == null ? "LEGACY" : zone.getPomodoroPreset(),
                active.period().getType().name(),
                active.remainingSeconds(),
                Times.millis(LocalDateTime.now(clock).plusSeconds(active.remainingSeconds())),
                active.index(),
                focusRound,
                focusRounds,
                active.period().getType() == PeriodType.BREAK
                        && active.index() == schedule.size() - 1,
                musicByPhase);
    }

    public boolean allowsNow(Zone zone, Track track) {
        List<ZonePeriod> schedule = periods.findByZoneIdOrderByOrderIndexAsc(zone.getId());
        return schedule.isEmpty() || matches(active(zone, schedule).period(), track);
    }

    /** 调用方已持有域锁；只改尚未播放条目，当前歌曲始终自然结束。 */
    public boolean reconcileLocked(Zone zone) {
        List<ZonePeriod> schedule = periods.findByZoneIdOrderByOrderIndexAsc(zone.getId());
        if (schedule.isEmpty()) return false;
        Active active = active(zone, schedule);
        ZonePeriod period = active.period();
        var waiting =
                queue.findByZoneIdAndStatusIn(
                        zone.getId(), List.of(QueueStatus.QUEUED, QueueStatus.PRESET));
        boolean changed = !java.util.Objects.equals(zone.getPomodoroPeriodIndex(), active.index());
        if (changed) zone.setPomodoroPeriodIndex(active.index());
        for (var item : waiting) {
            QueueStatus target = matches(period, item.getTrack()) ? QueueStatus.QUEUED : QueueStatus.PRESET;
            if (item.getStatus() != target) {
                item.setStatus(target);
                changed = true;
            }
        }
        if (changed) queue.saveAllAndFlush(waiting);
        return changed;
    }

    private boolean matches(ZonePeriod period, Track track) {
        Set<String> allowed = period.getAllowedTags();
        return allowed.isEmpty() || track.getTags().stream().anyMatch(allowed::contains);
    }

    private Active active(Zone zone, List<ZonePeriod> schedule) {
        long cycleSeconds =
                schedule.stream().mapToLong(p -> Math.max(1, p.getDurationMin()) * 60L).sum();
        LocalDateTime started =
                zone.getPomodoroStartedAt() == null ? zone.getCreatedAt() : zone.getPomodoroStartedAt();
        long elapsed = Math.max(0, Duration.between(started, LocalDateTime.now(clock)).getSeconds());
        long offset = Math.floorMod(elapsed, cycleSeconds);
        for (int i = 0; i < schedule.size(); i++) {
            ZonePeriod period = schedule.get(i);
            long duration = Math.max(1, period.getDurationMin()) * 60L;
            if (offset < duration) return new Active(period, i, duration - offset);
            offset -= duration;
        }
        ZonePeriod first = schedule.get(0);
        return new Active(first, 0, Math.max(1, first.getDurationMin()) * 60L);
    }

    private String detectAndValidate(List<ZoneCreateRequest.PeriodConfig> request) {
        if (request.size() > 12) throw invalid();
        if (request.stream().anyMatch(p -> p == null || p.orderIndex() == null)) throw invalid();
        List<ZoneCreateRequest.PeriodConfig> sorted =
                request.stream().sorted(Comparator.comparingInt(ZoneCreateRequest.PeriodConfig::orderIndex)).toList();
        for (int i = 0; i < sorted.size(); i++) {
            var period = sorted.get(i);
            if (period.orderIndex() != i || period.durationMin() == null || period.type() == null)
                throw invalid();
            PeriodType type;
            try {
                type = PeriodType.valueOf(period.type().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw invalid();
            }
            if ((i % 2 == 0 && type != PeriodType.FOCUS)
                    || (i % 2 == 1 && type != PeriodType.BREAK)) throw invalid();
            Set<String> tags = period.allowedTags() == null ? Set.of() : period.allowedTags();
            if (tags.size() > 24 || tags.stream().anyMatch(tag -> !TagCatalog.isFilterable(tag)))
                throw new BizException(ResultCode.PARAM_INVALID, "番茄钟只能选择音乐过滤目录内的标签");
        }
        if (matchesTemplate(sorted, CLASSIC)) return "CLASSIC";
        if (matchesTemplate(sorted, DEEP)) return "DEEP";
        throw new BizException(ResultCode.PARAM_INVALID, "请选择经典或深度专注预设");
    }

    private boolean matchesTemplate(List<ZoneCreateRequest.PeriodConfig> request, List<Step> template) {
        if (request.size() != template.size()) return false;
        for (int i = 0; i < template.size(); i++) {
            var actual = request.get(i);
            var expected = template.get(i);
            if (actual.durationMin() != expected.minutes()
                    || !actual.type().equalsIgnoreCase(expected.type().name())) return false;
        }
        return true;
    }

    private BizException invalid() {
        return new BizException(ResultCode.PARAM_INVALID, "番茄钟时段顺序无效");
    }

    private record Step(int minutes, PeriodType type) {}

    private record Active(ZonePeriod period, int index, long remainingSeconds) {}
}
