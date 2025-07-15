package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.service.simulator.MultiSourceApiSimulatorService;
import com.phamnam.tracking_vessel_flight.service.simulator.MultiSourceDataSimulator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Multi-Source Data Simulator Controller
 * 
 * REST API endpoints for testing and simulating data from multiple external
 * sources.
 * This controller provides comprehensive testing capabilities for the
 * MultiSourceExternalApiService.
 * 
 * Available endpoints:
 * - Simulator status and configuration
 * - Individual source testing
 * - Integration testing
 * - Performance testing
 * - Error scenario testing
 * - Comprehensive test data generation
 */
@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Multi-Source Data Simulator", description = "APIs for testing multi-source data collection with simulated data")
public class MultiSourceSimulatorController {

    private final MultiSourceDataSimulator dataSimulator;
    private final MultiSourceApiSimulatorService simulatorService;

    // ============================================================================
    // SIMULATOR STATUS AND CONFIGURATION
    // ============================================================================

    @Operation(summary = "Get simulator status", description = "Retrieve current simulator configuration and status information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulator status retrieved successfully")
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSimulatorStatus() {
        log.info("📊 Getting simulator status...");

        try {
            Map<String, Object> status = simulatorService.getSimulatorServiceStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("❌ Error getting simulator status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Get data simulator configuration", description = "Retrieve configuration details for the data simulator component")
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getSimulatorConfig() {
        try {
            Map<String, Object> config = dataSimulator.getSimulatorStatus();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("❌ Error getting simulator config: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================================
    // COMPREHENSIVE TESTING
    // ============================================================================

    @Operation(summary = "Test multi-source data collection", description = "Run a comprehensive test of the MultiSourceExternalApiService with simulated data from all sources")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Multi-source test completed successfully"),
            @ApiResponse(responseCode = "500", description = "Test execution failed")
    })
    @PostMapping("/test/multi-source")
    public ResponseEntity<Map<String, Object>> testMultiSourceDataCollection() {
        log.info("🧪 Starting comprehensive multi-source data collection test...");

        try {
            Map<String, Object> results = simulatorService.testMultiSourceDataCollection();

            if (Boolean.TRUE.equals(results.get("success"))) {
                log.info("✅ Multi-source test completed successfully");
                return ResponseEntity.ok(results);
            } else {
                log.warn("⚠️ Multi-source test completed with issues");
                return ResponseEntity.status(500).body(results);
            }
        } catch (Exception e) {
            log.error("❌ Multi-source test failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Test individual data sources", description = "Test each data source individually to validate source-specific behavior")
    @PostMapping("/test/individual-sources")
    public ResponseEntity<Map<String, Object>> testIndividualSources() {
        log.info("🔍 Testing individual data sources...");

        try {
            Map<String, Object> results = simulatorService.testIndividualSources();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("❌ Individual sources test failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================================
    // PERFORMANCE TESTING
    // ============================================================================

    @Operation(summary = "Run performance test", description = "Execute a performance test with configurable data volumes to assess system capacity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Performance test completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid test parameters"),
            @ApiResponse(responseCode = "403", description = "Performance testing is disabled"),
            @ApiResponse(responseCode = "500", description = "Performance test execution failed")
    })
    @PostMapping("/test/performance")
    public ResponseEntity<Map<String, Object>> runPerformanceTest(
            @Parameter(description = "Number of aircraft records to generate", example = "1000") @RequestParam(defaultValue = "100") int aircraftCount,

            @Parameter(description = "Number of vessel records to generate", example = "2000") @RequestParam(defaultValue = "200") int vesselCount) {

        log.info("⚡ Starting performance test: {} aircraft, {} vessels", aircraftCount, vesselCount);

        // Validate parameters
        if (aircraftCount < 0 || vesselCount < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Count parameters must be non-negative"));
        }

        if (aircraftCount > 10000 || vesselCount > 10000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Count parameters too large (max 10,000 each)"));
        }

        try {
            Map<String, Object> results = simulatorService.runPerformanceTest(aircraftCount, vesselCount);

            if (results.containsKey("error") && results.get("error").toString().contains("disabled")) {
                return ResponseEntity.status(403).body(results);
            }

            if (Boolean.TRUE.equals(results.get("success"))) {
                log.info("✅ Performance test completed successfully");
                return ResponseEntity.ok(results);
            } else {
                log.warn("⚠️ Performance test completed with issues");
                return ResponseEntity.status(500).body(results);
            }
        } catch (Exception e) {
            log.error("❌ Performance test failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ============================================================================
    // ERROR SCENARIO TESTING
    // ============================================================================

    @Operation(summary = "Test error scenarios", description = "Test various error conditions including network timeouts, empty responses, and data quality issues")
    @PostMapping("/test/error-scenarios")
    public ResponseEntity<Map<String, Object>> testErrorScenarios() {
        log.info("🚨 Testing error scenarios...");

        try {
            Map<String, Object> results = simulatorService.testErrorScenarios();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("❌ Error scenario testing failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================================
    // DATA GENERATION
    // ============================================================================

    @Operation(summary = "Generate comprehensive test data", description = "Generate a complete set of test data from all sources for development and testing purposes")
    @PostMapping("/generate/comprehensive")
    public ResponseEntity<Map<String, Object>> generateComprehensiveTestData() {
        log.info("🎯 Generating comprehensive test data...");

        try {
            Map<String, Object> testData = dataSimulator.generateComprehensiveTestData();

            if (testData.containsKey("error")) {
                return ResponseEntity.internalServerError().body(testData);
            }

            return ResponseEntity.ok(testData);
        } catch (Exception e) {
            log.error("❌ Comprehensive test data generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Generate sample aircraft data", description = "Generate sample aircraft data from FlightRadar24 source")
    @PostMapping("/generate/aircraft/flightradar24")
    public ResponseEntity<Object> generateFlightRadar24Data() {
        try {
            return ResponseEntity.ok(dataSimulator.simulateFlightRadar24Data().get());
        } catch (Exception e) {
            log.error("❌ FlightRadar24 data generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Generate sample aircraft data", description = "Generate sample aircraft data from ADS-B Exchange source")
    @PostMapping("/generate/aircraft/adsbexchange")
    public ResponseEntity<Object> generateAdsbExchangeData() {
        try {
            return ResponseEntity.ok(dataSimulator.simulateAdsbExchangeData().get());
        } catch (Exception e) {
            log.error("❌ ADS-B Exchange data generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Generate sample vessel data", description = "Generate sample vessel data from MarineTraffic source")
    @PostMapping("/generate/vessel/marinetraffic")
    public ResponseEntity<Object> generateMarineTrafficData() {
        try {
            return ResponseEntity.ok(dataSimulator.simulateMarineTrafficData().get());
        } catch (Exception e) {
            log.error("❌ MarineTraffic data generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Generate sample vessel data", description = "Generate sample vessel data from VesselFinder source")
    @PostMapping("/generate/vessel/vesselfinder")
    public ResponseEntity<Object> generateVesselFinderData() {
        try {
            return ResponseEntity.ok(dataSimulator.simulateVesselFinderData().get());
        } catch (Exception e) {
            log.error("❌ VesselFinder data generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Generate sample vessel data", description = "Generate sample vessel data from Chinaports source")
    @PostMapping("/generate/vessel/chinaports")
    public ResponseEntity<Object> generateChinaportsData() {
        try {
            return ResponseEntity.ok(dataSimulator.simulateChinaportsData().get());
        } catch (Exception e) {
            log.error("❌ Chinaports data generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Generate sample vessel data", description = "Generate sample vessel data from MarineTrafficV2 source")
    @PostMapping("/generate/vessel/marinetrafficv2")
    public ResponseEntity<Object> generateMarineTrafficV2Data() {
        try {
            return ResponseEntity.ok(dataSimulator.simulateMarineTrafficV2Data().get());
        } catch (Exception e) {
            log.error("❌ MarineTrafficV2 data generation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================================
    // UTILITY ENDPOINTS
    // ============================================================================

    @Operation(summary = "Quick health check", description = "Quick health check for the simulator services")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = Map.of(
                    "status", "UP",
                    "timestamp", System.currentTimeMillis(),
                    "simulator", "MultiSourceDataSimulator",
                    "version", "1.0.0");
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }

    @Operation(summary = "Get available test endpoints", description = "List all available simulator test endpoints and their descriptions")
    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getAvailableEndpoints() {
        Map<String, Object> endpoints = Map.of(
                "status", Map.of("method", "GET", "path", "/api/simulator/status",
                        "description", "Get simulator status and configuration"),
                "multiSourceTest", Map.of("method", "POST", "path", "/api/simulator/test/multi-source",
                        "description", "Test multi-source data collection"),
                "individualTest", Map.of("method", "POST", "path", "/api/simulator/test/individual-sources",
                        "description", "Test individual data sources"),
                "performanceTest", Map.of("method", "POST", "path", "/api/simulator/test/performance",
                        "description", "Run performance test with custom parameters"),
                "errorTest", Map.of("method", "POST", "path", "/api/simulator/test/error-scenarios",
                        "description", "Test error handling scenarios"),
                "generateData", Map.of("method", "POST", "path", "/api/simulator/generate/comprehensive",
                        "description", "Generate comprehensive test data"),
                "health", Map.of("method", "GET", "path", "/api/simulator/health",
                        "description", "Health check for simulator services"));

        return ResponseEntity.ok(Map.of(
                "availableEndpoints", endpoints,
                "totalEndpoints", endpoints.size(),
                "documentation", "See Swagger UI for detailed API documentation"));
    }
}
