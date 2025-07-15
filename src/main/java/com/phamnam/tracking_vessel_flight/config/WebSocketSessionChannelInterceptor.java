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
            StompCommand command = accessor.getCommand();
            String sessionId = accessor.getSessionId();
            
            // Enhanced logging for debugging
            log.debug("Processing STOMP message - Command: {}, SessionId: {}", command, sessionId);
            
            // Ensure sessionId is available for all operations
            if (sessionId != null) {
                // Store sessionId in message headers for easy access
                accessor.addNativeHeader("X-Session-Id", sessionId);
                
                // Set user for all commands that have a sessionId
                if (accessor.getUser() == null) {
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
            } else {
                log.warn("No sessionId found in STOMP message for command: {}", command);
            }

            // Enhanced command-specific handling
            if (StompCommand.CONNECT.equals(command)) {
                log.info("🔗 Client connecting with session: {}", sessionId);
            } else if (StompCommand.DISCONNECT.equals(command)) {
                log.info("🔌 Client disconnecting with session: {}", sessionId);
            } else if (StompCommand.SUBSCRIBE.equals(command)) {
                String destination = accessor.getDestination();
                log.info("📋 Client subscribing - Session: {}, Destination: {}", sessionId, destination);
            } else if (StompCommand.SEND.equals(command)) {
                String destination = accessor.getDestination();
                log.info("📤 Client sending message - Session: {}, Destination: {}", sessionId, destination);
            }
        }

        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        if (!sent) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null) {
                log.warn("❌ Failed to send message - Command: {}, SessionId: {}", 
                    accessor.getCommand(), accessor.getSessionId());
            }
        }
    }

    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECTED.equals(accessor.getCommand())) {
            String sessionId = accessor.getSessionId();
            log.info("✅ STOMP CONNECTED confirmed - Session: {}", sessionId);
            
            // Add session ID to frame headers for frontend access
            if (sessionId != null) {
                // Frontend expects frame.headers?.session || frame.headers?.sessionId
                accessor.addNativeHeader("session", sessionId);
                accessor.addNativeHeader("sessionId", sessionId);
                log.debug("Added session headers to CONNECTED frame: {}", sessionId);
            }
        }
        return message;
    }
}