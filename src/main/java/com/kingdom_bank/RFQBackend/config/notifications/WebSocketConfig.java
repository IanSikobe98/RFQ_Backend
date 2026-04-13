package com.kingdom_bank.RFQBackend.config.notifications;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    @PostConstruct
    public void init() {
        System.out.println("🔥🔥 WebSocketConfig LOADED");
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register SockJS endpoints
        System.out.println("🔥 REGISTERING WEBSOCKETS");
        System.out.println("Handler = " + notificationWebSocketHandler);
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Register plain WebSocket endpoint (without SockJS)
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications-direct")
                .setAllowedOriginPatterns("*");
    }
}