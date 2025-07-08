package com.phamnam.tracking_vessel_flight.service.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Refactored Multi-Source External API Service
 * 
 * This service now acts as a coordinator and status provider for the new data
 * collection architecture:
 * 1. Uses SimpleDataCollectionService for raw data collection
 * 2. Delegates fusion to ConsumerBasedDataFusionService
 * 3. Provides status and monitoring capabilities
 * 4. No longer performs direct API calls or scheduled collection (delegated to
 * other services)
 * 
 * New Data Flow:
 * SimpleDataCollectionService -> Raw Kafka Topics ->
 * ConsumerBasedDataFusionService -> Processed Topics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefactoredMultiSourceExternalApiService {

    private final SimpleDataCollectionService dataCollectionService;
    private final ConsumerBasedDataFusionService fusionService;

    // Configuration values
    @Value("${external.api.data-collection.poll-interval:30000}")
    private long dataCollectionPollInterval;

    @Value("${external.api.flightradar24.poll-interval:30000}")
    private long flightradar24PollInterval;

    @Value("${external.api.adsbexchange.poll-interval:30000}")
    private long adsbexchangePollInterval;

    @Value("${external.api.marinetraffic.poll-interval:60000}")
    private long marinetrafficPollInterval;

    @Value("${external.api.vesselfinder.poll-interval:60000}")
    private long vesselfinderPollInterval;

    @Value("${external.api.chinaports.poll-interval:60000}")
    private long chinaportsPollInterval;

    @Value("${external.api.marinetrafficv2.poll-interval:45000}")
    private long marinetrafficv2PollInterval;

    /**
     * Trigger manual data collection from all sources
     * This manually triggers the SimpleDataCollectionService to collect data
     */
    public void triggerManualDataCollection() {
        log.info("🚀 Triggering manual data collection from all sources...");

        try {
            // Trigger aircraft data collection
            dataCollectionService.collectFlightRadar24Data();
            dataCollectionService.collectAdsbExchangeData();

            // Trigger vessel data collection
            dataCollectionService.collectMarineTrafficData();
            dataCollectionService.collectVesselFinderData();
            dataCollectionService.collectChinaportsData();
            dataCollectionService.collectMarineTrafficV2Data();

            log.info("✅ Manual data collection triggered for all sources");

        } catch (Exception e) {
            log.error("❌ Error during manual data collection trigger: {}", e.getMessage(), e);
        }
    }

    /**
     * Trigger manual data fusion
     * This manually triggers the fusion service to process buffered data
     */
    public void triggerManualFusion() {
        log.info("🔄 Triggering manual data fusion...");

        try {
            fusionService.triggerAircraftFusion();
            fusionService.triggerVesselFusion();

            log.info("✅ Manual data fusion triggered");

        } catch (Exception e) {
            log.error("❌ Error during manual fusion trigger: {}", e.getMessage(), e);
        }
    }

    /**
     * Get comprehensive status of the new data collection architecture
     */
    public Map<String, Object> getComprehensiveStatus() {
        Map<String, Object> status = new HashMap<>();

        // Data collection service status
        status.put("dataCollection", dataCollectionService.getDataCollectionStatus());

        // Fusion service status
        status.put("dataFusion", fusionService.getFusionStatus());

        // Configuration status
        status.put("configuration", getConfigurationStatus());

        // Architecture info
        status.put("architecture", Map.of(
                "type", "Event-Driven Raw Data Topics",
                "dataFlow",
                "SimpleDataCollectionService -> Kafka Raw Topics -> ConsumerBasedDataFusionService -> Processed Topics",
                "benefits", List.of(
                        "Raw data preservation for AI/ML",
                        "Better scalability and fault tolerance",
                        "Independent processing streams",
                        "Data replay capabilities",
                        "Multiple consumers support")));

        return status;
    }

    /**
     * Get status of all data sources in the new architecture
     */
    public Map<String, Object> getAllSourcesStatus() {
        Map<String, Object> status = new HashMap<>();

        // Data collection status
        status.put("collectionService", dataCollectionService.getDataCollectionStatus());

        // Source configurations
        status.put("sourceConfig", Map.of(
                "aircraftSources", Map.of(
                        "flightradar24", Map.of("interval", flightradar24PollInterval, "enabled", true),
                        "adsbexchange", Map.of("interval", adsbexchangePollInterval, "enabled", true)),
                "vesselSources", Map.of(
                        "marinetraffic", Map.of("interval", marinetrafficPollInterval, "enabled", true),
                        "vesselfinder", Map.of("interval", vesselfinderPollInterval, "enabled", true),
                        "chinaports", Map.of("interval", chinaportsPollInterval, "enabled", true),
                        "marinetrafficv2", Map.of("interval", marinetrafficv2PollInterval, "enabled", true))));

        // Raw data topics info
        status.put("rawDataTopics", Map.of(
                "aircraft", List.of("raw-flightradar24-data", "raw-adsbexchange-data"),
                "vessel", List.of("raw-marinetraffic-data", "raw-vesselfinder-data",
                        "raw-chinaports-data", "raw-marinetrafficv2-data"),
                "totalTopics", 6));

        // Fusion status
        status.put("fusion", fusionService.getFusionStatus());

        return status;
    }

    /**
     * Get configuration status
     */
    public Map<String, Object> getConfigurationStatus() {
        return Map.of(
                "pollIntervals", getPollIntervalStatus(),
                "architecture", "Event-Driven with Raw Data Topics",
                "scheduledCollection", "Handled by SimpleDataCollectionService",
                "dataFusion", "Handled by ConsumerBasedDataFusionService",
                "rawDataPreservation", true,
                "kafkaTopics", 6);
    }

    /**
     * Get poll interval configurations
     */
    public Map<String, Object> getPollIntervalStatus() {
        return Map.of(
                "dataCollectionInterval", dataCollectionPollInterval,
                "flightradar24Interval", flightradar24PollInterval,
                "adsbexchangeInterval", adsbexchangePollInterval,
                "marinetrafficInterval", marinetrafficPollInterval,
                "vesselfinderInterval", vesselfinderPollInterval,
                "chinaportsInterval", chinaportsPollInterval,
                "marinetrafficv2Interval", marinetrafficv2PollInterval,
                "unit", "milliseconds");
    }

    /**
     * Log current architecture and configuration status
     */
    public void logArchitectureStatus() {
        log.info("🏗️ New Multi-Source Data Collection Architecture:");
        log.info("  📊 Raw Data Topics: 6 source-specific topics");
        log.info("  🔄 Data Collection: SimpleDataCollectionService (scheduled)");
        log.info("  🔗 Data Fusion: ConsumerBasedDataFusionService (event-driven)");
        log.info("  ✈️ Aircraft Sources:");
        log.info("    - FlightRadar24: {}ms -> raw-flightradar24-data", flightradar24PollInterval);
        log.info("    - ADS-B Exchange: {}ms -> raw-adsbexchange-data", adsbexchangePollInterval);
        log.info("  🚢 Vessel Sources:");
        log.info("    - MarineTraffic: {}ms -> raw-marinetraffic-data", marinetrafficPollInterval);
        log.info("    - VesselFinder: {}ms -> raw-vesselfinder-data", vesselfinderPollInterval);
        log.info("    - Chinaports: {}ms -> raw-chinaports-data", chinaportsPollInterval);
        log.info("    - MarineTrafficV2: {}ms -> raw-marinetrafficv2-data", marinetrafficv2PollInterval);
        log.info("  🎯 Benefits: Raw data preservation, Better scalability, Event-driven processing");
    }

    /**
     * Get data flow diagram representation
     */
    public Map<String, Object> getDataFlowDiagram() {
        return Map.of(
                "step1", Map.of(
                        "service", "SimpleDataCollectionService",
                        "action", "Scheduled API calls every 30-60s",
                        "output", "Raw data to source-specific Kafka topics"),
                "step2", Map.of(
                        "service", "Kafka Raw Data Topics",
                        "action", "Buffer and persist raw data by source",
                        "topics", List.of("raw-flightradar24-data", "raw-adsbexchange-data",
                                "raw-marinetraffic-data", "raw-vesselfinder-data",
                                "raw-chinaports-data", "raw-marinetrafficv2-data")),
                "step3", Map.of(
                        "service", "ConsumerBasedDataFusionService",
                        "action", "Consume raw data, perform fusion, publish processed data",
                        "output", "Fused data to processed topics"),
                "step4", Map.of(
                        "service", "Downstream Consumers",
                        "action", "Consume processed data for real-time updates, storage, analytics",
                        "consumers", List.of("RealTimeDataProcessor", "WebSocket service", "Database storage")));
    }

    /**
     * Get metrics and statistics
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Get collection service metrics
        Map<String, Object> collectionStatus = dataCollectionService.getDataCollectionStatus();
        metrics.put("dataCollection", collectionStatus);

        // Get fusion service metrics
        Map<String, Object> fusionStatus = fusionService.getFusionStatus();
        metrics.put("dataFusion", fusionStatus);

        // System metrics
        metrics.put("system", Map.of(
                "architecture", "Event-Driven Raw Data Topics",
                "rawDataTopics", 6,
                "aircraftSources", 2,
                "vesselSources", 4,
                "dataPreservation", true,
                "scalability", "High"));

        return metrics;
    }

    /**
     * Health check for the entire data collection pipeline
     */
    public Map<String, Object> performHealthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Check data collection service
            Map<String, Object> collectionStatus = dataCollectionService.getDataCollectionStatus();
            boolean collectionHealthy = "RUNNING".equals(collectionStatus.get("status"));
            health.put("dataCollection",
                    Map.of("status", collectionHealthy ? "HEALTHY" : "UNHEALTHY", "details", collectionStatus));

            // Check fusion service
            Map<String, Object> fusionStatus = fusionService.getFusionStatus();
            boolean fusionHealthy = "RUNNING".equals(fusionStatus.get("status"));
            health.put("dataFusion",
                    Map.of("status", fusionHealthy ? "HEALTHY" : "UNHEALTHY", "details", fusionStatus));

            // Overall health
            boolean overallHealthy = collectionHealthy && fusionHealthy;
            health.put("overall", Map.of(
                    "status", overallHealthy ? "HEALTHY" : "UNHEALTHY",
                    "timestamp", System.currentTimeMillis(),
                    "architecture", "Event-Driven Raw Data Topics"));

        } catch (Exception e) {
            health.put("overall", Map.of(
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()));
        }

        return health;
    }
}