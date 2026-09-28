package com.soundzone.message.controller;

import com.soundzone.auth.service.CurrentUser;
import com.soundzone.common.Result;
import com.soundzone.message.dto.DirectMessageRequest;
import com.soundzone.message.service.DirectMessageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages/users")
@RequiredArgsConstructor
public class DirectMessageController {
    private final DirectMessageService messages;
    private final CurrentUser current;

    @GetMapping("/{userId}")
    public Result<?> conversation(@PathVariable Long userId) {
        return Result.ok(messages.conversation(current.id(), userId));
    }

    @PostMapping("/{userId}")
    public Result<?> send(
            @PathVariable Long userId, @Valid @RequestBody DirectMessageRequest request) {
        return Result.ok(messages.send(current.id(), userId, request));
    }
}
