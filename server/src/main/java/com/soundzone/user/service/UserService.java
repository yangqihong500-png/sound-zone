package com.soundzone.user.service;

import com.soundzone.common.BizException;
import com.soundzone.common.ResultCode;
import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.repository.QueueItemRepository;
import com.soundzone.user.dto.UserProfileDTO;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户服务：我的页（歌品值 + 行为统计）
 * 歌品值由 FeedbackService 在反馈事件后重算并冗余存储
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final QueueItemRepository queueItemRepository;
    private final MomentRepository momentRepository;

    /** 我的页信息（Docs/02 审美身份证） */
    public UserProfileDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        long requests = queueItemRepository.countByRequesterId(userId);
        long likes = queueItemRepository.sumLikesByRequesterId(userId);
        long moments = momentRepository.countByUserId(userId);
        return new UserProfileDTO(user.getId(), user.getName(), user.getAvatarColor(),
                user.getTasteScore(), new UserProfileDTO.Stats(requests, likes, moments));
    }
}
