package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.service.realtime.MultiSourceExternalApiService;
import com.phamnam.tracking_vessel_flight.service.realtime.DataFusionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/data-sources")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Source Management", description = "APIs for managing external data sources")
public class DataSourceController {

        private final MultiSourceExternalApiService multiSourceService;
        private final DataFusionService dataFusionService;

        @Value("${data.fusion.enabled:true}")
        private boolean fusionEnabled;

        @Value("${data.fusion.deduplication.enabled:true}")
        private boolean deduplicationEnabled;

        @Value("${data.fusion.deduplication.time-window:30000}")
        private long deduplicationTimeWindowMs;

        @Value("${data.fusion.quality.threshold:0.5}")
        private double qualityThreshold;

        @GetMapping("/status")
        @Operation(summary = "Get status of all data sources", description = "Returns the current status of all configured external data sources")
        public ResponseEntity<Map<String, Object>> getDataSourcesStatus() {
                log.debug("Getting status of all data sources");
                Map<String, Object> status = multiSourceService.getAllSourcesStatus();
                return ResponseEntity.ok(status);
        }

        @PostMapping("/collect/aircraft")
        @Operation(summary = "Manually trigger aircraft data collection", description = "Manually trigger collection of aircraft data from all configured sources")
        public ResponseEntity<Map<String, Object>> triggerAircraftDataCollection() {
                log.info("Manually triggering aircraft data collection from all sources");

                multiSourceService.collectAllAircraftData()
                                .thenAccept(data -> log.info("Aircraft data collection completed with {} records",
                                                data.size()));

                return ResponseEntity.ok(Map.of(
                                "status", "triggered",
                                "message", "Aircraft data collection started from all sources"));
        }

        @PostMapping("/collect/vessel")
        @Operation(summary = "Manually trigger vessel data collection", description = "Manually trigger collection of vessel data from all configured sources")
        public ResponseEntity<Map<String, Object>> triggerVesselDataCollection() {
                log.info("Manually triggering vessel data collection from all sources");

                multiSourceService.collectAllVesselData()
                                .thenAccept(data -> log.info("Vessel data collection completed with {} records",
                                                data.size()));

                return ResponseEntity.ok(Map.of(
                                "status", "triggered",
                                "message", "Vessel data collection started from all sources"));
        }

        @PostMapping("/collect/all")
        @Operation(summary = "Manually trigger all data collection", description = "Manually trigger collection of all tracking data from all configured sources")
        public ResponseEntity<Map<String, Object>> triggerAllDataCollection() {
                log.info("Manually triggering all data collection from all sources");

                multiSourceService.collectAndProcessMultiSourceData();

                return ResponseEntity.ok(Map.of(
                                "status", "triggered",
                                "message", "All data collection started from all sources",
                                "note", "Data will be automatically merged and deduplicated"));
        }

        @GetMapping("/fusion/config")
        @Operation(summary = "Get data fusion configuration", description = "Returns the current configuration for data fusion and deduplication")
        public ResponseEntity<Map<String, Object>> getFusionConfig() {
                return ResponseEntity.ok(Map.of(
                                "fusionEnabled", fusionEnabled,
                                "deduplicationEnabled", deduplicationEnabled,
                                "deduplicationTimeWindow", deduplicationTimeWindowMs + " ms",
                                "qualityThreshold", qualityThreshold,
                                "sourcePriorities", Map.of(
                                                "flightradar24", 1,
                                                "adsbexchange", 2,
                                                "marinetraffic", 1,
                                                "vesselfinder", 2,
                                                "chinaports", 3,
                                                "marinetrafficv2", 4),
                                "mergeStrategy", "priority-based with quality scoring",
                                "aircraftDeduplicationTolerance", "100 meters",
                                "vesselDeduplicationTolerance", "100 meters",
                                "dataAgeThreshold", Map.of(
                                                "aircraft", "60 seconds",
                                                "vessel", "120 seconds")));
        }

        @GetMapping("/scheduled/status")
        @Operation(summary = "Check scheduled task status", description = "Check if scheduled data collection is running properly")
        public ResponseEntity<Map<String, Object>> getScheduledTaskStatus() {
                return ResponseEntity.ok(Map.of(
                                "schedulingEnabled", true,
                                "asyncEnabled", true,
                                "fixedRate", "30000ms (30 seconds)",
                                "executor", "scheduledTaskExecutor",
                                "note",
                                "Check application logs for '🚀 Starting multi-source data collection' every 30 seconds"));
        }
}