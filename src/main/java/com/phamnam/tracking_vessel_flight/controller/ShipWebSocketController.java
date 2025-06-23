/*
 * ShipWebSocketController - Temporarily disabled pending DTO alignment
 * This controller will be re-enabled once DTOs support required fields
 */

// TODO: Re-enable after DTO restructuring
/*
 * package com.phamnam.tracking_vessel_flight.controller;
 * 
 * import
 * com.phamnam.tracking_vessel_flight.dto.request.ShipSubscriptionRequest;
 * import
 * com.phamnam.tracking_vessel_flight.dto.request.AreaSubscriptionRequest;
 * import com.phamnam.tracking_vessel_flight.service.realtime.
 * WebSocketSubscriptionService;
 * import
 * com.phamnam.tracking_vessel_flight.service.realtime.RealTimeDataQueryService;
 * import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
 * import org.springframework.beans.factory.annotation.Autowired;
 * import org.springframework.messaging.handler.annotation.MessageMapping;
 * import org.springframework.messaging.handler.annotation.Payload;
 * import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
 * import org.springframework.messaging.simp.annotation.SubscribeMapping;
 * import org.springframework.stereotype.Controller;
 * import org.slf4j.Logger;
 * import org.slf4j.LoggerFactory;
 * 
 * import java.util.List;
 * import java.util.Map;
 * 
 * @Controller
 * public class ShipWebSocketController {
 * // Implementation will be added after DTO alignment
 * }
 */

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Controller
@Tag(name = "Ship WebSocket Controller", description = "WebSocket endpoints for real-time ship tracking")
public class ShipWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(ShipWebSocketController.class);

    @Autowired
    private WebSocketSubscriptionService subscriptionService;

    @Autowired
    private RealTimeDataQueryService realTimeDataQueryService;

    /**
     * Subscribe to ship area updates
     */
    @Operation(summary = "Subscribe to ship area", description = "Subscribe to ship updates within a geographic area")
    @MessageMapping("/ship/subscribe-area")
    public void subscribeToShipArea(@Payload AreaSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Ship area subscription request from session {}: lat({} to {}), lon({} to {})",
                sessionId, request.getMinLatitude(), request.getMaxLatitude(),
                request.getMinLongitude(), request.getMaxLongitude());

        try {
            subscriptionService.subscribeToShipArea(sessionId, request);

            // Get ships in area and send initial data
            List<ShipTrackingRequest> shipsInArea = getShipsInArea(
                    request.getMinLatitude(), request.getMaxLatitude(),
                    request.getMinLongitude(), request.getMaxLongitude());

            subscriptionService.sendShipAreaUpdate(sessionId, shipsInArea);
            logger.info("Ship area subscription processed successfully for session {}", sessionId);

        } catch (Exception e) {
            logger.error("Error processing ship area subscription for session {}: {}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * Unsubscribe from ship area updates
     */
    @Operation(summary = "Unsubscribe from ship area", description = "Unsubscribe from ship updates within a geographic area")
    @MessageMapping("/ship/unsubscribe-area")
    public void unsubscribeFromShipArea(@Payload AreaSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Ship area unsubscription request from session {}", sessionId);

        try {
            subscriptionService.unsubscribeFromShipArea(sessionId, request);
            logger.info("Ship area unsubscription processed successfully for session {}", sessionId);

        } catch (Exception e) {
            logger.error("Error processing ship area unsubscription for session {}: {}", sessionId, e.getMessage(), e);
        }
    }

    /**
     * Subscribe to specific ship updates
     */
    @Operation(summary = "Subscribe to ship", description = "Subscribe to updates for a specific ship")
    @MessageMapping("/ship/subscribe-ship")
    public void subscribeToShip(@Payload ShipSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Ship subscription request from session {} for MMSI: {}", sessionId, request.getMmsi());

        try {
            subscriptionService.subscribeToShip(sessionId, request.getMmsi());

            // Get current ship data and send
            ShipTrackingRequest shipData = getCurrentShipData(request.getMmsi());
            if (shipData != null) {
                subscriptionService.sendShipUpdate(sessionId, shipData);
            }

            // Get recent ship history and send
            Map<String, Object> recentData = getRecentShipData(request.getMmsi());
            if (recentData != null && !recentData.isEmpty()) {
                subscriptionService.sendShipHistory(sessionId, request.getMmsi(), recentData);
            }

            logger.info("Ship subscription processed successfully for session {} and MMSI {}", sessionId,
                    request.getMmsi());

        } catch (Exception e) {
            logger.error("Error processing ship subscription for session {} and MMSI {}: {}",
                    sessionId, request.getMmsi(), e.getMessage(), e);
        }
    }

    /**
     * Unsubscribe from specific ship updates
     */
    @Operation(summary = "Unsubscribe from ship", description = "Unsubscribe from updates for a specific ship")
    @MessageMapping("/ship/unsubscribe-ship")
    public void unsubscribeFromShip(@Payload ShipSubscriptionRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Ship unsubscription request from session {} for MMSI: {}", sessionId, request.getMmsi());

        try {
            subscriptionService.unsubscribeFromShip(sessionId, request.getMmsi());
            logger.info("Ship unsubscription processed successfully for session {} and MMSI {}", sessionId,
                    request.getMmsi());

        } catch (Exception e) {
            logger.error("Error processing ship unsubscription for session {} and MMSI {}: {}",
                    sessionId, request.getMmsi(), e.getMessage(), e);
        }
    }

    // Helper methods using RealTimeDataQueryService

    private List<ShipTrackingRequest> getShipsInArea(Double minLat, Double maxLat, Double minLon, Double maxLon) {
        try {
            return realTimeDataQueryService.getShipsInArea(minLat, maxLat, minLon, maxLon);
        } catch (Exception e) {
            logger.error("Error getting ships in area: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    private ShipTrackingRequest getCurrentShipData(String mmsi) {
        try {
            return realTimeDataQueryService.getCurrentShipData(mmsi);
        } catch (Exception e) {
            logger.error("Error getting current ship data for MMSI {}: {}", mmsi, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> getRecentShipData(String mmsi) {
        try {
            return realTimeDataQueryService.getRecentShipData(mmsi);
        } catch (Exception e) {
            logger.error("Error getting recent ship data for MMSI {}: {}", mmsi, e.getMessage(), e);
            return java.util.Collections.emptyMap();
        }
    }
}