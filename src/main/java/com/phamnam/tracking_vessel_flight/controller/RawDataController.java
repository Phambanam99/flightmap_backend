package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.models.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.RawVesselData;
import com.phamnam.tracking_vessel_flight.repository.RawAircraftDataRepository;
import com.phamnam.tracking_vessel_flight.repository.RawVesselDataRepository;
import com.phamnam.tracking_vessel_flight.service.realtime.RawDataStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/raw-data")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Raw Data Management", description = "APIs for managing and querying raw data from external sources")
@PreAuthorize("hasRole('ADMIN')")
public class RawDataController {

    private final RawDataStorageService rawDataStorageService;
    private final RawAircraftDataRepository rawAircraftDataRepository;
    private final RawVesselDataRepository rawVesselDataRepository;

    /**
     * Get raw data statistics for analysis
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get raw data statistics", description = "Get comprehensive statistics about raw data from all external sources")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getRawDataStatistics(
            @Parameter(description = "Start time for statistics period") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time for statistics period") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        try {
            if (start == null)
                start = LocalDateTime.now().minusDays(1);
            if (end == null)
                end = LocalDateTime.now();

            Map<String, Object> statistics = rawDataStorageService.getRawDataStatistics(start, end);

            return ResponseEntity.ok(MyApiResponse.success(statistics, "Raw data statistics retrieved successfully"));

        } catch (Exception e) {
            log.error("Error retrieving raw data statistics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to retrieve statistics: " + e.getMessage()));
        }
    }

    /**
     * Get raw aircraft data by source
     */
    @GetMapping("/aircraft/{source}")
    @Operation(summary = "Get raw aircraft data by source", description = "Retrieve raw aircraft data from a specific external source")
    public ResponseEntity<MyApiResponse<Page<RawAircraftData>>> getRawAircraftData(
            @Parameter(description = "Data source name (flightradar24, adsbexchange)") @PathVariable String source,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
            Page<RawAircraftData> data = rawAircraftDataRepository.findByDataSourceOrderByReceivedAtDesc(source,
                    pageable);

            return ResponseEntity.ok(MyApiResponse.success(data, "Raw aircraft data retrieved successfully"));

        } catch (Exception e) {
            log.error("Error retrieving raw aircraft data for source {}: {}", source, e.getMessage());
            return ResponseEntity.badRequest().body(MyApiResponse.error("Failed to retrieve data: " + e.getMessage()));
        }
    }

    /**
     * Get raw vessel data by source
     */
    @GetMapping("/vessels/{source}")
    @Operation(summary = "Get raw vessel data by source", description = "Retrieve raw vessel data from a specific external source")
    public ResponseEntity<MyApiResponse<Page<RawVesselData>>> getRawVesselData(
            @Parameter(description = "Data source name (marinetraffic, vesselfinder, chinaports, marinetrafficv2)") @PathVariable String source,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
            Page<RawVesselData> data = rawVesselDataRepository.findByDataSourceOrderByReceivedAtDesc(source, pageable);

            return ResponseEntity.ok(MyApiResponse.success(data, "Raw vessel data retrieved successfully"));

        } catch (Exception e) {
            log.error("Error retrieving raw vessel data for source {}: {}", source, e.getMessage());
            return ResponseEntity.badRequest().body(MyApiResponse.error("Failed to retrieve data: " + e.getMessage()));
        }
    }

    /**
     * Get data quality analysis by source
     */
    @GetMapping("/quality-analysis")
    @Operation(summary = "Get data quality analysis", description = "Analyze data quality metrics from all external sources")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getDataQualityAnalysis(
            @Parameter(description = "Start time for analysis period") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End time for analysis period") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        try {
            if (start == null)
                start = LocalDateTime.now().minusHours(24);
            if (end == null)
                end = LocalDateTime.now();

            List<Object[]> aircraftQuality = rawAircraftDataRepository.getDataQualityBySource(start, end);
            List<Object[]> vesselQuality = rawVesselDataRepository.getDataQualityBySource(start, end);
            List<Object[]> aircraftResponseTime = rawAircraftDataRepository.getResponseTimeStatsBySource(start, end);
            List<Object[]> vesselResponseTime = rawVesselDataRepository.getResponseTimeStatsBySource(start, end);

            Map<String, Object> analysis = Map.of(
                    "period", Map.of("start", start, "end", end),
                    "aircraftQuality", aircraftQuality,
                    "vesselQuality", vesselQuality,
                    "aircraftResponseTime", aircraftResponseTime,
                    "vesselResponseTime", vesselResponseTime);

            return ResponseEntity.ok(MyApiResponse.success(analysis, "Data quality analysis completed successfully"));

        } catch (Exception e) {
            log.error("Error performing data quality analysis: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to analyze data quality: " + e.getMessage()));
        }
    }

    /**
     * Get source health status
     */
    @GetMapping("/health")
    @Operation(summary = "Get source health status", description = "Check the health and availability of all external data sources")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getSourceHealth() {
        try {
            List<Object[]> aircraftHealth = rawAircraftDataRepository.getLastDataReceiptBySource();
            List<Object[]> vesselHealth = rawVesselDataRepository.getLastDataReceiptBySource();

            Map<String, Object> health = Map.of(
                    "timestamp", LocalDateTime.now(),
                    "aircraftSources", aircraftHealth,
                    "vesselSources", vesselHealth);

            return ResponseEntity.ok(MyApiResponse.success(health, "Source health status retrieved successfully"));

        } catch (Exception e) {
            log.error("Error retrieving source health: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to retrieve source health: " + e.getMessage()));
        }
    }

    /**
     * Find duplicate aircraft records
     */
    @GetMapping("/aircraft/{hexident}/duplicates")
    @Operation(summary = "Find duplicate aircraft records", description = "Find duplicate records for a specific aircraft across all sources")
    public ResponseEntity<MyApiResponse<List<RawAircraftData>>> findAircraftDuplicates(
            @Parameter(description = "Aircraft hexident") @PathVariable String hexident,
            @Parameter(description = "Hours to look back for duplicates") @RequestParam(defaultValue = "24") int hours) {

        try {
            LocalDateTime start = LocalDateTime.now().minusHours(hours);
            LocalDateTime end = LocalDateTime.now();

            List<RawAircraftData> duplicates = rawAircraftDataRepository
                    .findDuplicatesForAircraft(hexident, start, end);

            return ResponseEntity
                    .ok(MyApiResponse.success(duplicates, "Duplicate aircraft records found: " + duplicates.size()));

        } catch (Exception e) {
            log.error("Error finding aircraft duplicates for {}: {}", hexident, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to find duplicates: " + e.getMessage()));
        }
    }

    /**
     * Find duplicate vessel records
     */
    @GetMapping("/vessels/{mmsi}/duplicates")
    @Operation(summary = "Find duplicate vessel records", description = "Find duplicate records for a specific vessel across all sources")
    public ResponseEntity<MyApiResponse<List<RawVesselData>>> findVesselDuplicates(
            @Parameter(description = "Vessel MMSI") @PathVariable String mmsi,
            @Parameter(description = "Hours to look back for duplicates") @RequestParam(defaultValue = "24") int hours) {

        try {
            LocalDateTime start = LocalDateTime.now().minusHours(hours);
            LocalDateTime end = LocalDateTime.now();

            List<RawVesselData> duplicates = rawVesselDataRepository
                    .findDuplicatesForVessel(mmsi, start, end);

            return ResponseEntity
                    .ok(MyApiResponse.success(duplicates, "Duplicate vessel records found: " + duplicates.size()));

        } catch (Exception e) {
            log.error("Error finding vessel duplicates for {}: {}", mmsi, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to find duplicates: " + e.getMessage()));
        }
    }

    /**
     * Trigger manual raw data cleanup
     */
    @PostMapping("/cleanup")
    @Operation(summary = "Trigger raw data cleanup", description = "Manually trigger cleanup of old raw data based on retention policy")
    public ResponseEntity<MyApiResponse<String>> triggerCleanup() {
        try {
            rawDataStorageService.cleanupOldRawData();

            return ResponseEntity
                    .ok(MyApiResponse.success("Cleanup completed", "Raw data cleanup triggered successfully"));

        } catch (Exception e) {
            log.error("Error triggering raw data cleanup: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to trigger cleanup: " + e.getMessage()));
        }
    }
}