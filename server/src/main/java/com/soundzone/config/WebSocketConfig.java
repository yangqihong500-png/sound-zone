package com.soundzone.config;

import com.soundzone.realtime.*;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final ZoneSocketHandler zoneHandler;
    private final DirectMessageSocketHandler messageHandler;

    @Value("${soundzone.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private String[] origins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(zoneHandler, "/ws/zones").setAllowedOriginPatterns(origins);
        registry.addHandler(messageHandler, "/ws/messages").setAllowedOriginPatterns(origins);
    }
}
