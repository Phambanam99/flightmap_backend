package com.phamnam.tracking_vessel_flight.service.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.models.AlertEvent;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Track active subscribers by topic
    private final Map<String, Set<String>> subscribers = new ConcurrentHashMap<>();

    // ============================================================================
    // AIRCRAFT POSITION UPDATES
    // ============================================================================

    @Async
    public void broadcastAircraftUpdate(FlightTracking flightTracking) {
        try {
            Map<String, Object> update = Map.of(
                    "type", "aircraft-update",
                    "timestamp", LocalDateTime.now(),
                    "data", Map.of(
                            "hexident", flightTracking.getHexident(),
                            "latitude", flightTracking.getLatitude(),
                            "longitude", flightTracking.getLongitude(),
                            "altitude", flightTracking.getAltitude(),
                            "groundSpeed", flightTracking.getGroundSpeed(),
                            "track", flightTracking.getTrack(),
                            "squawk", flightTracking.getSquawk(),
                            "onGround", flightTracking.getOnGround(),
                            "emergency", flightTracking.getEmergency(),
                            "lastUpdate", flightTracking.getTimestamp()));

            // Broadcast to all aircraft subscribers
            messagingTemplate.convertAndSend("/topic/aircraft/positions", update);

            // Also send specific aircraft update
            messagingTemplate.convertAndSend(
                    "/topic/aircraft/" + flightTracking.getHexident(), update);

            log.debug("Broadcasted aircraft update for {}", flightTracking.getHexident());

        } catch (Exception e) {
            log.error("Failed to broadcast aircraft update", e);
        }
    }

    // ============================================================================
    // VESSEL POSITION UPDATES
    // ============================================================================

    @Async
    public void broadcastVesselUpdate(ShipTracking shipTracking) {
        try {
            Map<String, Object> update = Map.of(
                    "type", "vessel-update",
                    "timestamp", LocalDateTime.now(),
                    "data", Map.of(
                            "mmsi", shipTracking.getMmsi(),
                            "latitude", shipTracking.getLatitude(),
                            "longitude", shipTracking.getLongitude(),
                            "speed", shipTracking.getSpeed(),
                            "course", shipTracking.getCourse(),
                            "heading", shipTracking.getHeading(),
                            "navigationStatus", shipTracking.getNavigationStatus(),
                            "securityAlert", shipTracking.getSecurityAlert(),
                            "dangerousCargo", shipTracking.getDangerousCargo(),
                            "lastUpdate", shipTracking.getTimestamp()));

            // Broadcast to all vessel subscribers
            messagingTemplate.convertAndSend("/topic/vessels/positions", update);

            // Also send specific vessel update
            messagingTemplate.convertAndSend(
                    "/topic/vessels/" + shipTracking.getMmsi(), update);

            log.debug("Broadcasted vessel update for {}", shipTracking.getMmsi());

        } catch (Exception e) {
            log.error("Failed to broadcast vessel update", e);
        }
    }

    // ============================================================================
    // ALERT NOTIFICATIONS
    // ============================================================================

    @Async
    public void broadcastAlert(AlertEvent alertEvent) {
        try {
            Map<String, Object> alert = Map.of(
                    "type", "alert",
                    "timestamp", LocalDateTime.now(),
                    "data", Map.of(
                            "id", alertEvent.getId(),
                            "entityType", alertEvent.getEntityType(),
                            "entityId", alertEvent.getEntityId(),
                            "entityName", alertEvent.getEntityName(),
                            "priority", alertEvent.getPriority(),
                            "message", alertEvent.getAlertMessage(),
                            "latitude", alertEvent.getLatitude(),
                            "longitude", alertEvent.getLongitude(),
                            "eventTime", alertEvent.getEventTime(),
                            "status", alertEvent.getStatus()));

            // Broadcast to all alert subscribers
            messagingTemplate.convertAndSend("/topic/alerts", alert);

            // Send priority alerts to dedicated channel
            if ("CRITICAL".equals(alertEvent.getPriority()) || "HIGH".equals(alertEvent.getPriority())) {
                messagingTemplate.convertAndSend("/topic/alerts/priority", alert);
            }

            log.info("Broadcasted {} priority alert for {} {}",
                    alertEvent.getPriority(), alertEvent.getEntityType(), alertEvent.getEntityId());

        } catch (Exception e) {
            log.error("Failed to broadcast alert", e);
        }
    }

    // ============================================================================
    // SYSTEM STATUS UPDATES
    // ============================================================================

    @Async
    public void broadcastSystemStatus(Map<String, Object> status) {
        try {
            Map<String, Object> statusUpdate = Map.of(
                    "type", "system-status",
                    "timestamp", LocalDateTime.now(),
                    "data", status);

            messagingTemplate.convertAndSend("/topic/system/status", statusUpdate);
            log.debug("Broadcasted system status update");

        } catch (Exception e) {
            log.error("Failed to broadcast system status", e);
        }
    }

    // ============================================================================
    // STATISTICS UPDATES
    // ============================================================================

    @Async
    public void broadcastStatistics(Map<String, Object> statistics) {
        try {
            Map<String, Object> statsUpdate = Map.of(
                    "type", "statistics",
                    "timestamp", LocalDateTime.now(),
                    "data", statistics);

            messagingTemplate.convertAndSend("/topic/statistics", statsUpdate);
            log.debug("Broadcasted statistics update");

        } catch (Exception e) {
            log.error("Failed to broadcast statistics", e);
        }
    }

    // ============================================================================
    // KAFKA MESSAGE LISTENERS
    // ============================================================================

    @KafkaListener(topics = "websocket-updates", groupId = "websocket-group")
    public void handleWebSocketUpdates(String message) {
        try {
            Map<String, Object> updateData = objectMapper.readValue(message, Map.class);
            String updateType = (String) updateData.get("type");

            switch (updateType) {
                case "aircraft-position":
                    handleAircraftPositionFromKafka(updateData);
                    break;
                case "vessel-position":
                    handleVesselPositionFromKafka(updateData);
                    break;
                case "alert":
                    handleAlertFromKafka(updateData);
                    break;
                case "system-status":
                    handleSystemStatusFromKafka(updateData);
                    break;
                default:
                    log.debug("Unknown update type: {}", updateType);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse Kafka message for WebSocket update", e);
        }
    }

    private void handleAircraftPositionFromKafka(Map<String, Object> data) {
        messagingTemplate.convertAndSend("/topic/aircraft/positions", data);
    }

    private void handleVesselPositionFromKafka(Map<String, Object> data) {
        messagingTemplate.convertAndSend("/topic/vessels/positions", data);
    }

    private void handleAlertFromKafka(Map<String, Object> data) {
        messagingTemplate.convertAndSend("/topic/alerts", data);
    }

    private void handleSystemStatusFromKafka(Map<String, Object> data) {
        messagingTemplate.convertAndSend("/topic/system/status", data);
    }

    // ============================================================================
    // BATCH UPDATES
    // ============================================================================

    @Async
    public void broadcastBatchAircraftUpdates(java.util.List<FlightTracking> flightTrackings) {
        if (flightTrackings.isEmpty())
            return;

        try {
            java.util.List<Map<String, Object>> updates = flightTrackings.stream()
                    .map(ft -> Map.of(
                            "hexident", ft.getHexident(),
                            "latitude", ft.getLatitude(),
                            "longitude", ft.getLongitude(),
                            "altitude", ft.getAltitude(),
                            "groundSpeed", ft.getGroundSpeed(),
                            "track", ft.getTrack(),
                            "lastUpdate", ft.getTimestamp()))
                    .collect(java.util.stream.Collectors.toList());

            Map<String, Object> batchUpdate = Map.of(
                    "type", "aircraft-batch-update",
                    "timestamp", LocalDateTime.now(),
                    "count", updates.size(),
                    "data", updates);

            messagingTemplate.convertAndSend("/topic/aircraft/batch", batchUpdate);
            log.debug("Broadcasted batch aircraft updates: {} records", updates.size());

        } catch (Exception e) {
            log.error("Failed to broadcast batch aircraft updates", e);
        }
    }

    @Async
    public void broadcastBatchVesselUpdates(java.util.List<ShipTracking> shipTrackings) {
        if (shipTrackings.isEmpty())
            return;

        try {
            java.util.List<Map<String, Object>> updates = shipTrackings.stream()
                    .map(st -> Map.of(
                            "mmsi", st.getMmsi(),
                            "latitude", st.getLatitude(),
                            "longitude", st.getLongitude(),
                            "speed", st.getSpeed(),
                            "course", st.getCourse(),
                            "heading", st.getHeading(),
                            "lastUpdate", st.getTimestamp()))
                    .collect(java.util.stream.Collectors.toList());

            Map<String, Object> batchUpdate = Map.of(
                    "type", "vessel-batch-update",
                    "timestamp", LocalDateTime.now(),
                    "count", updates.size(),
                    "data", updates);

            messagingTemplate.convertAndSend("/topic/vessels/batch", batchUpdate);
            log.debug("Broadcasted batch vessel updates: {} records", updates.size());

        } catch (Exception e) {
            log.error("Failed to broadcast batch vessel updates", e);
        }
    }

    // ============================================================================
    // GEOGRAPHIC AREA UPDATES
    // ============================================================================

    @Async
    public void broadcastAreaUpdate(String areaId, java.util.List<Map<String, Object>> entities) {
        try {
            Map<String, Object> areaUpdate = Map.of(
                    "type", "area-update",
                    "timestamp", LocalDateTime.now(),
                    "areaId", areaId,
                    "entityCount", entities.size(),
                    "entities", entities);

            messagingTemplate.convertAndSend("/topic/areas/" + areaId, areaUpdate);
            log.debug("Broadcasted area update for {}: {} entities", areaId, entities.size());

        } catch (Exception e) {
            log.error("Failed to broadcast area update for {}", areaId, e);
        }
    }

    // ============================================================================
    // USER-SPECIFIC UPDATES
    // ============================================================================

    @Async
    public void sendPersonalUpdate(String userId, String type, Object data) {
        try {
            Map<String, Object> update = Map.of(
                    "type", type,
                    "timestamp", LocalDateTime.now(),
                    "data", data);

            messagingTemplate.convertAndSendToUser(userId, "/queue/personal", update);
            log.debug("Sent personal update to user {}: {}", userId, type);

        } catch (Exception e) {
            log.error("Failed to send personal update to user {}", userId, e);
        }
    }

    // ============================================================================
    // CONNECTION MANAGEMENT
    // ============================================================================

    public void addSubscriber(String topic, String sessionId) {
        subscribers.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.debug("Added subscriber {} to topic {}", sessionId, topic);
    }

    public void removeSubscriber(String topic, String sessionId) {
        Set<String> topicSubscribers = subscribers.get(topic);
        if (topicSubscribers != null) {
            topicSubscribers.remove(sessionId);
            if (topicSubscribers.isEmpty()) {
                subscribers.remove(topic);
            }
        }
        log.debug("Removed subscriber {} from topic {}", sessionId, topic);
    }

    public int getSubscriberCount(String topic) {
        return subscribers.getOrDefault(topic, Set.of()).size();
    }

    public Map<String, Integer> getAllSubscriberCounts() {
        return subscribers.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()));
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    public void broadcastGenericMessage(String topic, Object message) {
        try {
            messagingTemplate.convertAndSend(topic, message);
            log.debug("Broadcasted generic message to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to broadcast generic message to topic: {}", topic, e);
        }
    }

    public boolean isTopicActive(String topic) {
        return subscribers.containsKey(topic) && !subscribers.get(topic).isEmpty();
    }
}