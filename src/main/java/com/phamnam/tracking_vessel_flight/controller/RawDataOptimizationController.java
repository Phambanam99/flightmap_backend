package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.service.realtime.RawDataCompressionService;
import com.phamnam.tracking_vessel_flight.service.realtime.RawDataFilteringService;
import com.phamnam.tracking_vessel_flight.service.realtime.RawDataRetentionService;
import com.phamnam.tracking_vessel_flight.service.realtime.RawDataStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/raw-data")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Raw Data Optimization", description = "APIs for managing raw data filtering, compression, and retention")
public class RawDataOptimizationController {

    private final RawDataFilteringService filteringService;
    private final RawDataCompressionService compressionService;
    private final RawDataRetentionService retentionService;
    private final RawDataStorageService storageService;

    /**
     * Get raw data filtering statistics
     */
    @GetMapping("/filtering/stats")
    @Operation(summary = "Get filtering statistics", description = "Get statistics about raw data filtering including sampling and smart filtering")
    public ResponseEntity<MyApiResponse<RawDataFilteringService.FilteringStats>> getFilteringStats() {
        try {
            RawDataFilteringService.FilteringStats stats = filteringService.getFilteringStats();
            return ResponseEntity.ok(MyApiResponse.success(stats, "Filtering statistics retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving filtering stats: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to retrieve filtering statistics: " + e.getMessage()));
        }
    }

