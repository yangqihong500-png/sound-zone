package com.soundzone.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundzone.auth.service.SessionService;
import com.soundzone.common.BizException;
import com.soundzone.message.service.DirectMessageEvent;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 私信页面的轻量实时通知；消息正文仍由鉴权 REST 接口读取。 */
@Component
@RequiredArgsConstructor
public class DirectMessageSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper json;
    private final SessionService sessions;
    private final Map<String, WebSocketSession> sockets = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession socket) {
        socket.setTextMessageSizeLimit(2048);
        socket.getAttributes().put("openedAt", System.currentTimeMillis());
        sockets.put(socket.getId(), socket);
    }

    @Override
    protected void handleTextMessage(WebSocketSession socket, TextMessage message)
            throws Exception {
        try {
            String token = json.readTree(message.getPayload()).path("token").asText();
            Long userId = sessions.authenticate(token);
            socket.getAttributes().put("userId", userId);
            socket.getAttributes().put("token", token);
            send(socket, Map.of("type", "READY"));
        } catch (Exception e) {
            send(
                    socket,
                    Map.of(
                            "type",
                            "ERROR",
                            "code",
                            e instanceof BizException b ? b.getResultCode().getCode() : 1001));
            socket.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void created(DirectMessageEvent event) {
        for (var socket : sockets.values()) {
            Object current = socket.getAttributes().get("userId");
            if (!event.senderId().equals(current) && !event.recipientId().equals(current)) continue;
            try {
                sessions.authenticate((String) socket.getAttributes().get("token"));
                send(
                        socket,
                        Map.of(
                                "type",
                                "MESSAGE",
                                "messageId",
                                event.messageId(),
                                "fromUserId",
                                event.senderId(),
                                "toUserId",
                                event.recipientId()));
            } catch (Exception e) {
                try {
                    socket.close(CloseStatus.POLICY_VIOLATION);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void removeUnauthenticated() {
        for (var socket : sockets.values())
            if (!socket.getAttributes().containsKey("userId")
                    && System.currentTimeMillis()
                                    - (long) socket.getAttributes().get("openedAt")
                            > 15000)
                try {
                    socket.close(CloseStatus.POLICY_VIOLATION);
                } catch (Exception ignored) {
                }
    }

    private void send(WebSocketSession socket, Object value) throws Exception {
        synchronized (socket) {
            if (socket.isOpen())
                socket.sendMessage(new TextMessage(json.writeValueAsString(value)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession socket, CloseStatus status) {
        sockets.remove(socket.getId());
    }
}
