/*
 * ShipNotificationService - Real-time ship notification service
 * This service handles WebSocket notifications for ship tracking updates
 */

package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class ShipNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ShipNotificationService.class);

    @Autowired
    private WebSocketSubscriptionService subscriptionService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast ship update to all subscribers
     */
    public void broadcastShipUpdate(ShipTrackingRequest shipData) {
        try {
            // Get subscribers for this specific ship
            List<String> shipSubscribers = subscriptionService.getShipSubscribers(shipData.getMmsi());

            // Send to specific ship subscribers
            for (String sessionId : shipSubscribers) {
                sendShipUpdateToSession(sessionId, shipData);
            }

            // Get area subscribers who should receive this update
            List<String> areaSubscribers = subscriptionService.getShipAreaSubscribers(
                    shipData.getLatitude(), shipData.getLongitude());

            for (String sessionId : areaSubscribers) {
                sendShipUpdateToSession(sessionId, shipData);
            }

            logger.debug("Ship update broadcast for MMSI: {}", shipData.getMmsi());

        } catch (Exception e) {
            logger.error("Error broadcasting ship update for MMSI: {}: {}",
                    shipData.getMmsi(), e.getMessage());
        }
    }

    /**
     * Send ship update to specific session
     */
    public void sendShipUpdateToSession(String sessionId, ShipTrackingRequest shipData) {
        try {
            subscriptionService.sendShipUpdate(sessionId, shipData);
            logger.debug("Ship update sent to session {} for MMSI: {}", sessionId, shipData.getMmsi());

        } catch (Exception e) {
            logger.error("Error sending ship update to session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Send emergency alert for ship
     */
    public void sendShipEmergencyAlert(ShipTrackingRequest shipData, String alertType, String message) {
        try {
            Map<String, Object> alertData = createEmergencyAlert(shipData, alertType, message);

            // Send to all ship subscribers
            List<String> subscribers = subscriptionService.getShipSubscribers(shipData.getMmsi());
            for (String sessionId : subscribers) {
                messagingTemplate.convertAndSendToUser(sessionId, "/queue/ship/alerts", alertData);
            }

            // Also broadcast to general alerts channel
            messagingTemplate.convertAndSend("/topic/alerts/ships", alertData);

            logger.warn("Emergency alert sent for ship MMSI: {} - Type: {}",
                    shipData.getMmsi(), alertType, message);

        } catch (Exception e) {
            logger.error("Error sending emergency alert for ship MMSI: {}: {}",
                    shipData.getMmsi(), e.getMessage());
        }
    }

    /**
     * Send ship status update to area subscribers
     */
    public void sendShipStatusToArea(ShipTrackingRequest shipData, String statusType) {
        try {
            Map<String, Object> statusUpdate = Map.of(
                    "type", "ship_status_update",
                    "statusType", statusType,
                    "mmsi", shipData.getMmsi(),
                    "latitude", shipData.getLatitude(),
                    "longitude", shipData.getLongitude(),
                    "speed", shipData.getSpeed() != null ? shipData.getSpeed() : 0.0,
                    "course", shipData.getCourse() != null ? shipData.getCourse() : 0.0,
                    "timestamp",
                    shipData.getTimestamp() != null ? shipData.getTimestamp() : java.time.LocalDateTime.now());

            List<String> areaSubscribers = subscriptionService.getShipAreaSubscribers(
                    shipData.getLatitude(), shipData.getLongitude());

            for (String sessionId : areaSubscribers) {
                messagingTemplate.convertAndSendToUser(sessionId, "/queue/ship/status", statusUpdate);
            }

            logger.debug("Ship status update sent to {} area subscribers for MMSI: {}",
                    areaSubscribers.size(), shipData.getMmsi());

        } catch (Exception e) {
            logger.error("Error sending ship status update to area for MMSI: {}: {}",
                    shipData.getMmsi(), e.getMessage());
        }
    }

    /**
     * Broadcast batch ship updates
     */
    public void broadcastBatchShipUpdates(List<ShipTrackingRequest> shipUpdates) {
        try {
            Map<String, Object> batchUpdate = Map.of(
                    "type", "ship_batch_update",
                    "count", shipUpdates.size(),
                    "ships", shipUpdates,
                    "timestamp", java.time.LocalDateTime.now());

            // Send to all ship subscribers
            messagingTemplate.convertAndSend("/topic/ships/batch", batchUpdate);

            logger.debug("Batch ship updates sent: {} ships", shipUpdates.size());

        } catch (Exception e) {
            logger.error("Error sending batch ship updates: {}", e.getMessage());
        }
    }

    /**
     * Create emergency alert data structure
     */
    private Map<String, Object> createEmergencyAlert(ShipTrackingRequest shipData, String alertType, String message) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", "ship_emergency");
        alert.put("alertType", alertType);
        alert.put("message", message);
        alert.put("mmsi", shipData.getMmsi());
        alert.put("latitude", shipData.getLatitude());
        alert.put("longitude", shipData.getLongitude());
        alert.put("speed", shipData.getSpeed());
        alert.put("course", shipData.getCourse());
        alert.put("navStatus", shipData.getNavStatus());
        alert.put("timestamp",
                shipData.getTimestamp() != null ? shipData.getTimestamp() : java.time.LocalDateTime.now());
        alert.put("severity", "HIGH");
        return alert;
    }

    /**
     * Send ship tracking history to subscribers
     */
    public void sendShipHistory(String sessionId, String mmsi, Map<String, Object> historyData) {
        try {
            subscriptionService.sendShipHistory(sessionId, mmsi, historyData);
            logger.debug("Ship history sent to session {} for MMSI: {}", sessionId, mmsi);

        } catch (Exception e) {
            logger.error("Error sending ship history to session {}: {}", sessionId, e.getMessage());
        }
    }
}