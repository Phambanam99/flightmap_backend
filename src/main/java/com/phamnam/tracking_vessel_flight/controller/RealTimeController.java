package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.models.enums.AlertPriority;
import com.phamnam.tracking_vessel_flight.models.enums.EntityType;
import com.phamnam.tracking_vessel_flight.service.realtime.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/realtime")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RealTimeController {

    private final ExternalApiService externalApiService;
    private final RealTimeDataProcessor dataProcessor;
    private final AlertRuleEngine alertRuleEngine;
    private final AnalyticsDashboardService analyticsService;
    private final WebSocketService webSocketService;

    // ============================================================================
    // SYSTEM STATUS ENDPOINTS
    // ============================================================================

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "status", "operational",
                    "apiStatus", externalApiService.getApiStatus(),
                    "processingStats", dataProcessor.getProcessingStatistics(),
                    "websocketStats", webSocketService.getAllSubscriberCounts());

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting system status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve system status"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthCheck() {
        try {
            boolean flightradar24Available = externalApiService.isFlightRadar24Available();
            boolean marineTrafficAvailable = externalApiService.isMarineTrafficAvailable();

            boolean healthy = flightradar24Available || marineTrafficAvailable;

            Map<String, Object> health = Map.of(
                    "status", healthy ? "UP" : "DOWN",
                    "timestamp", System.currentTimeMillis(),
                    "checks", Map.of(
                            "flightRadar24", flightradar24Available ? "UP" : "DOWN",
                            "marineTraffic", marineTrafficAvailable ? "UP" : "DOWN"));

            return healthy ? ResponseEntity.ok(health) : ResponseEntity.status(503).body(health);
        } catch (Exception e) {
            log.error("Error performing health check", e);
            return ResponseEntity.status(503)
                    .body(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }

    // ============================================================================
    // ANALYTICS ENDPOINTS
    // ============================================================================

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardAnalytics() {
        try {
            Map<String, Object> analytics = analyticsService.getRealTimeStatistics();
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error getting dashboard analytics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve dashboard analytics"));
        }
    }

    @GetMapping("/analytics/geographic")
    public ResponseEntity<Map<String, Object>> getGeographicAnalytics() {
        try {
            Map<String, Object> geoAnalytics = analyticsService.getGeographicStatistics();
            return ResponseEntity.ok(geoAnalytics);
        } catch (Exception e) {
            log.error("Error getting geographic analytics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve geographic analytics"));
        }
    }

    @GetMapping("/analytics/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceAnalytics() {
        try {
            Map<String, Object> perfAnalytics = analyticsService.getPerformanceStatistics();
            return ResponseEntity.ok(perfAnalytics);
        } catch (Exception e) {
            log.error("Error getting performance analytics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve performance analytics"));
        }
    }

    @GetMapping("/analytics/historical")
    public ResponseEntity<Map<String, Object>> getHistoricalAnalytics(
            @RequestParam(defaultValue = "7") int days) {
        try {
            if (days < 1 || days > 365) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Days parameter must be between 1 and 365"));
            }

            Map<String, Object> histAnalytics = analyticsService.getHistoricalStatistics(days);
            return ResponseEntity.ok(histAnalytics);
        } catch (Exception e) {
            log.error("Error getting historical analytics for {} days", days, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve historical analytics"));
        }
    }

    @GetMapping("/analytics/entity/{entityType}/{entityId}")
    public ResponseEntity<Map<String, Object>> getEntityAnalytics(
            @PathVariable EntityType entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "7") int days) {
        try {
            Map<String, Object> entityAnalytics = analyticsService.getEntityAnalytics(entityType, entityId, days);
            return ResponseEntity.ok(entityAnalytics);
        } catch (Exception e) {
            log.error("Error getting analytics for {} {}", entityType, entityId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve entity analytics"));
        }
    }

    // ============================================================================
    // DATA PROCESSING ENDPOINTS
    // ============================================================================

    @PostMapping("/data/force-collection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> forceDataCollection() {
        try {
            dataProcessor.forceDataCollection();

            return ResponseEntity.ok(Map.of(
                    "message", "Data collection triggered successfully",
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Error forcing data collection", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to trigger data collection"));
        }
    }

    @GetMapping("/data/processing-stats")
    public ResponseEntity<Map<String, Object>> getProcessingStatistics() {
        try {
            Map<String, Object> stats = dataProcessor.getProcessingStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting processing statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve processing statistics"));
        }
    }

    @PostMapping("/data/clear-cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearProcessingCache() {
        try {
            dataProcessor.clearStaleCache();
            alertRuleEngine.clearRuleCache();

            return ResponseEntity.ok(Map.of(
                    "message", "Cache cleared successfully",
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to clear cache"));
        }
    }

    // ============================================================================
    // ALERT MANAGEMENT ENDPOINTS
    // ============================================================================

    @GetMapping("/alerts/active")
    public ResponseEntity<?> getActiveAlerts() {
        try {
            return ResponseEntity.ok(alertRuleEngine.getActiveAlerts());
        } catch (Exception e) {
            log.error("Error getting active alerts", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve active alerts"));
        }
    }

    @GetMapping("/alerts/entity/{entityType}/{entityId}")
    public ResponseEntity<?> getEntityAlerts(
            @PathVariable EntityType entityType,
            @PathVariable String entityId) {
        try {
            return ResponseEntity.ok(alertRuleEngine.getAlertsByEntity(entityType, entityId));
        } catch (Exception e) {
            log.error("Error getting alerts for {} {}", entityType, entityId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve entity alerts"));
        }
    }

    @PostMapping("/alerts/manual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createManualAlert(
            @RequestBody CreateManualAlertRequest request) {
        try {
            if (request.getEntityType() == null || request.getEntityId() == null ||
                    request.getPriority() == null || request.getMessage() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required fields"));
            }

            var alertEvent = alertRuleEngine.createManualAlert(
                    request.getEntityType(),
                    request.getEntityId(),
                    request.getPriority(),
                    request.getMessage(),
                    request.getLatitude(),
                    request.getLongitude());

            return ResponseEntity.ok(alertEvent);
        } catch (Exception e) {
            log.error("Error creating manual alert", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create manual alert"));
        }
    }

    @PostMapping("/alerts/{alertId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resolveAlert(
            @PathVariable Long alertId,
            @RequestBody ResolveAlertRequest request) {
        try {
            if (request.getResolution() == null || request.getResolution().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Resolution message is required"));
            }

            alertRuleEngine.resolveAlert(alertId, request.getResolution());

            return ResponseEntity.ok(Map.of(
                    "message", "Alert resolved successfully",
                    "alertId", alertId,
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Error resolving alert {}", alertId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to resolve alert"));
        }
    }

    // ============================================================================
    // WEBSOCKET MANAGEMENT ENDPOINTS
    // ============================================================================

    @GetMapping("/websocket/stats")
    public ResponseEntity<Map<String, Object>> getWebSocketStatistics() {
        try {
            Map<String, Integer> subscriberCounts = webSocketService.getAllSubscriberCounts();

            Map<String, Object> stats = Map.of(
                    "subscriberCounts", subscriberCounts,
                    "totalSubscribers", subscriberCounts.values().stream().mapToInt(Integer::intValue).sum(),
                    "activeTopics", subscriberCounts.size(),
                    "timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting WebSocket statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve WebSocket statistics"));
        }
    }

    @PostMapping("/websocket/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> broadcastMessage(
            @RequestBody BroadcastMessageRequest request) {
        try {
            if (request.getTopic() == null || request.getMessage() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Topic and message are required"));
            }

            webSocketService.broadcastGenericMessage(request.getTopic(), request.getMessage());

            return ResponseEntity.ok(Map.of(
                    "message", "Broadcast sent successfully",
                    "topic", request.getTopic(),
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Error broadcasting message", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to broadcast message"));
        }
    }

    // ============================================================================
    // REQUEST/RESPONSE CLASSES
    // ============================================================================

    public static class CreateManualAlertRequest {
        private EntityType entityType;
        private String entityId;
        private AlertPriority priority;
        private String message;
        private Double latitude;
        private Double longitude;

        // Getters and setters
        public EntityType getEntityType() {
            return entityType;
        }

        public void setEntityType(EntityType entityType) {
            this.entityType = entityType;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public AlertPriority getPriority() {
            return priority;
        }

        public void setPriority(AlertPriority priority) {
            this.priority = priority;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }
    }

    public static class ResolveAlertRequest {
        private String resolution;

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }
    }

    public static class BroadcastMessageRequest {
        private String topic;
        private Object message;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public Object getMessage() {
            return message;
        }

        public void setMessage(Object message) {
            this.message = message;
        }
    }
}