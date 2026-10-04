package com.soundzone.user.service;

import com.soundzone.activity.repository.ActivityEventRepository;
import com.soundzone.activity.service.ActivityService;
import com.soundzone.auth.repository.UserCredentialRepository;
import com.soundzone.common.*;
import com.soundzone.feedback.repository.TrackCollectionRepository;
import com.soundzone.moment.service.ImageStorage;
import com.soundzone.moment.entity.MomentStatus;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.dto.QueueItemDTO;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.dto.TrackDTO;
import com.soundzone.user.dto.ListeningSummaryDTO;
import com.soundzone.user.dto.UserProfileDTO;
import com.soundzone.user.entity.*;
import com.soundzone.user.repository.*;
import com.soundzone.zone.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository users;
    private final UserCredentialRepository credentials;
    private final QueueItemRepository queue;
    private final MomentRepository moments;
    private final FollowRepository follows;
    private final TrackCollectionRepository collections;
    private final ZoneRepository zones;
    private final ActivityService activity;
    private final ActivityEventRepository activityEvents;
    private final Clock clock;
    private final ImageStorage imageStorage;

    public UserProfileDTO getProfile(Long id, Long viewer) {
        var u = users.findById(id).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        return new UserProfileDTO(
                u.getId(),
                u.getName(),
                u.getAvatarColor(),
                u.getAvatarUrl(),
                u.getCoverUrl(),
                new UserProfileDTO.Stats(
                        queue.countByRequesterId(id),
                        queue.sumLikesByRequesterId(id),
                        moments.countByUserIdAndStatus(id, MomentStatus.NORMAL),
                        follows.countByFollowerId(id)),
                follows.findByFollowerIdAndFolloweeId(viewer, id).isPresent());
    }

    /** 修改公开用户 ID；独立账号同步更新登录名，避免展示名与登录名分叉。 */
    public Map<String, String> updateName(Long id, String requestedName) {
        String name = requestedName.strip();
        User user = users.lockById(id).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        users.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new BizException(ResultCode.PARAM_INVALID, "该用户 ID 已被使用"); });

        credentials.findByUserId(id)
                .ifPresent(
                        credential -> {
                            String loginName = name.toLowerCase(Locale.ROOT);
                            credentials.findByLoginName(loginName)
                                    .filter(existing -> !existing.getUser().getId().equals(id))
                                    .ifPresent(existing -> { throw new BizException(ResultCode.PARAM_INVALID, "该用户 ID 已被使用"); });
                            credential.setLoginName(loginName);
                            credentials.save(credential);
                        });
        user.setName(name);
        users.saveAndFlush(user);
        return Map.of("name", name);
    }

    public Map<String, String> updateAvatar(Long id, MultipartFile file) {
        User user = users.lockById(id).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        String oldUrl = user.getAvatarUrl();
        String newUrl = "/profile-avatars/" + imageStorage.storeProfileAvatar(file);
        user.setAvatarUrl(newUrl);
        users.saveAndFlush(user);
        if (oldUrl != null && oldUrl.startsWith("/profile-avatars/")) {
            String oldKey = oldUrl.substring("/profile-avatars/".length());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { imageStorage.deleteProfileAvatar(oldKey); }
            });
        }
        return Map.of("avatarUrl", newUrl);
    }

    public Map<String, String> updateCover(Long id, MultipartFile file) {
        User user = users.lockById(id).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        String oldUrl = user.getCoverUrl();
        String newUrl = "/profile-covers/" + imageStorage.storeProfileCover(file);
        user.setCoverUrl(newUrl);
        users.saveAndFlush(user);
        if (oldUrl != null && oldUrl.startsWith("/profile-covers/")) {
            String oldKey = oldUrl.substring("/profile-covers/".length());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { imageStorage.deleteProfileCover(oldKey); }
            });
        }
        return Map.of("coverUrl", newUrl);
    }

    /** 汇总本人已由服务端心跳确认的共听时间；暂停、失联和歌曲不一致的区间不会进入统计。 */
    public ListeningSummaryDTO listeningSummary(Long userId) {
        users.findById(userId).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDay = today.minusDays(6);
        var recent =
                activityEvents.findByUserIdAndTypeAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                        userId, "LISTEN", firstDay.atStartOfDay());

        LinkedHashMap<LocalDate, Long> daily = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) daily.put(firstDay.plusDays(i), 0L);
        Map<Long, Long> zoneSeconds = new HashMap<>();
        for (var event : recent) {
            long seconds = Math.max(0, event.getDurationSeconds());
            LocalDate day = event.getCreatedAt().toLocalDate();
            if (daily.containsKey(day)) daily.merge(day, seconds, Long::sum);
            if (event.getZoneId() != null) zoneSeconds.merge(event.getZoneId(), seconds, Long::sum);
        }

        List<Long> topZoneIds =
                zoneSeconds.entrySet().stream()
                        .sorted(
                                Map.Entry.<Long, Long>comparingByValue()
                                        .reversed()
                                        .thenComparing(Map.Entry.comparingByKey()))
                        .limit(3)
                        .map(Map.Entry::getKey)
                        .toList();
        Map<Long, com.soundzone.zone.entity.Zone> zoneById =
                zones.findAllById(topZoneIds).stream()
                        .collect(
                                Collectors.toMap(
                                        com.soundzone.zone.entity.Zone::getId, Function.identity()));
        List<ListeningSummaryDTO.TopZone> topZones =
                topZoneIds.stream()
                        .filter(zoneById::containsKey)
                        .map(
                                id ->
                                        new ListeningSummaryDTO.TopZone(
                                                id, zoneById.get(id).getName(), zoneSeconds.get(id)))
                        .toList();
        List<ListeningSummaryDTO.DailyListening> days =
                daily.entrySet().stream()
                        .map(
                                entry ->
                                        new ListeningSummaryDTO.DailyListening(
                                                entry.getKey().toString(), entry.getValue()))
                        .toList();
        long last7DaysSeconds = daily.values().stream().mapToLong(Long::longValue).sum();
        return new ListeningSummaryDTO(
                activityEvents.sumListeningSeconds(userId),
                daily.getOrDefault(today, 0L),
                last7DaysSeconds,
                days,
                topZones);
    }

    public void follow(Long from, Long to) {
        if (from.equals(to)) throw new BizException(ResultCode.PARAM_INVALID, "不能关注自己");
        User user =
                users.lockById(from).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        User target =
                users.findById(to).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        if (follows.findByFollowerIdAndFolloweeId(from, to).isPresent()) return;
        Follow f = new Follow();
        f.setFollower(user);
        f.setFollowee(target);
        follows.save(f);
        activity.record(from, null, null, "FOLLOW", 0);
    }

    public void unfollow(Long from, Long to) {
        users.lockById(from).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        follows.deleteByFollowerIdAndFolloweeId(from, to);
    }

    public List<Map<String, Object>> following(Long id) {
        return follows.findByFollowerIdOrderByCreatedAtDesc(id).stream()
                .map(
                        f ->
                                Map.<String, Object>of(
                                        "id",
                                        f.getFollowee().getId(),
                                        "name",
                                        f.getFollowee().getName(),
                                        "avatarColor",
                                        f.getFollowee().getAvatarColor(),
                                        "avatarUrl",
                                        Objects.toString(f.getFollowee().getAvatarUrl(), "")))
                .toList();
    }

    public List<TrackDTO> collections(Long id) {
        return collections.findByUserIdOrderByCreatedAtDesc(id).stream()
                .map(c -> TrackDTO.from(c.getTrack()))
                .toList();
    }

    public List<Map<String, Object>> uploads(Long id) {
        return queue.findByRequesterIdOrderByLikesDescCreatedAtDescIdDesc(id).stream()
                .map(
                        q ->
                                Map.<String, Object>of(
                                        "item",
                                        QueueItemDTO.from(q, null, false),
                                        "zoneId",
                                        q.getZone().getId(),
                                        "zoneName",
                                        q.getZone().getName(),
                                        "zoneStatus",
                                        q.getZone().getStatus().name()))
                .toList();
    }

    /** 我的域目前定义为本人创建，包含已结束历史；不对其他用户暴露私密域。 */
    public List<Map<String, Object>> zones(Long id) {
        return zones.findByHostIdOrderByCreatedAtDesc(id).stream()
                .map(
                        z ->
                                Map.<String, Object>of(
                                        "id",
                                        z.getId(),
                                        "name",
                                        z.getName(),
                                        "scene",
                                        z.getScene(),
                                        "visibility",
                                        z.getVisibility().name(),
                                        "status",
                                        z.getStatus().name()))
                .toList();
    }
}
