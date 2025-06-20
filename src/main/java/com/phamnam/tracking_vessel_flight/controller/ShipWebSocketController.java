package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.ShipSubscriptionRequest;
import com.phamnam.tracking_vessel_flight.dto.request.AreaSubscriptionRequest;
import com.phamnam.tracking_vessel_flight.service.realtime.WebSocketSubscriptionService;
import com.phamnam.tracking_vessel_flight.service.realtime.RealTimeDataQueryService;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Controller
public class ShipWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(ShipWebSocketController.class);

    @Autowired
    private WebSocketSubscriptionService subscriptionService;

    @Autowired
    private RealTimeDataQueryService realTimeDataQueryService;

    /**
     * Subscribe to ship updates in a geographic area
     */
    @MessageMapping("/ship/subscribe-area")
    public void subscribeToArea(@Payload AreaSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            logger.info("Ship area subscription request from session {}: {}", sessionId, request);

            subscriptionService.subscribeToShipArea(sessionId, request);

            // Send initial data for the area
            List<ShipTrackingRequest> shipsInArea = realTimeDataQueryService.getShipsInArea(
                    request.getMinLatitude(), request.getMaxLatitude(),
                    request.getMinLongitude(), request.getMaxLongitude());

            subscriptionService.sendShipAreaUpdate(sessionId, shipsInArea);

        } catch (Exception e) {
            logger.error("Error processing ship area subscription: {}", e.getMessage(), e);
        }
    }

    /**
     * Unsubscribe from ship area updates
     */
    @MessageMapping("/ship/unsubscribe-area")
    public void unsubscribeFromArea(@Payload AreaSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            logger.info("Ship area unsubscription request from session {}: {}", sessionId, request);

            subscriptionService.unsubscribeFromShipArea(sessionId, request);

        } catch (Exception e) {
            logger.error("Error processing ship area unsubscription: {}", e.getMessage(), e);
        }
    }

    /**
     * Subscribe to specific ship updates
     */
    @MessageMapping("/ship/subscribe-ship")
    public void subscribeToShip(@Payload ShipSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            logger.info("Ship subscription request from session {}: {}", sessionId, request);

            subscriptionService.subscribeToShip(sessionId, request.getMmsi());

            // Send initial ship data
            ShipTrackingRequest shipData = realTimeDataQueryService.getLatestShipData(request.getMmsi());
            if (shipData != null) {
                subscriptionService.sendShipUpdate(sessionId, shipData);
            }

            // Send recent history if requested
            if (request.getIncludeHistory() != null && request.getIncludeHistory()) {
                Map<String, Object> recentData = realTimeDataQueryService.getRecentShipData(
                        request.getMmsi(), 24); // Last 24 hours
                subscriptionService.sendShipHistory(sessionId, request.getMmsi(), recentData);
            }

        } catch (Exception e) {
            logger.error("Error processing ship subscription: {}", e.getMessage(), e);
        }
    }

    /**
     * Unsubscribe from specific ship updates
     */
    @MessageMapping("/ship/unsubscribe-ship")
    public void unsubscribeFromShip(@Payload ShipSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            logger.info("Ship unsubscription request from session {}: {}", sessionId, request);

            subscriptionService.unsubscribeFromShip(sessionId, request.getMmsi());

        } catch (Exception e) {
            logger.error("Error processing ship unsubscription: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle client connection to ship tracking
     */
    @SubscribeMapping("/topic/ship-tracking")
    public List<ShipTrackingRequest> onShipTrackingConnect() {
        try {
            logger.debug("Client connected to ship tracking");

            // Return all active ships for initial load
            return realTimeDataQueryService.getAllActiveShips();

        } catch (Exception e) {
            logger.error("Error handling ship tracking connection: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Handle client connection to ship monitoring dashboard
     */
    @SubscribeMapping("/topic/ship-monitoring")
    public Map<String, Object> onShipMonitoringConnect() {
        try {
            logger.debug("Client connected to ship monitoring dashboard");

            // Return system statistics and active ships
            Map<String, Object> response = realTimeDataQueryService.getSystemStatistics();
            response.put("ships", realTimeDataQueryService.getAllActiveShips());

            return response;

        } catch (Exception e) {
            logger.error("Error handling ship monitoring connection: {}", e.getMessage(), e);
            return Map.of("error", "Failed to load monitoring data");
        }
    }
}