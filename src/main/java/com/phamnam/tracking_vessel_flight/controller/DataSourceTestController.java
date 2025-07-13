package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.service.realtime.externalApi.ChinaportsApiService;
import com.phamnam.tracking_vessel_flight.service.realtime.externalApi.MarineTrafficV2ApiService;
import com.phamnam.tracking_vessel_flight.service.realtime.MultiSourceExternalApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/test/datasources")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Source Testing", description = "Testing APIs for new data sources")
public class DataSourceTestController {

    private final ChinaportsApiService chinaportsApiService;
    private final MarineTrafficV2ApiService marineTrafficV2ApiService;
    private final MultiSourceExternalApiService multiSourceService;

    @GetMapping("/chinaports/status")
    @Operation(summary = "Test Chinaports service status")
    public ResponseEntity<Map<String, Object>> testChinaportsStatus() {
        log.info("Testing Chinaports service status");

        Map<String, Object> response = Map.of(
                "serviceName", "ChinaportsApiService",
                "status", chinaportsApiService.getChinaportsStatus(),
                "available", chinaportsApiService.isChinaportsAvailable(),
                "serviceClass", chinaportsApiService.getClass().getSimpleName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/marinetrafficv2/status")
    @Operation(summary = "Test MarineTraffic V2 service status")
    public ResponseEntity<Map<String, Object>> testMarineTrafficV2Status() {
        log.info("Testing MarineTraffic V2 service status");

        Map<String, Object> response = Map.of(
                "serviceName", "MarineTrafficV2ApiService",
                "status", marineTrafficV2ApiService.getMarineTrafficV2Status(),
                "available", marineTrafficV2ApiService.isMarineTrafficV2Available(),
                "serviceClass", marineTrafficV2ApiService.getClass().getSimpleName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/chinaports/fetch")
    @Operation(summary = "Test fetch data from Chinaports")
    public ResponseEntity<Map<String, Object>> testChinaportsFetch() {
        log.info("Testing Chinaports data fetch");

        try {
            CompletableFuture<List<VesselTrackingRequest>> future = chinaportsApiService.fetchVesselData();
            List<VesselTrackingRequest> data = future.get(); // Wait for completion

            Map<String, Object> response = Map.of(
                    "success", true,
                    "dataCount", data.size(),
                    "data", data.size() > 0 ? data.subList(0, Math.min(3, data.size())) : List.of(),
                    "message", "Successfully fetched " + data.size() + " vessels from Chinaports");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error testing Chinaports fetch", e);

            Map<String, Object> response = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to fetch data from Chinaports");

            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/marinetrafficv2/fetch")
    @Operation(summary = "Test fetch data from MarineTraffic V2")
    public ResponseEntity<Map<String, Object>> testMarineTrafficV2Fetch() {
        log.info("Testing MarineTraffic V2 data fetch");

        try {
            CompletableFuture<List<VesselTrackingRequest>> future = marineTrafficV2ApiService.fetchVesselData();
            List<VesselTrackingRequest> data = future.get(); // Wait for completion

            Map<String, Object> response = Map.of(
                    "success", true,
                    "dataCount", data.size(),
                    "data", data.size() > 0 ? data.subList(0, Math.min(3, data.size())) : List.of(),
                    "message", "Successfully fetched " + data.size() + " vessels from MarineTraffic V2");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error testing MarineTraffic V2 fetch", e);

            Map<String, Object> response = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to fetch data from MarineTraffic V2");

            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/multisource/fetch")
    @Operation(summary = "Test multi-source data collection")
    public ResponseEntity<Map<String, Object>> testMultiSourceFetch() {
        log.info("Testing multi-source data collection");

        try {
            CompletableFuture<List<VesselTrackingRequest>> future = multiSourceService.collectAllVesselData();
            List<VesselTrackingRequest> data = future.get(); // Wait for completion

            Map<String, Object> response = Map.of(
                    "success", true,
                    "mergedDataCount", data.size(),
                    "sampleData", data.size() > 0 ? data.subList(0, Math.min(3, data.size())) : List.of(),
                    "message", "Successfully collected and merged " + data.size() + " vessels from all sources",
                    "allSourcesStatus", multiSourceService.getAllSourcesStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error testing multi-source fetch", e);

            Map<String, Object> response = Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "message", "Failed to collect data from multiple sources");

            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/all/dependency-check")
    @Operation(summary = "Check if all services are properly injected")
    public ResponseEntity<Map<String, Object>> checkDependencies() {
        log.info("Checking service dependencies");

        Map<String, Object> response = Map.of(
                "chinaportsService", Map.of(
                        "injected", chinaportsApiService != null,
                        "className", chinaportsApiService != null ? chinaportsApiService.getClass().getName() : "null"),
                "marineTrafficV2Service", Map.of(
                        "injected", marineTrafficV2ApiService != null,
                        "className",
                        marineTrafficV2ApiService != null ? marineTrafficV2ApiService.getClass().getName() : "null"),
                "multiSourceService", Map.of(
                        "injected", multiSourceService != null,
                        "className", multiSourceService != null ? multiSourceService.getClass().getName() : "null"),
                "allInjected",
                chinaportsApiService != null && marineTrafficV2ApiService != null && multiSourceService != null);

        return ResponseEntity.ok(response);
    }
}