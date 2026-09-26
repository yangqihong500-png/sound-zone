package com.soundzone.user.service;

import com.soundzone.activity.service.ActivityService;
import com.soundzone.common.*;
import com.soundzone.feedback.repository.TrackCollectionRepository;
import com.soundzone.moment.entity.MomentStatus;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.dto.QueueItemDTO;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.track.dto.TrackDTO;
import com.soundzone.user.dto.UserProfileDTO;
import com.soundzone.user.entity.*;
import com.soundzone.user.repository.*;
import com.soundzone.zone.repository.ZoneRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository users;
    private final QueueItemRepository queue;
    private final MomentRepository moments;
    private final FollowRepository follows;
    private final TrackCollectionRepository collections;
    private final ZoneRepository zones;
    private final ActivityService activity;

    public UserProfileDTO getProfile(Long id, Long viewer) {
        var u = users.findById(id).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        return new UserProfileDTO(
                u.getId(),
                u.getName(),
                u.getAvatarColor(),
                new UserProfileDTO.Stats(
                        queue.countByRequesterId(id),
                        queue.sumLikesByRequesterId(id),
                        moments.countByUserIdAndStatus(id, MomentStatus.NORMAL),
                        follows.countByFollowerId(id)),
                follows.findByFollowerIdAndFolloweeId(viewer, id).isPresent());
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
                                        f.getFollowee().getAvatarColor()))
                .toList();
    }

    public List<TrackDTO> collections(Long id) {
        return collections.findByUserIdOrderByCreatedAtDesc(id).stream()
                .map(c -> TrackDTO.from(c.getTrack()))
                .toList();
    }

    public List<Map<String, Object>> uploads(Long id) {
        return queue.findByRequesterIdOrderByCreatedAtDescIdDesc(id).stream()
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