    /**
     * Clear filtering cache
     */
    @PostMapping("/filtering/clear-cache")
    @Operation(summary = "Clear filtering cache", description = "Clear the cached data used for smart filtering comparisons")
    public ResponseEntity<MyApiResponse<String>> clearFilteringCache() {
        try {
            filteringService.clearCache();
            return ResponseEntity.ok(MyApiResponse.success("Cache cleared", "Filtering cache cleared successfully"));
        } catch (Exception e) {
            log.error("Error clearing filtering cache: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to clear filtering cache: " + e.getMessage()));
        }
    }

    /**
     * Get compression statistics
     */
    @GetMapping("/compression/stats")
    @Operation(summary = "Get compression statistics", description = "Get statistics about raw data compression including ratios and savings")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getCompressionStats() {
        try {
            Map<String, Object> stats = compressionService.getCompressionStats();
            return ResponseEntity.ok(MyApiResponse.success(stats, "Compression statistics retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving compression stats: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to retrieve compression statistics: " + e.getMessage()));
        }
    }

    /**
     * Test compression with sample data
     */
    @PostMapping("/compression/test")
    @Operation(summary = "Test compression", description = "Test compression algorithm with sample data")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> testCompression(
            @Parameter(description = "Sample data to test compression on") @RequestBody String sampleData) {
        try {
            Map<String, Object> result = compressionService.testCompression(sampleData);
            return ResponseEntity.ok(MyApiResponse.success(result, "Compression test completed successfully"));
        } catch (Exception e) {
            log.error("Error testing compression: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to test compression: " + e.getMessage()));
        }
    }

    /**
     * Reset compression statistics
     */
    @PostMapping("/compression/reset-stats")
    @Operation(summary = "Reset compression statistics", description = "Reset all compression statistics to zero")
    public ResponseEntity<MyApiResponse<String>> resetCompressionStats() {
        try {
            compressionService.resetStats();
            return ResponseEntity.ok(MyApiResponse.success("Stats reset", "Compression statistics reset successfully"));
        } catch (Exception e) {
            log.error("Error resetting compression stats: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to reset compression statistics: " + e.getMessage()));
        }
    }

    /**
     * Get retention statistics
     */
    @GetMapping("/retention/stats")
    @Operation(summary = "Get retention statistics", description = "Get statistics about raw data retention and cleanup operations")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getRetentionStats() {
        try {
            Map<String, Object> stats = retentionService.getRetentionStats();
            return ResponseEntity.ok(MyApiResponse.success(stats, "Retention statistics retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving retention stats: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to retrieve retention statistics: " + e.getMessage()));
        }
    }

    /**
     * Get cleanup preview
     */
    @GetMapping("/retention/preview")
    @Operation(summary = "Get cleanup preview", description = "Preview what records would be deleted in the next cleanup operation")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getCleanupPreview() {
        try {
            Map<String, Object> preview = retentionService.getCleanupPreview();
            return ResponseEntity.ok(MyApiResponse.success(preview, "Cleanup preview generated successfully"));
        } catch (Exception e) {
            log.error("Error generating cleanup preview: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to generate cleanup preview: " + e.getMessage()));
        }
    }

    /**
     * Trigger manual cleanup
     */
    @PostMapping("/retention/cleanup")
    @Operation(summary = "Trigger manual cleanup", description = "Manually trigger the raw data cleanup process")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> triggerCleanup() {
        try {
            long deletedRecords = retentionService.performCleanup();
            Map<String, Object> result = Map.of(
                    "deletedRecords", deletedRecords,
                    "message", "Manual cleanup completed successfully");
            return ResponseEntity.ok(MyApiResponse.success(result, "Cleanup completed successfully"));
        } catch (Exception e) {
            log.error("Error during manual cleanup: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to perform cleanup: " + e.getMessage()));
        }
    }

    /**
     * Force cleanup by data source
     */
    @PostMapping("/retention/cleanup/{dataSource}")
    @Operation(summary = "Force cleanup by source", description = "Force cleanup of specific data source older than specified days")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> forceCleanupBySource(
            @Parameter(description = "Data source name") @PathVariable String dataSource,
            @Parameter(description = "Delete records older than this many days") @RequestParam(defaultValue = "7") int days) {
        try {
            long deletedRecords = retentionService.forceCleanupBySource(dataSource, days);
            Map<String, Object> result = Map.of(
                    "dataSource", dataSource,
                    "days", days,
                    "deletedRecords", deletedRecords,
                    "message", "Force cleanup completed successfully");
            return ResponseEntity.ok(MyApiResponse.success(result, "Force cleanup completed successfully"));
        } catch (Exception e) {
            log.error("Error during force cleanup for source {}: {}", dataSource, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to perform force cleanup: " + e.getMessage()));
        }
    }

    /**
     * Get storage information
     */
    @GetMapping("/storage/info")
    @Operation(summary = "Get storage information", description = "Get information about raw data storage sizes and record counts")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getStorageInfo() {
        try {
            Map<String, Object> info = retentionService.getStorageInfo();
            return ResponseEntity.ok(MyApiResponse.success(info, "Storage information retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving storage info: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to retrieve storage information: " + e.getMessage()));
        }
    }

    /**
     * Reset retention statistics
     */
    @PostMapping("/retention/reset-stats")
    @Operation(summary = "Reset retention statistics", description = "Reset all retention statistics to zero")
    public ResponseEntity<MyApiResponse<String>> resetRetentionStats() {
        try {
            retentionService.resetStats();
            return ResponseEntity.ok(MyApiResponse.success("Stats reset", "Retention statistics reset successfully"));
        } catch (Exception e) {
            log.error("Error resetting retention stats: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to reset retention statistics: " + e.getMessage()));
        }
    }

    /**
     * Get overall optimization status
     */
    @GetMapping("/optimization/status")
    @Operation(summary = "Get optimization status", description = "Get overall status of all raw data optimization features")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getOptimizationStatus() {
        try {
            Map<String, Object> status = Map.of(
                    "filtering", (Object) filteringService.getFilteringStats(),
                    "compression", compressionService.getCompressionStats(),
                    "retention", retentionService.getRetentionStats(),
                    "storage", retentionService.getStorageInfo());
            return ResponseEntity.ok(MyApiResponse.success(status, "Optimization status retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving optimization status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to retrieve optimization status: " + e.getMessage()));
        }
    }

    /**
     * Get health status of raw data optimization
     */
    @GetMapping("/optimization/health")
    @Operation(summary = "Get optimization health status", description = "Get health status of raw data optimization services")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getHealthStatus() {
        try {
            Map<String, Object> health = Map.of(
                    "filteringService", "healthy",
                    "compressionService", "healthy",
                    "retentionService", "healthy",
                    "storageService", "healthy",
                    "timestamp", System.currentTimeMillis(),
                    "status", "UP");
            return ResponseEntity.ok(MyApiResponse.success(health, "Health status retrieved successfully"));
        } catch (Exception e) {
            log.error("Error retrieving health status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.error("Failed to retrieve health status: " + e.getMessage()));
        }
    }
}