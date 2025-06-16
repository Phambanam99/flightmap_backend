package com.phamnam.tracking_vessel_flight.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;

@Component
@Slf4j
public class WebSocketSessionChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            String sessionId = accessor.getSessionId();
            StompCommand command = accessor.getCommand();
            
            log.debug("Processing STOMP message - Command: {}, SessionId: {}", command, sessionId);

            // Set user for all commands that have a sessionId
            if (sessionId != null && accessor.getUser() == null) {
                Principal principal = new Principal() {
                    @Override
                    public String getName() {
                        return sessionId;
                    }

                    @Override
                    public String toString() {
                        return getName();
                    }
                };
                
                accessor.setUser(principal);
                log.debug("Set principal for session: {}", sessionId);
            }

            // Additional handling for specific commands
            if (StompCommand.CONNECT.equals(command)) {
                log.info("Client connecting with session: {}", sessionId);
            } else if (StompCommand.DISCONNECT.equals(command)) {
                log.info("Client disconnecting with session: {}", sessionId);
            } else if (StompCommand.SUBSCRIBE.equals(command)) {
                log.debug("Client subscribing with session: {}", sessionId);
            } else if (StompCommand.SEND.equals(command)) {
                log.debug("Client sending message with session: {}", sessionId);
            }
        }

        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        if (!sent) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null) {
                log.warn("Failed to send message - Command: {}, SessionId: {}", 
                    accessor.getCommand(), accessor.getSessionId());
            }
        }
    }
}