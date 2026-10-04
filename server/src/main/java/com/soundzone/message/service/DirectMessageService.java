package com.soundzone.message.service;

import com.soundzone.common.*;
import com.soundzone.message.dto.*;
import com.soundzone.message.entity.DirectMessage;
import com.soundzone.message.repository.DirectMessageRepository;
import com.soundzone.notification.service.NotificationService;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DirectMessageService {
    private static final int HISTORY_LIMIT = 100;

    private final DirectMessageRepository messages;
    private final UserRepository users;
    private final FollowRepository follows;
    private final ApplicationEventPublisher events;
    private final NotificationService notifications;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<DirectMessageDTO> conversation(Long viewerId, Long otherId) {
        validatePeer(viewerId, otherId);
        boolean connected = connected(viewerId, otherId);
        boolean hasHistory = messages.countConversation(viewerId, otherId) > 0;
        if (!connected && !hasHistory)
            throw new BizException(ResultCode.FORBIDDEN, "关注对方后才能发起私信");

        List<DirectMessageDTO> result =
                new ArrayList<>(
                        messages.findConversation(
                                        viewerId, otherId, PageRequest.of(0, HISTORY_LIMIT))
                                .stream()
                                .map(DirectMessageDTO::from)
                                .toList());
        Collections.reverse(result);
        return result;
    }

    public DirectMessageDTO send(Long senderId, Long recipientId, DirectMessageRequest request) {
        User sender = validatePeer(senderId, recipientId);
        User recipient =
                users.findById(recipientId)
                        .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        if (!connected(senderId, recipientId))
            throw new BizException(ResultCode.FORBIDDEN, "双方存在关注关系后才能发送私信");

        String body = request.body() == null ? "" : request.body().trim();
        if (body.isEmpty() || body.length() > 500)
            throw new BizException(ResultCode.PARAM_INVALID, "消息需为 1 至 500 字");

        DirectMessage message = new DirectMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setBody(body);
        message.setCreatedAt(LocalDateTime.now(clock));
        DirectMessage saved = messages.save(message);
        notifications.directMessage(saved);
        events.publishEvent(new DirectMessageEvent(saved.getId(), senderId, recipientId));
        return DirectMessageDTO.from(saved);
    }

    private User validatePeer(Long viewerId, Long otherId) {
        if (viewerId.equals(otherId))
            throw new BizException(ResultCode.PARAM_INVALID, "不能给自己发送私信");
        User viewer =
                users.findById(viewerId)
                        .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        if (!users.existsById(otherId)) throw new BizException(ResultCode.USER_NOT_FOUND);
        return viewer;
    }

    private boolean connected(Long first, Long second) {
        return follows.findByFollowerIdAndFolloweeId(first, second).isPresent()
                || follows.findByFollowerIdAndFolloweeId(second, first).isPresent();
    }
}
