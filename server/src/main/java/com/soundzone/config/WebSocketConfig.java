package com.soundzone.config;

import com.soundzone.realtime.ZoneSocketHandler;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final ZoneSocketHandler handler;

    @Value("${soundzone.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private String[] origins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/zones").setAllowedOriginPatterns(origins);
    }
}
