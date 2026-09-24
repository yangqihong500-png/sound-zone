package com.soundzone.user.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.user.dto.UserProfileDTO;
import com.soundzone.user.entity.Follow;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.FollowRepository;
import com.soundzone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务（v2：功能型主页 + 关注体系）
 * v2 变更：歌品值取消（去游戏化）；新增 关注/取关/关注列表（决议 D7）
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final QueueItemRepository queueItemRepository;
    private final MomentRepository momentRepository;
    private final FollowRepository followRepository;

    /** 我的页信息（功能型主页：上传/获赞/分享/关注） */
    public UserProfileDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        long uploads = queueItemRepository.countByRequesterId(userId);
        long likes = queueItemRepository.sumLikesByRequesterId(userId);
        long moments = momentRepository.countByUserId(userId);
        long following = followRepository.countByFollowerId(userId);
        return new UserProfileDTO(user.getId(), user.getName(), user.getAvatarColor(),
                new UserProfileDTO.Stats(uploads, likes, moments, following));
    }

    /** 关注上传者（决议 D7：轻入口，幂等） */
    @Transactional
    public void follow(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new BizException(ResultCode.PARAM_INVALID, "不能关注自己");
        }
        User from = userRepository.findById(fromUserId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        User to = userRepository.findById(toUserId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        if (followRepository.findByFollowerIdAndFolloweeId(fromUserId, toUserId).isPresent()) {
            return; // 已关注，幂等返回
        }
        Follow follow = new Follow();
        follow.setFollower(from);
        follow.setFollowee(to);
        followRepository.save(follow);
    }

    /** 取消关注 */
    @Transactional
    public void unfollow(Long fromUserId, Long toUserId) {
        followRepository.deleteByFollowerIdAndFolloweeId(fromUserId, toUserId);
    }

    /** 我的关注列表（被关注者昵称） */
    public List<String> following(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return followRepository.findByFollowerIdOrderByCreatedAtDesc(userId)
                .stream().map(f -> f.getFollowee().getName()).toList();
    }
}
