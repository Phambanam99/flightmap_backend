package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.service.kafka.RawDataTopicsConsumer;
import com.phamnam.tracking_vessel_flight.service.realtime.ConsumerBasedDataFusionService;
import com.phamnam.tracking_vessel_flight.service.realtime.RefactoredMultiSourceExternalApiService;
import com.phamnam.tracking_vessel_flight.service.realtime.SimpleDataCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simple Raw Data Test Controller
 * 
 * Simplified controller for testing the raw data topics architecture.
 * Provides basic endpoints for status checking and manual triggers.
 */
@RestController
@RequestMapping("/api/test/raw-data")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SimpleRawDataTestController {

    private final SimpleDataCollectionService dataCollectionService;
    private final ConsumerBasedDataFusionService fusionService;
    private final RefactoredMultiSourceExternalApiService refactoredService;
    private final RawDataTopicsConsumer monitoringConsumer;

    // ============================================================================
    // STATUS AND MONITORING ENDPOINTS
    // ============================================================================

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getOverallStatus() {
        log.info("📊 Getting overall raw data topics status");

        try {
            Map<String, Object> status = refactoredService.getComprehensiveStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("❌ Error getting overall status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    @GetMapping("/status/collection")
    public ResponseEntity<Map<String, Object>> getDataCollectionStatus() {
        log.info("📊 Getting data collection service status");

        try {
            Map<String, Object> status = dataCollectionService.getDataCollectionStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("❌ Error getting collection status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    @GetMapping("/status/fusion")
    public ResponseEntity<Map<String, Object>> getFusionStatus() {
        log.info("📊 Getting data fusion service status");

        try {
            Map<String, Object> status = fusionService.getFusionStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("❌ Error getting fusion status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    @GetMapping("/status/monitoring")
    public ResponseEntity<Map<String, Object>> getMonitoringStatus() {
        log.info("📊 Getting monitoring service metrics");

        try {
            Map<String, Object> metrics = monitoringConsumer.getMonitoringMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("❌ Error getting monitoring metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> performHealthCheck() {
        log.info("🏥 Performing health check");

        try {
            Map<String, Object> health = refactoredService.performHealthCheck();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("❌ Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    // ============================================================================
    // MANUAL TRIGGERS
    // ============================================================================

    @PostMapping("/trigger/collection")
    public ResponseEntity<Map<String, Object>> triggerManualCollection() {
        log.info("🚀 Triggering manual data collection");

        try {
            refactoredService.triggerManualDataCollection();
            return ResponseEntity.ok(Map.of(
                    "message", "Manual data collection triggered successfully",
                    "timestamp", System.currentTimeMillis(),
                    "status", "SUCCESS"));
        } catch (Exception e) {
            log.error("❌ Failed to trigger manual collection: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    @PostMapping("/trigger/fusion")
    public ResponseEntity<Map<String, Object>> triggerManualFusion() {
        log.info("🔄 Triggering manual data fusion");

        try {
            refactoredService.triggerManualFusion();
            return ResponseEntity.ok(Map.of(
                    "message", "Manual data fusion triggered successfully",
                    "timestamp", System.currentTimeMillis(),
                    "status", "SUCCESS"));
        } catch (Exception e) {
            log.error("❌ Failed to trigger manual fusion: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    // ============================================================================
    // DATA QUALITY AND MONITORING
    // ============================================================================

    @GetMapping("/quality/report")
    public ResponseEntity<Map<String, Object>> getDataQualityReport() {
        log.info("📊 Getting data quality report");

        try {
            Map<String, Object> report = monitoringConsumer.getDataQualityReport();
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("❌ Failed to get data quality report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    @PostMapping("/monitoring/reset")
    public ResponseEntity<Map<String, Object>> resetMonitoringMetrics() {
        log.info("🔄 Resetting monitoring metrics");

        try {
            monitoringConsumer.resetMetrics();
            return ResponseEntity.ok(Map.of(
                    "message", "Monitoring metrics reset successfully",
                    "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("❌ Failed to reset monitoring metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    @GetMapping("/monitoring/summary")
    public ResponseEntity<String> getMonitoringSummary() {
        log.info("📊 Getting monitoring summary");

        try {
            monitoringConsumer.logStatusSummary();
            return ResponseEntity.ok("Monitoring summary logged to console. Check application logs for details.");
        } catch (Exception e) {
            log.error("❌ Failed to get monitoring summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ============================================================================
    // ARCHITECTURE INFORMATION
    // ============================================================================

    @GetMapping("/architecture/diagram")
    public ResponseEntity<Map<String, Object>> getDataFlowDiagram() {
        log.info("🏗️ Getting data flow diagram");

        try {
            Map<String, Object> diagram = refactoredService.getDataFlowDiagram();
            return ResponseEntity.ok(diagram);
        } catch (Exception e) {
            log.error("❌ Failed to get data flow diagram: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    @GetMapping("/architecture/metrics")
    public ResponseEntity<Map<String, Object>> getArchitectureMetrics() {
        log.info("📊 Getting architecture metrics");

        try {
            Map<String, Object> metrics = refactoredService.getMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("❌ Failed to get architecture metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage(), "timestamp", System.currentTimeMillis()));
        }
    }

    @PostMapping("/architecture/log-status")
    public ResponseEntity<String> logArchitectureStatus() {
        log.info("📊 Logging architecture status");

        try {
            refactoredService.logArchitectureStatus();
            return ResponseEntity.ok("Architecture status logged to console. Check application logs for details.");
        } catch (Exception e) {
            log.error("❌ Failed to log architecture status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ============================================================================
    // INFORMATION ENDPOINTS
    // ============================================================================

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        return ResponseEntity.ok(Map.of(
                "systemName", "Raw Data Topics Architecture",
                "version", "1.0.0",
                "description", "Event-driven data collection and fusion system",
                "features", Map.of(
                        "rawDataPreservation", true,
                        "sourceSpecificTopics", 6,
                        "eventDrivenFusion", true,
                        "dataQualityMonitoring", true,
                        "scalableArchitecture", true),
                "endpoints", Map.of(
                        "status", "GET /api/test/raw-data/status",
                        "health", "GET /api/test/raw-data/health",
                        "collection", "POST /api/test/raw-data/trigger/collection",
                        "fusion", "POST /api/test/raw-data/trigger/fusion",
                        "quality", "GET /api/test/raw-data/quality/report",
                        "architecture", "GET /api/test/raw-data/architecture/diagram"),
                "timestamp", System.currentTimeMillis()));
    }

    @GetMapping("/sources")
    public ResponseEntity<Map<String, Object>> getDataSources() {
        return ResponseEntity.ok(Map.of(
                "aircraftSources", Map.of(
                        "flightradar24", Map.of(
                                "topic", "raw-flightradar24-data",
                                "description", "FlightRadar24 aircraft tracking data"),
                        "adsbexchange", Map.of(
                                "topic", "raw-adsbexchange-data",
                                "description", "ADS-B Exchange aircraft tracking data")),
                "vesselSources", Map.of(
                        "marinetraffic", Map.of(
                                "topic", "raw-marinetraffic-data",
                                "description", "MarineTraffic vessel tracking data"),
                        "vesselfinder", Map.of(
                                "topic", "raw-vesselfinder-data",
                                "description", "VesselFinder vessel tracking data"),
                        "chinaports", Map.of(
                                "topic", "raw-chinaports-data",
                                "description", "China Ports vessel tracking data"),
                        "marinetrafficv2", Map.of(
                                "topic", "raw-marinetrafficv2-data",
                                "description", "MarineTraffic V2 vessel tracking data")),
                "totalSources", 6,
                "timestamp", System.currentTimeMillis()));
    }
}