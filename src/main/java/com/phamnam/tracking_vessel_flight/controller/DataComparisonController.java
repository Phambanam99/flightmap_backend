package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.models.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.RawVesselData;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import com.phamnam.tracking_vessel_flight.repository.FlightTrackingRepository;
import com.phamnam.tracking_vessel_flight.repository.RawAircraftDataRepository;
import com.phamnam.tracking_vessel_flight.repository.RawVesselDataRepository;
import com.phamnam.tracking_vessel_flight.repository.ShipTrackingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/data-comparison")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Comparison", description = "APIs for comparing raw source data with merged/processed data")
// @PreAuthorize("hasRole('ADMIN')")
public class DataComparisonController {

    private final RawAircraftDataRepository rawAircraftDataRepository;
    private final RawVesselDataRepository rawVesselDataRepository;
    private final FlightTrackingRepository flightTrackingRepository;
    private final ShipTrackingRepository shipTrackingRepository;

    /**
     * Compare aircraft data from all sources with merged result
     */
    @GetMapping("/aircraft/{hexident}")
    @Operation(summary = "Compare aircraft data from sources vs merged")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> compareAircraftData(
            @PathVariable String hexident,
            @RequestParam(defaultValue = "24") int hours) {

        try {
            LocalDateTime start = LocalDateTime.now().minusHours(hours);
            LocalDateTime end = LocalDateTime.now();

            List<RawAircraftData> rawData = rawAircraftDataRepository
                    .findByHexidentAndReceivedAtBetween(hexident, start, end);

            List<FlightTracking> processedData = flightTrackingRepository
                    .findByHexidentAndTimestampBetween(hexident, start, end);

            Map<String, List<RawAircraftData>> dataBySource = rawData.stream()
                    .collect(Collectors.groupingBy(RawAircraftData::getDataSource));

            Map<String, Object> comparison = Map.of(
                    "aircraft", hexident,
                    "timeWindow", Map.of("start", start, "end", end),
                    "rawDataSources", analyzeRawAircraftSources(dataBySource),
                    "processedData", analyzeProcessedAircraftData(processedData),
                    "fusionMetrics", calculateAircraftFusionMetrics(dataBySource, processedData),
                    "dataQualityComparison", compareAircraftDataQuality(dataBySource, processedData));

            return ResponseEntity
                    .ok(MyApiResponse.success(comparison, "Aircraft data comparison completed successfully"));

        } catch (Exception e) {
            log.error("Error comparing aircraft data for {}: {}", hexident, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to compare aircraft data: " + e.getMessage()));
        }
    }

    /**
     * Compare vessel data from all sources with merged result
     */
    @GetMapping("/vessels/{mmsi}")
    @Operation(summary = "Compare vessel data from sources vs merged")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> compareVesselData(
            @PathVariable String mmsi,
            @RequestParam(defaultValue = "24") int hours) {

        try {
            LocalDateTime start = LocalDateTime.now().minusHours(hours);
            LocalDateTime end = LocalDateTime.now();

            List<RawVesselData> rawData = rawVesselDataRepository
                    .findByMmsiAndReceivedAtBetween(mmsi, start, end);

            List<ShipTracking> processedData = shipTrackingRepository
                    .findByMmsiAndTimestampBetween(mmsi, start, end);

            Map<String, List<RawVesselData>> dataBySource = rawData.stream()
                    .collect(Collectors.groupingBy(RawVesselData::getDataSource));

            Map<String, Object> comparison = Map.of(
                    "vessel", mmsi,
                    "timeWindow", Map.of("start", start, "end", end),
                    "rawDataSources", analyzeRawVesselSources(dataBySource),
                    "processedData", analyzeProcessedVesselData(processedData),
                    "fusionMetrics", calculateVesselFusionMetrics(dataBySource, processedData),
                    "dataQualityComparison", compareVesselDataQuality(dataBySource, processedData));

            return ResponseEntity
                    .ok(MyApiResponse.success(comparison, "Vessel data comparison completed successfully"));

        } catch (Exception e) {
            log.error("Error comparing vessel data for {}: {}", mmsi, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to compare vessel data: " + e.getMessage()));
        }
    }

    /**
     * Get fusion effectiveness summary
     */
    @GetMapping("/fusion-effectiveness")
    @Operation(summary = "Get fusion effectiveness summary")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getFusionEffectiveness(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        try {
            if (start == null)
                start = LocalDateTime.now().minusHours(24);
            if (end == null)
                end = LocalDateTime.now();

            Map<String, Object> aircraftEffectiveness = analyzeAircraftFusionEffectiveness(start, end);
            Map<String, Object> vesselEffectiveness = analyzeVesselFusionEffectiveness(start, end);

            Map<String, Object> effectiveness = Map.of(
                    "period", Map.of("start", start, "end", end),
                    "aircraftFusion", aircraftEffectiveness,
                    "vesselFusion", vesselEffectiveness,
                    "overallMetrics", calculateOverallFusionMetrics(aircraftEffectiveness, vesselEffectiveness));

            return ResponseEntity
                    .ok(MyApiResponse.success(effectiveness, "Fusion effectiveness analysis completed successfully"));

        } catch (Exception e) {
            log.error("Error analyzing fusion effectiveness: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to analyze fusion effectiveness: " + e.getMessage()));
        }
    }

    /**
     * Get source contribution analysis
     */
    @GetMapping("/source-contribution")
    @Operation(summary = "Get source contribution analysis")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getSourceContribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        try {
            if (start == null)
                start = LocalDateTime.now().minusHours(24);
            if (end == null)
                end = LocalDateTime.now();

            Map<String, Object> contribution = Map.of(
                    "period", Map.of("start", start, "end", end),
                    "aircraftSources", analyzeAircraftSourceContribution(start, end),
                    "vesselSources", analyzeVesselSourceContribution(start, end),
                    "recommendations", generateSourceOptimizationRecommendations(start, end));

            return ResponseEntity
                    .ok(MyApiResponse.success(contribution, "Source contribution analysis completed successfully"));

        } catch (Exception e) {
            log.error("Error analyzing source contribution: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to analyze source contribution: " + e.getMessage()));
        }
    }

    // Helper methods for aircraft analysis
    private Map<String, Object> analyzeRawAircraftSources(Map<String, List<RawAircraftData>> dataBySource) {
        Map<String, Object> analysis = new HashMap<>();

        dataBySource.forEach((source, data) -> {
            Map<String, Object> sourceAnalysis = Map.of(
                    "recordCount", data.size(),
                    "averageQuality",
                    data.stream().mapToDouble(d -> d.getDataQuality() != null ? d.getDataQuality() : 0.0).average()
                            .orElse(0.0),
                    "averageResponseTime",
                    data.stream().mapToLong(d -> d.getApiResponseTime() != null ? d.getApiResponseTime() : 0L).average()
                            .orElse(0.0),
                    "latestTimestamp", data.stream().map(RawAircraftData::getReceivedAt).max(Comparator.naturalOrder()),
                    "validRecords", data.stream().mapToInt(d -> Boolean.TRUE.equals(d.getIsValid()) ? 1 : 0).sum(),
                    "sourcePriority", data.isEmpty() ? null : data.get(0).getSourcePriority());
            analysis.put(source, sourceAnalysis);
        });

        return analysis;
    }

    private Map<String, Object> analyzeProcessedAircraftData(List<FlightTracking> processedData) {
        return Map.of(
                "recordCount", processedData.size(),
                "timeRange", processedData.isEmpty() ? null
                        : Map.of(
                                "earliest",
                                processedData.stream().map(FlightTracking::getTimestamp).min(Comparator.naturalOrder()),
                                "latest", processedData.stream().map(FlightTracking::getTimestamp)
                                        .max(Comparator.naturalOrder())));
    }

    private Map<String, Object> calculateAircraftFusionMetrics(Map<String, List<RawAircraftData>> dataBySource,
            List<FlightTracking> processedData) {
        int totalRawRecords = dataBySource.values().stream().mapToInt(List::size).sum();
        int processedRecords = processedData.size();

        return Map.of(
                "totalRawRecords", totalRawRecords,
                "processedRecords", processedRecords,
                "compressionRatio", totalRawRecords > 0 ? (double) processedRecords / totalRawRecords : 0.0,
                "sourcesContributing", dataBySource.size(),
                "fusionEfficiency", calculateFusionEfficiency(totalRawRecords, processedRecords));
    }

    private Map<String, Object> compareAircraftDataQuality(Map<String, List<RawAircraftData>> dataBySource,
            List<FlightTracking> processedData) {
        double averageRawQuality = dataBySource.values().stream()
                .flatMap(List::stream)
                .mapToDouble(d -> d.getDataQuality() != null ? d.getDataQuality() : 0.0)
                .average().orElse(0.0);

        return Map.of(
                "averageRawDataQuality", averageRawQuality,
                "qualityBySource", dataBySource.entrySet().stream().collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .mapToDouble(d -> d.getDataQuality() != null ? d.getDataQuality() : 0.0)
                                        .average().orElse(0.0))),
                "estimatedProcessedQuality", estimateProcessedDataQuality(averageRawQuality));
    }

    // Helper methods for vessel analysis
    private Map<String, Object> analyzeRawVesselSources(Map<String, List<RawVesselData>> dataBySource) {
        Map<String, Object> analysis = new HashMap<>();

        dataBySource.forEach((source, data) -> {
            Map<String, Object> sourceAnalysis = Map.of(
                    "recordCount", data.size(),
                    "averageQuality",
                    data.stream().mapToDouble(d -> d.getDataQuality() != null ? d.getDataQuality() : 0.0).average()
                            .orElse(0.0),
                    "averageResponseTime",
                    data.stream().mapToLong(d -> d.getApiResponseTime() != null ? d.getApiResponseTime() : 0L).average()
                            .orElse(0.0),
                    "latestTimestamp", data.stream().map(RawVesselData::getReceivedAt).max(Comparator.naturalOrder()),
                    "validRecords", data.stream().mapToInt(d -> Boolean.TRUE.equals(d.getIsValid()) ? 1 : 0).sum(),
                    "sourcePriority", data.isEmpty() ? null : data.get(0).getSourcePriority());
            analysis.put(source, sourceAnalysis);
        });

        return analysis;
    }

    private Map<String, Object> analyzeProcessedVesselData(List<ShipTracking> processedData) {
        return Map.of(
                "recordCount", processedData.size(),
                "timeRange", processedData.isEmpty() ? null
                        : Map.of(
                                "earliest",
                                processedData.stream().map(ShipTracking::getTimestamp).min(Comparator.naturalOrder()),
                                "latest",
                                processedData.stream().map(ShipTracking::getTimestamp).max(Comparator.naturalOrder())));
    }

    private Map<String, Object> calculateVesselFusionMetrics(Map<String, List<RawVesselData>> dataBySource,
            List<ShipTracking> processedData) {
        int totalRawRecords = dataBySource.values().stream().mapToInt(List::size).sum();
        int processedRecords = processedData.size();

        return Map.of(
                "totalRawRecords", totalRawRecords,
                "processedRecords", processedRecords,
                "compressionRatio", totalRawRecords > 0 ? (double) processedRecords / totalRawRecords : 0.0,
                "sourcesContributing", dataBySource.size(),
                "fusionEfficiency", calculateFusionEfficiency(totalRawRecords, processedRecords));
    }

    private Map<String, Object> compareVesselDataQuality(Map<String, List<RawVesselData>> dataBySource,
            List<ShipTracking> processedData) {
        double averageRawQuality = dataBySource.values().stream()
                .flatMap(List::stream)
                .mapToDouble(d -> d.getDataQuality() != null ? d.getDataQuality() : 0.0)
                .average().orElse(0.0);

        return Map.of(
                "averageRawDataQuality", averageRawQuality,
                "qualityBySource", dataBySource.entrySet().stream().collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .mapToDouble(d -> d.getDataQuality() != null ? d.getDataQuality() : 0.0)
                                        .average().orElse(0.0))),
                "estimatedProcessedQuality", estimateProcessedDataQuality(averageRawQuality));
    }

    // Overall analysis methods
    private Map<String, Object> analyzeAircraftFusionEffectiveness(LocalDateTime start, LocalDateTime end) {
        List<Object[]> qualityStats = rawAircraftDataRepository.getDataQualityBySource(start, end);
        List<Object[]> recordCounts = rawAircraftDataRepository.countRecordsBySource(start, end);

        return Map.of(
                "qualityStatsBySource", qualityStats,
                "recordCountsBySource", recordCounts,
                "overallEffectiveness", calculateOverallEffectiveness(qualityStats, recordCounts));
    }

    private Map<String, Object> analyzeVesselFusionEffectiveness(LocalDateTime start, LocalDateTime end) {
        List<Object[]> qualityStats = rawVesselDataRepository.getDataQualityBySource(start, end);
        List<Object[]> recordCounts = rawVesselDataRepository.countRecordsBySource(start, end);

        return Map.of(
                "qualityStatsBySource", qualityStats,
                "recordCountsBySource", recordCounts,
                "overallEffectiveness", calculateOverallEffectiveness(qualityStats, recordCounts));
    }

    private Map<String, Object> calculateOverallFusionMetrics(Map<String, Object> aircraftMetrics,
            Map<String, Object> vesselMetrics) {
        return Map.of(
                "totalSources", 6,
                "aircraftSources", 2,
                "vesselSources", 4,
                "fusionEnabled", true,
                "recommendedOptimizations", List.of(
                        "Monitor source response times regularly",
                        "Adjust priority weights based on quality metrics",
                        "Consider increasing polling frequency for high-quality sources"));
    }

    private Map<String, Object> analyzeAircraftSourceContribution(LocalDateTime start, LocalDateTime end) {
        List<Object[]> recordCounts = rawAircraftDataRepository.countRecordsBySource(start, end);
        return analyzeSourceContribution(recordCounts, "aircraft");
    }

    private Map<String, Object> analyzeVesselSourceContribution(LocalDateTime start, LocalDateTime end) {
        List<Object[]> recordCounts = rawVesselDataRepository.countRecordsBySource(start, end);
        return analyzeSourceContribution(recordCounts, "vessel");
    }

    private Map<String, Object> analyzeSourceContribution(List<Object[]> recordCounts, String entityType) {
        long totalRecords = recordCounts.stream().mapToLong(row -> (Long) row[1]).sum();

        Map<String, Object> contributions = new HashMap<>();
        recordCounts.forEach(row -> {
            String source = (String) row[0];
            Long count = (Long) row[1];
            double percentage = totalRecords > 0 ? (double) count / totalRecords * 100 : 0.0;

            contributions.put(source, Map.of(
                    "recordCount", count,
                    "percentage", percentage,
                    "contributionLevel", getContributionLevel(percentage)));
        });

        return Map.of(
                "entityType", entityType,
                "totalRecords", totalRecords,
                "sourceContributions", contributions);
    }

    // Utility methods
    private double calculateFusionEfficiency(int rawRecords, int processedRecords) {
        if (rawRecords == 0)
            return 0.0;
        return Math.min(1.0, (double) processedRecords / rawRecords);
    }

    private double estimateProcessedDataQuality(double rawQuality) {
        return Math.min(1.0, rawQuality * 1.1);
    }

    private double calculateOverallEffectiveness(List<Object[]> qualityStats, List<Object[]> recordCounts) {
        double avgQuality = qualityStats.stream()
                .mapToDouble(row -> ((Number) row[1]).doubleValue())
                .average().orElse(0.0);

        long totalRecords = recordCounts.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        return avgQuality * Math.log10(Math.max(1, totalRecords)) / 10.0;
    }

    private String getContributionLevel(double percentage) {
        if (percentage > 40)
            return "HIGH";
        if (percentage > 20)
            return "MEDIUM";
        if (percentage > 5)
            return "LOW";
        return "MINIMAL";
    }

    private List<String> generateSourceOptimizationRecommendations(LocalDateTime start, LocalDateTime end) {
        return List.of(
                "Review low-contributing sources for cost optimization",
                "Increase retention period for high-quality sources",
                "Consider load balancing between similar sources",
                "Implement quality-based source selection");
    }
}