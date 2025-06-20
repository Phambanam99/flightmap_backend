package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class ShipNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ShipNotificationService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WebSocketSubscriptionService subscriptionService;

    /**
     * Send ship update to all subscribers
     */
    public void broadcastShipUpdate(ShipTrackingRequest shipData) {
        try {
            // Broadcast to general ship tracking topic
            messagingTemplate.convertAndSend("/topic/ship-tracking", shipData);

            // Send to specific ship subscribers
            List<String> shipSubscribers = subscriptionService.getShipSubscribers(shipData.getMmsi());
            for (String sessionId : shipSubscribers) {
                messagingTemplate.convertAndSendToUser(
                        sessionId,
                        "/queue/ship-updates",
                        shipData);
            }

            // Send to area subscribers
            notifyAreaSubscribers(shipData);

            logger.debug("Ship update broadcast for MMSI: {}", shipData.getMmsi());

        } catch (Exception e) {
            logger.error("Error broadcasting ship update for MMSI {}: {}",
                    shipData.getMmsi(), e.getMessage());
        }
    }

    /**
     * Send ship update to specific session
     */
    public void sendShipUpdateToSession(String sessionId, ShipTrackingRequest shipData) {
        try {
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/ship-updates",
                    shipData);

            logger.debug("Ship update sent to session {} for MMSI: {}", sessionId, shipData.getMmsi());

        } catch (Exception e) {
            logger.error("Error sending ship update to session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Send ship area update to session
     */
    public void sendShipAreaUpdateToSession(String sessionId, List<ShipTrackingRequest> ships) {
        try {
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/ship-area-updates",
                    ships);

            logger.debug("Ship area update sent to session {} with {} ships", sessionId, ships.size());

        } catch (Exception e) {
            logger.error("Error sending ship area update to session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Send ship history to session
     */
    public void sendShipHistoryToSession(String sessionId, String mmsi, Map<String, Object> historyData) {
        try {
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/ship-history",
                    Map.of(
                            "mmsi", mmsi,
                            "data", historyData));

            logger.debug("Ship history sent to session {} for MMSI: {}", sessionId, mmsi);

        } catch (Exception e) {
            logger.error("Error sending ship history to session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Send monitoring dashboard update
     */
    public void broadcastMonitoringUpdate(Map<String, Object> monitoringData) {
        try {
            messagingTemplate.convertAndSend("/topic/ship-monitoring", monitoringData);

            logger.debug("Ship monitoring update broadcast");

        } catch (Exception e) {
            logger.error("Error broadcasting ship monitoring update: {}", e.getMessage());
        }
    }

    /**
     * Send emergency alert for ship
     */
    public void sendShipEmergencyAlert(ShipTrackingRequest shipData, String alertType, String message) {
        try {
            Map<String, Object> alert = Map.of(
                    "type", "SHIP_EMERGENCY",
                    "alertType", alertType,
                    "mmsi", shipData.getMmsi(),
                    "message", message,
                    "shipData", shipData,
                    "timestamp", System.currentTimeMillis());

            // Broadcast emergency alert to all monitoring sessions
            messagingTemplate.convertAndSend("/topic/ship-alerts", alert);

            // Send to specific ship subscribers
            List<String> subscribers = subscriptionService.getShipSubscribers(shipData.getMmsi());
            for (String sessionId : subscribers) {
                messagingTemplate.convertAndSendToUser(
                        sessionId,
                        "/queue/ship-alerts",
                        alert);
            }

            logger.warn("Ship emergency alert sent - MMSI: {}, Type: {}, Message: {}",
                    shipData.getMmsi(), alertType, message);

        } catch (Exception e) {
            logger.error("Error sending ship emergency alert: {}", e.getMessage());
        }
    }

    /**
     * Notify area subscribers about ship movement
     */
    private void notifyAreaSubscribers(ShipTrackingRequest shipData) {
        try {
            if (shipData.getLatitude() == null || shipData.getLongitude() == null) {
                return;
            }

            List<String> areaSubscribers = subscriptionService.getShipAreaSubscribers(
                    shipData.getLatitude(), shipData.getLongitude());

            for (String sessionId : areaSubscribers) {
                messagingTemplate.convertAndSendToUser(
                        sessionId,
                        "/queue/ship-area-updates",
                        List.of(shipData));
            }

            if (!areaSubscribers.isEmpty()) {
                logger.debug("Ship update sent to {} area subscribers for MMSI: {}",
                        areaSubscribers.size(), shipData.getMmsi());
            }

        } catch (Exception e) {
            logger.error("Error notifying area subscribers: {}", e.getMessage());
        }
    }

    /**
     * Send connection acknowledgment
     */
    public void sendConnectionAck(String sessionId, String message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/connection-ack",
                    Map.of(
                            "status", "connected",
                            "message", message,
                            "timestamp", System.currentTimeMillis()));

            logger.debug("Connection acknowledgment sent to session: {}", sessionId);

        } catch (Exception e) {
            logger.error("Error sending connection acknowledgment: {}", e.getMessage());
        }
    }

    /**
     * Send error message to session
     */
    public void sendErrorToSession(String sessionId, String error) {
        try {
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/errors",
                    Map.of(
                            "error", error,
                            "timestamp", System.currentTimeMillis()));

            logger.debug("Error message sent to session {}: {}", sessionId, error);

        } catch (Exception e) {
            logger.error("Error sending error message: {}", e.getMessage());
        }
    }
}