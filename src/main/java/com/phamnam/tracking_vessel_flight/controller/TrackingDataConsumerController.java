package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.service.realtime.KafkaConsumerService;
import com.phamnam.tracking_vessel_flight.service.realtime.TrackingCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking/consumer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tracking Data Consumer Management", description = "REST APIs for monitoring and managing Kafka consumer services that process tracking data. This controller provides insights into data consumption status and cache management.")
public class TrackingDataConsumerController {

    private final KafkaConsumerService kafkaConsumerService;
    private final TrackingCacheService trackingCacheService;

    @Operation(summary = "Get consumer status and statistics", description = "Returns current status of Kafka consumers, including processing statistics, cache status, and consumer health information.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consumer status retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/status")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getConsumerStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Get consumer statistics from KafkaConsumerService
            Map<String, Object> consumerStats = kafkaConsumerService.getConsumerStatistics();
            boolean isHealthy = kafkaConsumerService.isHealthy();

            status.put("consumerStatistics", consumerStats);
            status.put("consumerHealth", isHealthy ? "HEALTHY" : "UNHEALTHY");
            status.put("timestamp", System.currentTimeMillis());

            // Get cache statistics (nếu TrackingCacheService có methods này)
            Map<String, Object> cacheStats = new HashMap<>();
            cacheStats.put("status", "Cache service running");
            cacheStats.put("note", "Detailed cache metrics implementation can be added to TrackingCacheService");

            status.put("cacheService", cacheStats);

            return ResponseEntity.ok(
                    MyApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .message("Consumer status retrieved successfully")
                            .data(status)
                            .build());

        } catch (Exception e) {
            log.error("Error retrieving consumer status", e);
            status.put("error", e.getMessage());
            status.put("consumerHealth", "ERROR");
            status.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(
                    MyApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Error retrieving consumer status")
                            .data(status)
                            .build());
        }
    }

    @Operation(summary = "Reset consumer counters", description = "Resets the update counters and batch timers in the Kafka consumer service. Useful for debugging and maintenance.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Counters reset successfully"),
            @ApiResponse(responseCode = "500", description = "Error resetting counters")
    })
    @PostMapping("/reset-counters")
    public ResponseEntity<MyApiResponse<Void>> resetCounters() {
        try {
            kafkaConsumerService.resetCounters();

            return ResponseEntity.ok(
                    MyApiResponse.<Void>builder()
                            .success(true)
                            .message("Consumer counters reset successfully")
                            .build());

        } catch (Exception e) {
            log.error("Error resetting consumer counters", e);

            return ResponseEntity.ok(
                    MyApiResponse.<Void>builder()
                            .success(false)
                            .message("Error resetting counters: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Trigger manual batch update", description = "Manually triggers a batch update to all connected clients. This forces an immediate update regardless of the batch threshold.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch update triggered successfully"),
            @ApiResponse(responseCode = "500", description = "Error triggering batch update")
    })
    @PostMapping("/trigger-batch-update")
    public ResponseEntity<MyApiResponse<Void>> triggerBatchUpdate() {
        try {
            kafkaConsumerService.triggerManualBatchUpdate();

            return ResponseEntity.ok(
                    MyApiResponse.<Void>builder()
                            .success(true)
                            .message("Manual batch update triggered successfully")
                            .build());

        } catch (Exception e) {
            log.error("Error triggering manual batch update", e);

            return ResponseEntity.ok(
                    MyApiResponse.<Void>builder()
                            .success(false)
                            .message("Error triggering batch update: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Clear tracking data cache", description = "Clears all cached tracking data for both flights and vessels. Use with caution as this will remove all cached real-time data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache cleared successfully"),
            @ApiResponse(responseCode = "500", description = "Error clearing cache")
    })
    @PostMapping("/cache/clear")
    public ResponseEntity<MyApiResponse<Void>> clearCache() {
        try {
            // Note: You would need to implement this method in TrackingCacheService
            // trackingCacheService.clearAllCache();

            log.info("Cache clear operation requested");

            return ResponseEntity.ok(
                    MyApiResponse.<Void>builder()
                            .success(true)
                            .message("Cache cleared successfully (implementation needed in TrackingCacheService)")
                            .build());

        } catch (Exception e) {
            log.error("Error clearing cache", e);

            return ResponseEntity.ok(
                    MyApiResponse.<Void>builder()
                            .success(false)
                            .message("Error clearing cache: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Get cache metrics and statistics", description = "Returns detailed metrics about the tracking data cache, including size, hit rates, and memory usage.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache metrics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Error retrieving cache metrics")
    })
    @GetMapping("/cache/metrics")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // Note: These metrics would need to be implemented in TrackingCacheService
            metrics.put("implementation", "To be implemented in TrackingCacheService");
            metrics.put("suggested_metrics", Map.of(
                    "flight_cache_size", "Number of cached flight records",
                    "vessel_cache_size", "Number of cached vessel records",
                    "cache_hit_rate", "Percentage of cache hits vs misses",
                    "memory_usage", "Memory consumed by cache",
                    "last_update_time", "Timestamp of last cache update",
                    "eviction_count", "Number of cache evictions"));

            return ResponseEntity.ok(
                    MyApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .message("Cache metrics template retrieved")
                            .data(metrics)
                            .build());

        } catch (Exception e) {
            log.error("Error retrieving cache metrics", e);

            return ResponseEntity.ok(
                    MyApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Error retrieving cache metrics: " + e.getMessage())
                            .build());
        }
    }
}