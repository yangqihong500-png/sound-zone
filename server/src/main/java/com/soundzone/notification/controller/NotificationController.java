package com.soundzone.notification.controller;

import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.Result;
import com.soundzone.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notifications;
    private final CurrentUser current;

    @GetMapping
    public Result<?> list() {
        return Result.ok(notifications.list(current.id()));
    }

    @GetMapping("/unread-count")
    public Result<?> unreadCount() {
        return Result.ok(Map.of("count", notifications.unreadCount(current.id())));
    }

    @PutMapping("/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        notifications.markRead(current.id(), id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<?> markAllRead() {
        notifications.markAllRead(current.id());
        return Result.ok();
    }
}
