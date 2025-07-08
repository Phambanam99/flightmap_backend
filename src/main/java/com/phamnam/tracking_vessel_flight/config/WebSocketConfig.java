package com.phamnam.tracking_vessel_flight.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker for broadcasting
        config.enableSimpleBroker(
                "/topic", // For broadcasting to all subscribers
                "/queue", // For personal messages
                "/user" // For user-specific messages
        );

        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");

        // User destination prefix for personal messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure CORS
                .withSockJS()
                .setHeartbeatTime(25000); // Send heartbeat every 25 seconds

        // Alternative endpoint without SockJS
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Configure transport settings
        registration.setMessageSizeLimit(64 * 1024); // 64KB max message size
        registration.setSendBufferSizeLimit(512 * 1024); // 512KB send buffer
        registration.setSendTimeLimit(20 * 1000); // 20 seconds send timeout
    }
}