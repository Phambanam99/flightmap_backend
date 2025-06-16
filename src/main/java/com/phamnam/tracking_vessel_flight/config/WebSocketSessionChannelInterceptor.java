package com.phamnam.tracking_vessel_flight.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class WebSocketSessionChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Tạo Principal từ sessionId khi client kết nối
            String sessionId = accessor.getSessionId();
            accessor.setUser(new Principal() {
                @Override
                public String getName() {
                    return sessionId;
                }

                @Override
                public String toString() {
                    return getName();
                }
            });
        }

        return message;
    }
}