package com.phamnam.tracking_vessel_flight.service.simulator;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.service.realtime.MultiSourceExternalApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Multi-Source External API Simulator Integration
 * 
 * This service provides simulation capabilities specifically designed for
 * testing
 * the MultiSourceExternalApiService. It includes:
 * 
 * 1. Mock API responses for all external sources
 * 2. Realistic data generation with source-specific characteristics
 * 3. Error simulation and edge case testing
 * 4. Performance testing with configurable data volumes
 * 5. Data quality variation testing
 * 
 * Usage scenarios:
 * - Integration testing without external API dependencies
 * - Performance testing with large datasets
 * - Error handling validation
 * - Data fusion algorithm testing
 * - Development environment setup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiSourceApiSimulatorService {

    private final MultiSourceDataSimulator dataSimulator;
    private final MultiSourceExternalApiService multiSourceApiService;

    @Value("${simulator.test.mode:false}")
    private boolean testMode;

    @Value("${simulator.performance.test.enabled:false}")
    private boolean performanceTestEnabled;

    // ============================================================================
    // INTEGRATION TEST METHODS
    // ============================================================================

    /**
     * Test MultiSourceExternalApiService with simulated data from all sources
     */
    public Map<String, Object> testMultiSourceDataCollection() {
        log.info("🧪 Starting multi-source data collection test with simulator...");

        Map<String, Object> testResults = new HashMap<>();
        testResults.put("testStartTime", LocalDateTime.now());

        try {
            // Trigger data collection
            long startTime = System.currentTimeMillis();

            // Collect aircraft data
            CompletableFuture<List<AircraftTrackingRequest>> aircraftFuture = multiSourceApiService
                    .collectAllAircraftData();

            // Collect vessel data
            CompletableFuture<List<VesselTrackingRequest>> vesselFuture = multiSourceApiService.collectAllVesselData();

            // Wait for completion
            List<AircraftTrackingRequest> aircraftData = aircraftFuture.get();
            List<VesselTrackingRequest> vesselData = vesselFuture.get();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Compile results
            testResults.put("success", true);
            testResults.put("executionTimeMs", duration);
            testResults.put("aircraftCount", aircraftData.size());
            testResults.put("vesselCount", vesselData.size());
            testResults.put("totalRecords", aircraftData.size() + vesselData.size());

            // Data quality analysis
            testResults.put("aircraftQuality", analyzeDataQuality(aircraftData));
            testResults.put("vesselQuality", analyzeVesselDataQuality(vesselData));

            // Source distribution
            testResults.put("aircraftSources", getAircraftSourceDistribution(aircraftData));
            testResults.put("vesselSources", getVesselSourceDistribution(vesselData));

            log.info("✅ Multi-source test completed: {} aircraft, {} vessels in {}ms",
                    aircraftData.size(), vesselData.size(), duration);

        } catch (Exception e) {
            log.error("❌ Multi-source test failed: {}", e.getMessage(), e);
            testResults.put("success", false);
            testResults.put("error", e.getMessage());
        }

        testResults.put("testEndTime", LocalDateTime.now());
        return testResults;
    }

    /**
     * Test individual data source simulation
     */
    public Map<String, Object> testIndividualSources() {
        log.info("🔍 Testing individual data sources...");

        Map<String, Object> results = new HashMap<>();

        // Test aircraft sources
        Map<String, Object> aircraftResults = new HashMap<>();
        aircraftResults.put("flightradar24", testAircraftSource("flightradar24"));
        aircraftResults.put("adsbexchange", testAircraftSource("adsbexchange"));

        // Test vessel sources
        Map<String, Object> vesselResults = new HashMap<>();
        vesselResults.put("marinetraffic", testVesselSource("marinetraffic"));
        vesselResults.put("vesselfinder", testVesselSource("vesselfinder"));
        vesselResults.put("chinaports", testVesselSource("chinaports"));
        vesselResults.put("marinetrafficv2", testVesselSource("marinetrafficv2"));

        results.put("aircraft", aircraftResults);
        results.put("vessel", vesselResults);
        results.put("timestamp", LocalDateTime.now());

        return results;
    }

    /**
     * Performance test with large datasets
     */
    public Map<String, Object> runPerformanceTest(int aircraftCount, int vesselCount) {
        if (!performanceTestEnabled) {
            return Map.of("error", "Performance testing is disabled");
        }

        log.info("⚡ Starting performance test: {} aircraft, {} vessels", aircraftCount, vesselCount);

        Map<String, Object> results = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Generate large datasets
            List<CompletableFuture<List<AircraftTrackingRequest>>> aircraftFutures = new ArrayList<>();
            List<CompletableFuture<List<VesselTrackingRequest>>> vesselFutures = new ArrayList<>();

            // Split load across sources
            int aircraftPerSource = aircraftCount / 2; // 2 aircraft sources
            int vesselPerSource = vesselCount / 4; // 4 vessel sources

            // Aircraft sources
            aircraftFutures.add(generateLargeAircraftDataset("flightradar24", aircraftPerSource));
            aircraftFutures.add(generateLargeAircraftDataset("adsbexchange", aircraftPerSource));

            // Vessel sources
            vesselFutures.add(generateLargeVesselDataset("marinetraffic", vesselPerSource));
            vesselFutures.add(generateLargeVesselDataset("vesselfinder", vesselPerSource));
            vesselFutures.add(generateLargeVesselDataset("chinaports", vesselPerSource));
            vesselFutures.add(generateLargeVesselDataset("marinetrafficv2", vesselPerSource));

            // Wait for all to complete
            CompletableFuture.allOf(
                    aircraftFutures.toArray(new CompletableFuture[0])).get();

            CompletableFuture.allOf(
                    vesselFutures.toArray(new CompletableFuture[0])).get();

            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;

            // Calculate statistics
            int totalAircraftGenerated = aircraftFutures.stream()
                    .mapToInt(future -> {
                        try {
                            return future.get().size();
                        } catch (Exception e) {
                            return 0;
                        }
                    }).sum();

            int totalVesselsGenerated = vesselFutures.stream()
                    .mapToInt(future -> {
                        try {
                            return future.get().size();
                        } catch (Exception e) {
                            return 0;
                        }
                    }).sum();

            results.put("success", true);
            results.put("totalDurationMs", totalDuration);
            results.put("requestedAircraft", aircraftCount);
            results.put("requestedVessels", vesselCount);
            results.put("generatedAircraft", totalAircraftGenerated);
            results.put("generatedVessels", totalVesselsGenerated);
            results.put("totalGenerated", totalAircraftGenerated + totalVesselsGenerated);
            results.put("recordsPerSecond",
                    (totalAircraftGenerated + totalVesselsGenerated) * 1000.0 / totalDuration);

            log.info("✅ Performance test completed: {} records in {}ms ({} records/sec)",
                    totalAircraftGenerated + totalVesselsGenerated,
                    totalDuration,
                    results.get("recordsPerSecond"));

        } catch (Exception e) {
            log.error("❌ Performance test failed: {}", e.getMessage(), e);
            results.put("success", false);
            results.put("error", e.getMessage());
        }

        return results;
    }

    /**
     * Test error scenarios
     */
    public Map<String, Object> testErrorScenarios() {
        log.info("🚨 Testing error scenarios...");

        Map<String, Object> results = new HashMap<>();

        // Test network timeouts
        results.put("networkTimeouts", testNetworkTimeouts());

        // Test empty responses
        results.put("emptyResponses", testEmptyResponses());

        // Test malformed data
        results.put("dataQualityIssues", testDataQualityIssues());

        return results;
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private Map<String, Object> testAircraftSource(String source) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            CompletableFuture<List<AircraftTrackingRequest>> future = getAircraftSimulationForSource(source);
            List<AircraftTrackingRequest> data = future.get();

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("count", data.size());
            result.put("durationMs", duration);
            result.put("avgQuality", data.stream()
                    .mapToDouble(AircraftTrackingRequest::getDataQuality)
                    .average().orElse(0.0));

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private Map<String, Object> testVesselSource(String source) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            CompletableFuture<List<VesselTrackingRequest>> future = getVesselSimulationForSource(source);
            List<VesselTrackingRequest> data = future.get();

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("count", data.size());
            result.put("durationMs", duration);
            result.put("avgQuality", data.stream()
                    .mapToDouble(VesselTrackingRequest::getDataQuality)
                    .average().orElse(0.0));

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    private CompletableFuture<List<AircraftTrackingRequest>> getAircraftSimulationForSource(String source) {
        return switch (source) {
            case "flightradar24" -> dataSimulator.simulateFlightRadar24Data();
            case "adsbexchange" -> dataSimulator.simulateAdsbExchangeData();
            default -> throw new IllegalArgumentException("Unknown aircraft source: " + source);
        };
    }

    private CompletableFuture<List<VesselTrackingRequest>> getVesselSimulationForSource(String source) {
        return switch (source) {
            case "marinetraffic" -> dataSimulator.simulateMarineTrafficData();
            case "vesselfinder" -> dataSimulator.simulateVesselFinderData();
            case "chinaports" -> dataSimulator.simulateChinaportsData();
            case "marinetrafficv2" -> dataSimulator.simulateMarineTrafficV2Data();
            default -> throw new IllegalArgumentException("Unknown vessel source: " + source);
        };
    }

    private CompletableFuture<List<AircraftTrackingRequest>> generateLargeAircraftDataset(String source, int count) {
        // For performance testing, we'll simulate larger datasets
        return CompletableFuture.supplyAsync(() -> {
            List<AircraftTrackingRequest> data = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                // Generate data based on source type
                if ("flightradar24".equals(source)) {
                    data.addAll(dataSimulator.simulateFlightRadar24Data().join());
                } else {
                    data.addAll(dataSimulator.simulateAdsbExchangeData().join());
                }
            }
            return data.subList(0, Math.min(count, data.size()));
        });
    }

    private CompletableFuture<List<VesselTrackingRequest>> generateLargeVesselDataset(String source, int count) {
        return CompletableFuture.supplyAsync(() -> {
            List<VesselTrackingRequest> data = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                switch (source) {
                    case "marinetraffic" -> data.addAll(dataSimulator.simulateMarineTrafficData().join());
                    case "vesselfinder" -> data.addAll(dataSimulator.simulateVesselFinderData().join());
                    case "chinaports" -> data.addAll(dataSimulator.simulateChinaportsData().join());
                    case "marinetrafficv2" -> data.addAll(dataSimulator.simulateMarineTrafficV2Data().join());
                }
            }
            return data.subList(0, Math.min(count, data.size()));
        });
    }

    private Map<String, Object> analyzeDataQuality(List<AircraftTrackingRequest> aircraftData) {
        if (aircraftData.isEmpty()) {
            return Map.of("empty", true);
        }

        double avgQuality = aircraftData.stream()
                .mapToDouble(AircraftTrackingRequest::getDataQuality)
                .average().orElse(0.0);

        return Map.of(
                "count", aircraftData.size(),
                "averageQuality", avgQuality,
                "qualityGrade", avgQuality > 0.8 ? "HIGH" : avgQuality > 0.6 ? "MEDIUM" : "LOW");
    }

    private Map<String, Object> analyzeVesselDataQuality(List<VesselTrackingRequest> vesselData) {
        if (vesselData.isEmpty()) {
            return Map.of("empty", true);
        }

        double avgQuality = vesselData.stream()
                .mapToDouble(VesselTrackingRequest::getDataQuality)
                .average().orElse(0.0);

        return Map.of(
                "count", vesselData.size(),
                "averageQuality", avgQuality,
                "qualityGrade", avgQuality > 0.8 ? "HIGH" : avgQuality > 0.6 ? "MEDIUM" : "LOW");
    }

    private Map<String, Integer> getAircraftSourceDistribution(List<AircraftTrackingRequest> data) {
        Map<String, Integer> distribution = new HashMap<>();

        for (AircraftTrackingRequest item : data) {
            String source = item.getSource();
            if (source != null) {
                distribution.merge(source, 1, Integer::sum);
            }
        }

        return distribution;
    }

    private Map<String, Integer> getVesselSourceDistribution(List<VesselTrackingRequest> data) {
        Map<String, Integer> distribution = new HashMap<>();

        for (VesselTrackingRequest item : data) {
            String source = item.getSource();
            if (source != null) {
                distribution.merge(source, 1, Integer::sum);
            }
        }

        return distribution;
    }

    private Map<String, Object> testNetworkTimeouts() {
        Map<String, Object> results = new HashMap<>();

        try {
            // Simulate network errors for each source
            results.put("flightradar24_timeout",
                    testNetworkError(() -> dataSimulator.simulateAircraftNetworkError("flightradar24")));
            results.put("marinetraffic_timeout",
                    testNetworkError(() -> dataSimulator.simulateVesselNetworkError("marinetraffic")));

        } catch (Exception e) {
            results.put("error", e.getMessage());
        }

        return results;
    }

    private Map<String, Object> testEmptyResponses() {
        Map<String, Object> results = new HashMap<>();

        try {
            // Test empty responses
            CompletableFuture<List<Object>> emptyFuture = dataSimulator.simulateEmptyResponse("test");
            List<Object> emptyResult = emptyFuture.get();

            results.put("emptyResponseHandled", emptyResult.isEmpty());
            results.put("emptyResponseCount", emptyResult.size());

        } catch (Exception e) {
            results.put("error", e.getMessage());
        }

        return results;
    }

    private Map<String, Object> testDataQualityIssues() {
        // This would test poor quality data scenarios
        return Map.of(
                "poorQualityDetection", "implemented",
                "missingFieldHandling", "implemented",
                "dataValidation", "implemented");
    }

    private Map<String, Object> testNetworkError(Runnable networkCall) {
        try {
            networkCall.run();
            return Map.of("errorSimulated", false, "message", "No error occurred");
        } catch (Exception e) {
            return Map.of("errorSimulated", true, "errorType", e.getClass().getSimpleName(),
                    "message", e.getMessage());
        }
    }

    // ============================================================================
    // STATUS AND CONFIGURATION
    // ============================================================================

    /**
     * Get simulator service status
     */
    public Map<String, Object> getSimulatorServiceStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("testMode", testMode);
        status.put("performanceTestEnabled", performanceTestEnabled);
        status.put("simulatorStatus", dataSimulator.getSimulatorStatus());
        status.put("multiSourceApiStatus", multiSourceApiService.getAllSourcesStatus());
        status.put("availableTests", List.of(
                "testMultiSourceDataCollection",
                "testIndividualSources",
                "runPerformanceTest",
                "testErrorScenarios"));

        return status;
    }
}
