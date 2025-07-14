package com.phamnam.tracking_vessel_flight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketSessionChannelInterceptor channelInterceptor;

    public WebSocketConfig(WebSocketSessionChannelInterceptor channelInterceptor) {
        this.channelInterceptor = channelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.info("Configuring WebSocket message broker");

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

        log.info("WebSocket message broker configured successfully");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registering STOMP endpoints");

        // Register WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure CORS
                .withSockJS()
                .setHeartbeatTime(25000); // Send heartbeat every 25 seconds

        // Alternative endpoint without SockJS
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");

        log.info("STOMP endpoints registered successfully");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        log.info("Configuring WebSocket transport");

        // Configure transport settings
        registration.setMessageSizeLimit(64 * 1024); // 64KB max message size
        registration.setSendBufferSizeLimit(512 * 1024); // 512KB send buffer
        registration.setSendTimeLimit(20 * 1000); // 20 seconds send timeout

        log.info("WebSocket transport configured successfully");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        log.info("Configuring client inbound channel with interceptor");
        registration.interceptors(channelInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        log.info("Configuring client outbound channel with interceptor");
        registration.interceptors(channelInterceptor);
    }

    @Bean
    public SimpUserRegistry simpUserRegistry() {
        return new DefaultSimpUserRegistry();
    }
}