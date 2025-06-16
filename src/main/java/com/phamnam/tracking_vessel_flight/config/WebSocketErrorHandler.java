package com.phamnam.tracking_vessel_flight.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Component
@Slf4j
public class WebSocketErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    public Message<byte[]> handleClientMessageProcessingError(
            Message<byte[]> clientMessage, Throwable ex) {
        
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
        
        if (accessor != null) {
            String sessionId = accessor.getSessionId();
            StompCommand command = accessor.getCommand();
            
            log.error("WebSocket error for session {} with command {}: {}", 
                sessionId, command, ex.getMessage());
            
            // Handle specific errors
            if (ex.getMessage() != null && ex.getMessage().contains("No decoder")) {
                log.error("Decoder error - this usually indicates STOMP protocol issues");
                
                // Create error response
                StompHeaderAccessor errorAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
                errorAccessor.setSessionId(sessionId);
                errorAccessor.setMessage("Protocol error: " + ex.getMessage());
                
                return createErrorMessage(errorAccessor);
            }
        }
        
        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    private Message<byte[]> createErrorMessage(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return accessor.getMessageHeaders().containsKey("stompCommand") ? 
            accessor.getMessageToSend() : null;
    }
} 