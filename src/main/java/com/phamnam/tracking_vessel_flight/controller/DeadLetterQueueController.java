package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.service.kafka.DeadLetterQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/monitoring/dead-letter-queue")
@Tag(name = "Dead Letter Queue Management", description = "API endpoints for monitoring and managing dead letter queue")
public class DeadLetterQueueController {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueController.class);

    @Autowired
    private DeadLetterQueueService deadLetterQueueService;

    @GetMapping("/metrics")
    @Operation(summary = "Get dead letter queue metrics", description = "Retrieve current metrics including message counts and error statistics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        try {
            Map<String, Object> metrics = deadLetterQueueService.getMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error getting dead letter queue metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reset-metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset dead letter queue metrics", description = "Reset all metrics counters (admin only)")
    public ResponseEntity<String> resetMetrics() {
        try {
            deadLetterQueueService.resetMetrics();
            logger.info("Dead letter queue metrics reset by admin");
            return ResponseEntity.ok("Dead letter queue metrics reset successfully");
        } catch (Exception e) {
            logger.error("Error resetting dead letter queue metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to reset metrics");
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Check dead letter queue health", description = "Get health status of the dead letter queue system")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try {
            Map<String, Object> metrics = deadLetterQueueService.getMetrics();

            // Simple health check logic
            long errorCount = (Long) metrics.get("totalErrorCount");
            String status = errorCount > 1000 ? "DEGRADED" : "HEALTHY";

            return ResponseEntity.ok(Map.of(
                    "status", status,
                    "totalErrors", errorCount,
                    "deadLetterMessages", metrics.get("deadLetterMessageCount"),
                    "timestamp", metrics.get("lastUpdated")));
        } catch (Exception e) {
            logger.error("Error getting dead letter queue health: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "status", "ERROR",
                    "message", "Unable to retrieve health status"));
        }
    }
}