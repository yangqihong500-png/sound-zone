package com.soundzone.notification.service;

import com.soundzone.common.*;
import com.soundzone.message.entity.DirectMessage;
import com.soundzone.notification.dto.NotificationDTO;
import com.soundzone.notification.entity.*;
import com.soundzone.notification.repository.NotificationRepository;
import com.soundzone.queue.entity.QueueItem;
import com.soundzone.user.entity.User;
import com.soundzone.user.repository.*;
import com.soundzone.zone.entity.Zone;
import com.soundzone.zone.service.ZoneAccess;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final FollowRepository follows;
    private final ZoneAccess access;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<NotificationDTO> list(Long recipientId) {
        users.findById(recipientId).orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        return notifications.findTop100ByRecipientIdOrderByUpdatedAtDescIdDesc(recipientId).stream()
                .map(NotificationDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long recipientId) {
        return notifications.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    public void markRead(Long recipientId, Long notificationId) {
        Notification notification =
                notifications
                        .findByIdAndRecipientId(notificationId, recipientId)
                        .orElseThrow(() -> new BizException(ResultCode.NOT_RESOURCE_OWNER));
        if (notification.getReadAt() == null) {
            notification.setReadAt(now());
        }
    }

    public void markAllRead(Long recipientId) {
        notifications.markAllRead(recipientId, now());
    }

    public NotificationDTO invite(Long zoneId, Long senderId, Long recipientId) {
        Zone zone = access.host(zoneId, senderId);
        if (senderId.equals(recipientId))
            throw new BizException(ResultCode.PARAM_INVALID, "不能邀请自己");
        User recipient =
                users.findById(recipientId)
                        .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));
        if (follows.findByFollowerIdAndFolloweeId(senderId, recipientId).isEmpty())
            throw new BizException(ResultCode.FORBIDDEN, "只能邀请你已关注的用户");
        User sender = zone.getHost();
        Notification notification =
                notifications
                        .findFirstByRecipientIdAndZoneIdAndTypeAndReadAtIsNullOrderByUpdatedAtDescIdDesc(
                                recipientId, zoneId, NotificationType.ZONE_INVITE)
                        .orElseGet(() -> fresh(recipient, NotificationType.ZONE_INVITE));
        notification.setActor(sender);
        notification.setZone(zone);
        notification.setTitle(sender.getName() + " 邀请你一起听歌");
        notification.setBody("进入「" + zone.getName() + "」开始实时共听");
        touch(notification);
        return NotificationDTO.from(notifications.save(notification));
    }

    public void directMessage(DirectMessage message) {
        User recipient = message.getRecipient();
        User sender = message.getSender();
        Notification notification =
                notifications
                        .findFirstByRecipientIdAndActorIdAndTypeAndReadAtIsNullOrderByUpdatedAtDescIdDesc(
                                recipient.getId(), sender.getId(), NotificationType.DIRECT_MESSAGE)
                        .orElseGet(() -> fresh(recipient, NotificationType.DIRECT_MESSAGE));
        notification.setActor(sender);
        notification.setMessageId(message.getId());
        notification.setTitle(sender.getName() + " 发来私信");
        notification.setBody(abbreviate(message.getBody(), 80));
        touch(notification);
        notifications.save(notification);
    }

    public void trackStarted(QueueItem item) {
        User recipient = item.getRequester();
        Notification notification =
                notifications
                        .findFirstByRecipientIdAndQueueItemIdAndTypeAndReadAtIsNullOrderByUpdatedAtDescIdDesc(
                                recipient.getId(), item.getId(), NotificationType.TRACK_STARTED)
                        .orElseGet(() -> fresh(recipient, NotificationType.TRACK_STARTED));
        notification.setZone(item.getZone());
        notification.setQueueItem(item);
        notification.setTitle("你上传的歌曲开始播放了");
        notification.setBody("「" + item.getTrack().getTitle() + "」正在「" + item.getZone().getName() + "」播放");
        touch(notification);
        notifications.save(notification);
    }

    public void trackLiked(QueueItem item, User actor) {
        User recipient = item.getRequester();
        if (recipient.getId().equals(actor.getId())) return;
        Notification notification =
                notifications
                        .findFirstByRecipientIdAndQueueItemIdAndTypeAndReadAtIsNullOrderByUpdatedAtDescIdDesc(
                                recipient.getId(), item.getId(), NotificationType.TRACK_LIKE)
                        .orElseGet(() -> fresh(recipient, NotificationType.TRACK_LIKE));
        notification.setActor(actor);
        notification.setZone(item.getZone());
        notification.setQueueItem(item);
        notification.setTitle("你的歌曲收到了新点赞");
        notification.setBody(actor.getName() + " 点赞了「" + item.getTrack().getTitle() + "」");
        touch(notification);
        notifications.save(notification);
    }

    private Notification fresh(User recipient, NotificationType type) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setEventCount(0);
        notification.setCreatedAt(now());
        notification.setUpdatedAt(notification.getCreatedAt());
        return notification;
    }

    private void touch(Notification notification) {
        notification.setEventCount(notification.getEventCount() + 1);
        notification.setUpdatedAt(now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String abbreviate(String value, int limit) {
        String text = value == null ? "" : value.trim();
        return text.length() <= limit ? text : text.substring(0, limit - 1) + "…";
    }
}
