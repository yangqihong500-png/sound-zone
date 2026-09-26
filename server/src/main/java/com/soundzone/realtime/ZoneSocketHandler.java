package com.soundzone.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundzone.auth.service.SessionService;
import com.soundzone.common.BizException;
import com.soundzone.zone.service.ZoneAccess;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Bearer 凭证放首条消息，避免出现在 URL／访问日志中；推送只发给已准入成员。 */
@Component
@RequiredArgsConstructor
public class ZoneSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper json;
    private final SessionService sessions;
    private final ZoneAccess access;
    private final Map<String, WebSocketSession> sockets = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession socket) {
        socket.setTextMessageSizeLimit(4096);
        socket.getAttributes().put("openedAt", System.currentTimeMillis());
        sockets.put(socket.getId(), socket);
    }

    @Override
    protected void handleTextMessage(WebSocketSession socket, TextMessage message)
            throws Exception {
        try {
            var node = json.readTree(message.getPayload());
            String token = node.path("token").asText();
            Long userId = sessions.authenticate(token);
            long zoneId = node.path("zoneId").asLong();
            access.member(zoneId, userId);
            socket.getAttributes().put("userId", userId);
            socket.getAttributes().put("zoneId", zoneId);
            socket.getAttributes().put("token", token);
            send(socket, Map.of("type", "READY", "zoneId", zoneId));
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
    public void changed(ZoneEvent event) {
        for (var socket : sockets.values()) {
            var attrs = socket.getAttributes();
            if (!event.zoneId().equals(attrs.get("zoneId"))) continue;
            if (event.toUserId() != null && !event.toUserId().equals(attrs.get("userId"))) continue;
            try {
                sessions.authenticate((String) attrs.get("token"));
                if (!"ENDED".equals(event.type()))
                    access.member(event.zoneId(), (Long) attrs.get("userId"));
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", event.type());
                payload.put("zoneId", event.zoneId());
                if (event.itemId() != null) payload.put("itemId", event.itemId());
                send(socket, payload);
                if ("ENDED".equals(event.type())) socket.close(CloseStatus.NORMAL);
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
                    && System.currentTimeMillis() - (long) socket.getAttributes().get("openedAt")
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
