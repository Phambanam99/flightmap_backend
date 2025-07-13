package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.service.kafka.KafkaMonitoringService;
import com.phamnam.tracking_vessel_flight.service.kafka.DeadLetterQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring/kafka")
@Tag(name = "Kafka Monitoring", description = "Comprehensive Kafka system monitoring and error tracking")
public class KafkaMonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMonitoringController.class);

    @Autowired
    private KafkaMonitoringService kafkaMonitoringService;

    @Autowired
    private DeadLetterQueueService deadLetterQueueService;

    @GetMapping("/health")
    @Operation(summary = "Get Kafka system health summary", description = "Quick overview of Kafka system health and error status")
    public ResponseEntity<Map<String, Object>> getHealthSummary() {
        try {
            Map<String, Object> health = kafkaMonitoringService.getHealthSummary();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Error getting Kafka health summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Unable to retrieve health summary"));
        }
    }

    @GetMapping("/report")
    @Operation(summary = "Get comprehensive monitoring report", description = "Detailed report including error patterns, metrics, and health assessment")
    public ResponseEntity<Map<String, Object>> getMonitoringReport() {
        try {
            Map<String, Object> report = kafkaMonitoringService.getMonitoringReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Error getting Kafka monitoring report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to generate monitoring report",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get monitoring dashboard data", description = "Combined data for monitoring dashboard including all key metrics")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            // Get monitoring data
            dashboard.put("kafkaMonitoring", kafkaMonitoringService.getMonitoringReport());

            // Get dead letter queue data
            dashboard.put("deadLetterQueue", deadLetterQueueService.getMetrics());

            // Get health summary
            dashboard.put("healthSummary", kafkaMonitoringService.getHealthSummary());

            // Calculate overall system status
            Map<String, Object> kafkaHealth = kafkaMonitoringService.getHealthSummary();
            String kafkaStatus = (String) kafkaHealth.get("status");

            Map<String, Object> dlqMetrics = deadLetterQueueService.getMetrics();
            long dlqMessages = (Long) dlqMetrics.get("deadLetterMessageCount");

            String overallStatus = determineOverallStatus(kafkaStatus, dlqMessages);
            dashboard.put("overallStatus", overallStatus);

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            logger.error("Error getting dashboard data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to get dashboard data",
                    "message", e.getMessage()));
        }
    }

    @PostMapping("/reset-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset all monitoring metrics", description = "Reset all Kafka monitoring and dead letter queue metrics (admin only)")
    public ResponseEntity<String> resetAllMetrics() {
        try {
            kafkaMonitoringService.resetMetrics();
            deadLetterQueueService.resetMetrics();

            logger.info("All Kafka monitoring metrics reset by admin");
            return ResponseEntity.ok("All monitoring metrics reset successfully");
        } catch (Exception e) {
            logger.error("Error resetting monitoring metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to reset metrics");
        }
    }

    @GetMapping("/errors/by-topic")
    @Operation(summary = "Get errors grouped by topic", description = "Detailed breakdown of errors by Kafka topic")
    public ResponseEntity<Map<String, Object>> getErrorsByTopic() {
        try {
            Map<String, Object> report = kafkaMonitoringService.getMonitoringReport();

            Map<String, Object> result = new HashMap<>();
            result.put("errorsByTopic", report.get("errorsByTopic"));
            result.put("lastErrorByTopic", report.get("lastErrorByTopic"));
            result.put("reportTimestamp", report.get("reportTimestamp"));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting errors by topic: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/errors/by-type")
    @Operation(summary = "Get errors grouped by type", description = "Detailed breakdown of errors by error type")
    public ResponseEntity<Map<String, Object>> getErrorsByType() {
        try {
            Map<String, Object> report = kafkaMonitoringService.getMonitoringReport();

            Map<String, Object> result = new HashMap<>();
            result.put("errorsByType", report.get("errorsByType"));
            result.put("mostCommonErrorType",
                    ((Map<String, Object>) report.get("healthAssessment")).get("mostCommonErrorType"));
            result.put("reportTimestamp", report.get("reportTimestamp"));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting errors by type: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/errors/recent")
    @Operation(summary = "Get recent errors", description = "List of most recent Kafka errors with details")
    public ResponseEntity<Map<String, Object>> getRecentErrors() {
        try {
            Map<String, Object> report = kafkaMonitoringService.getMonitoringReport();

            Map<String, Object> result = new HashMap<>();
            result.put("recentErrors", report.get("recentErrors"));
            result.put("totalKafkaErrors", report.get("totalKafkaErrors"));
            result.put("lastErrorTime", report.get("lastErrorTime"));
            result.put("reportTimestamp", report.get("reportTimestamp"));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting recent errors: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Determine overall system status based on individual component statuses
     */
    private String determineOverallStatus(String kafkaStatus, long dlqMessages) {
        if ("HEALTHY".equals(kafkaStatus) && dlqMessages == 0) {
            return "HEALTHY";
        } else if ("MINOR_ISSUES".equals(kafkaStatus) && dlqMessages < 10) {
            return "MINOR_ISSUES";
        } else if ("NEEDS_ATTENTION".equals(kafkaStatus) || dlqMessages >= 10) {
            return "DEGRADED";
        } else {
            return "CRITICAL";
        }
    }
}