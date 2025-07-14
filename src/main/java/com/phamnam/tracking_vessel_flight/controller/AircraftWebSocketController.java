package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.AreaSubscriptionRequest;
import com.phamnam.tracking_vessel_flight.dto.request.AircraftSubscriptionRequest;
import com.phamnam.tracking_vessel_flight.service.realtime.WebSocketSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Aircraft WebSocket Controller", description = "WebSocket endpoints for real-time aircraft tracking")
public class AircraftWebSocketController {

        private final WebSocketSubscriptionService subscriptionService;
        private final SimpMessagingTemplate messagingTemplate;

        /**
         * Xử lý khi client đăng ký theo dõi khu vực
         */
        @Operation(summary = "Subscribe to area", description = "Subscribe to aircraft updates within a geographic area")
        @MessageMapping("/subscribe-area")
        public void subscribeToArea(@Payload AreaSubscriptionRequest request,
                        SimpMessageHeaderAccessor headerAccessor) {
                String sessionId = headerAccessor.getSessionId();
                log.info("Client {} đăng ký khu vực: {},{} đến {},{}",
                                sessionId, request.getMinLat(), request.getMinLon(),
                                request.getMaxLat(), request.getMaxLon());

                // Thêm log chi tiết
                log.debug("STOMP headers: {}", headerAccessor.getMessageHeaders());
                log.debug("Processing area subscription with sessionId: {}", sessionId);
                log.debug("User principal: {}", headerAccessor.getUser());

                try {
                        // Gọi service với đầy đủ log - convert Double to float
                        subscriptionService.subscribeToArea(
                                        sessionId,
                                        request.getMinLat().floatValue(),
                                        request.getMaxLat().floatValue(),
                                        request.getMinLon().floatValue(),
                                        request.getMaxLon().floatValue());
                        log.info("Area subscription processed successfully for {}", sessionId);
                } catch (Exception e) {
                        log.error("Error processing area subscription: {}", e.getMessage(), e);
                }
        }

        /**
         * Test endpoint for broadcast functionality
         */
        @MessageMapping("/test-broadcast")
        public void testBroadcast(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
                String sessionId = headerAccessor.getSessionId();
                log.info("Test broadcast request from session: {}", sessionId);

                try {
                        // Broadcast to all subscribers of /topic/test
                        Map<String, Object> response = new HashMap<>();
                        response.put("type", "test-broadcast");
                        response.put("message", "Test broadcast successful");
                        response.put("sessionId", sessionId);
                        response.put("timestamp", java.time.LocalDateTime.now());

                        messagingTemplate.convertAndSend("/topic/test", response);
                        log.info("Test broadcast sent successfully");
                } catch (Exception e) {
                        log.error("Error sending test broadcast: {}", e.getMessage(), e);
                }
        }

        /**
         * Test endpoint to verify WebSocket user destination handling
         */
        @MessageMapping("/test-user-destination")
        public void testUserDestination(SimpMessageHeaderAccessor headerAccessor) {
                String sessionId = headerAccessor.getSessionId();
                log.info("Test user destination for session: {}", sessionId);
                log.debug("User principal: {}", headerAccessor.getUser());

                try {
                        // Test sending a message to user destination
                        subscriptionService.testUserDestination(sessionId);
                        log.info("Test message sent successfully to session: {}", sessionId);
                } catch (Exception e) {
                        log.error("Error sending test message to session {}: {}", sessionId, e.getMessage(), e);
                }
        }

        /**
         * Xử lý khi client hủy đăng ký khu vực
         */
        @Operation(summary = "Unsubscribe from area", description = "Unsubscribe from aircraft updates within a geographic area")
        @MessageMapping("/unsubscribe-area")
        public void unsubscribeFromArea(@Payload AreaSubscriptionRequest request,
                        SimpMessageHeaderAccessor headerAccessor) {
                String sessionId = headerAccessor.getSessionId();
                log.info("Client {} hủy đăng ký khu vực: {},{} đến {},{}",
                                sessionId, request.getMinLat(), request.getMinLon(),
                                request.getMaxLat(), request.getMaxLon());

                subscriptionService.unsubscribeFromArea(sessionId,
                                request.getMinLat().doubleValue(), request.getMaxLat().doubleValue(),
                                request.getMinLon().doubleValue(), request.getMaxLon().doubleValue());
        }

        /**
         * Xử lý khi client đăng ký theo dõi máy bay cụ thể
         */
        @Operation(summary = "Subscribe to aircraft", description = "Subscribe to updates for a specific aircraft")
        @MessageMapping("/subscribe-aircraft")
        public void subscribeToAircraft(@Payload AircraftSubscriptionRequest request,
                        SimpMessageHeaderAccessor headerAccessor) {
                String sessionId = headerAccessor.getSessionId();
                String hexIdent = request.getHexIdent();
                log.info("Client {} đăng ký máy bay: {}", sessionId, hexIdent);

                subscriptionService.subscribeToAircraft(sessionId, hexIdent);
        }

        /**
         * Xử lý khi client hủy đăng ký máy bay
         */
        @Operation(summary = "Unsubscribe from aircraft", description = "Unsubscribe from updates for a specific aircraft")
        @MessageMapping("/unsubscribe-aircraft")
        public void unsubscribeFromAircraft(@Payload AircraftSubscriptionRequest request,
                        SimpMessageHeaderAccessor headerAccessor) {
                String sessionId = headerAccessor.getSessionId();
                String hexIdent = request.getHexIdent();
                log.info("Client {} hủy đăng ký máy bay: {}", sessionId, hexIdent);

                subscriptionService.unsubscribeFromAircraft(sessionId, hexIdent);
        }
}