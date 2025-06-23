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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/entities")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Entity Details", description = "APIs for displaying detailed entity information with both merged and source-specific data")
public class EntityDetailsController {

    private final RawAircraftDataRepository rawAircraftDataRepository;
    private final RawVesselDataRepository rawVesselDataRepository;
    private final FlightTrackingRepository flightTrackingRepository;
    private final ShipTrackingRepository shipTrackingRepository;

    /**
     * Get detailed aircraft information with merged data and source breakdown
     */
    @GetMapping("/aircraft/{hexident}/details")
    @Operation(summary = "Get detailed aircraft information")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getAircraftDetails(
            @PathVariable String hexident,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "true") boolean includeSourceData) {

        try {
            LocalDateTime start = LocalDateTime.now().minusHours(hours);
            LocalDateTime end = LocalDateTime.now();

            // Get merged/processed data
            List<FlightTracking> processedData = flightTrackingRepository
                    .findByHexidentAndTimestampBetween(hexident, start, end);

            Map<String, Object> result = new HashMap<>();
            result.put("hexident", hexident);
            result.put("timeWindow", Map.of("start", start, "end", end, "hoursBack", hours));

            // Main processed data
            result.put("processedData", formatProcessedAircraftData(processedData));

            // Source-specific data if requested
            if (includeSourceData) {
                List<RawAircraftData> rawData = rawAircraftDataRepository
                        .findByHexidentAndReceivedAtBetween(hexident, start, end);

                result.put("sourceData", formatRawAircraftDataBySources(rawData));
                result.put("sourceSummary", createAircraftSourceSummary(rawData));
            }

            return ResponseEntity.ok(MyApiResponse.success(result, "Aircraft details retrieved successfully"));

        } catch (Exception e) {
            log.error("Error retrieving aircraft details for {}: {}", hexident, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to retrieve aircraft details: " + e.getMessage()));
        }
    }

    /**
     * Get detailed vessel information with merged data and source breakdown
     */
    @GetMapping("/vessels/{mmsi}/details")
    @Operation(summary = "Get detailed vessel information")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getVesselDetails(
            @PathVariable String mmsi,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "true") boolean includeSourceData) {

        try {
            LocalDateTime start = LocalDateTime.now().minusHours(hours);
            LocalDateTime end = LocalDateTime.now();

            // Get merged/processed data
            List<ShipTracking> processedData = shipTrackingRepository
                    .findByMmsiAndTimestampBetween(mmsi, start, end);

            Map<String, Object> result = new HashMap<>();
            result.put("mmsi", mmsi);
            result.put("timeWindow", Map.of("start", start, "end", end, "hoursBack", hours));

            // Main processed data
            result.put("processedData", formatProcessedVesselData(processedData));

            // Source-specific data if requested
            if (includeSourceData) {
                List<RawVesselData> rawData = rawVesselDataRepository
                        .findByMmsiAndReceivedAtBetween(mmsi, start, end);

                result.put("sourceData", formatRawVesselDataBySources(rawData));
                result.put("sourceSummary", createVesselSourceSummary(rawData));
            }

            return ResponseEntity.ok(MyApiResponse.success(result, "Vessel details retrieved successfully"));

        } catch (Exception e) {
            log.error("Error retrieving vessel details for {}: {}", mmsi, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to retrieve vessel details: " + e.getMessage()));
        }
    }

    /**
     * Get latest position and status for aircraft
     */
    @GetMapping("/aircraft/{hexident}/current")
    @Operation(summary = "Get current aircraft status", description = "Get the most recent position and status information for aircraft")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getCurrentAircraftStatus(
            @PathVariable String hexident) {

        try {
            LocalDateTime start = LocalDateTime.now().minusHours(6); // Last 6 hours
            LocalDateTime end = LocalDateTime.now();

            // Get most recent processed data
            List<FlightTracking> recentData = flightTrackingRepository
                    .findByHexidentAndTimestampBetween(hexident, start, end);

            FlightTracking latest = recentData.stream()
                    .max(Comparator.comparing(FlightTracking::getTimestamp))
                    .orElse(null);

            // Get recent raw data for source breakdown
            List<RawAircraftData> rawData = rawAircraftDataRepository
                    .findByHexidentAndReceivedAtBetween(hexident, start, end);

            Map<String, Object> result = new HashMap<>();
            result.put("hexident", hexident);
            result.put("currentStatus", latest != null ? formatSingleFlightTracking(latest) : null);
            result.put("lastUpdate", latest != null ? latest.getTimestamp() : null);
            result.put("dataAge", latest != null ? calculateDataAge(latest.getTimestamp()) : null);
            result.put("sourcesReporting", createCurrentSourcesStatus(rawData));
            result.put("isActive", latest != null && isDataRecent(latest.getTimestamp(), 30)); // Active if updated
                                                                                               // within 30 minutes

            return ResponseEntity.ok(MyApiResponse.success(result, "Current aircraft status retrieved successfully"));

        } catch (Exception e) {
            log.error("Error retrieving current aircraft status for {}: {}", hexident, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to retrieve current status: " + e.getMessage()));
        }
    }

    /**
     * Get latest position and status for vessel
     */
    @GetMapping("/vessels/{mmsi}/current")
    @Operation(summary = "Get current vessel status", description = "Get the most recent position and status information for vessel")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getCurrentVesselStatus(
            @PathVariable String mmsi) {

        try {
            LocalDateTime start = LocalDateTime.now().minusHours(6); // Last 6 hours
            LocalDateTime end = LocalDateTime.now();

            // Get most recent processed data
            List<ShipTracking> recentData = shipTrackingRepository
                    .findByMmsiAndTimestampBetween(mmsi, start, end);

            ShipTracking latest = recentData.stream()
                    .max(Comparator.comparing(ShipTracking::getTimestamp))
                    .orElse(null);

            // Get recent raw data for source breakdown
            List<RawVesselData> rawData = rawVesselDataRepository
                    .findByMmsiAndReceivedAtBetween(mmsi, start, end);

            Map<String, Object> result = new HashMap<>();
            result.put("mmsi", mmsi);
            result.put("currentStatus", latest != null ? formatSingleShipTracking(latest) : null);
            result.put("lastUpdate", latest != null ? latest.getTimestamp() : null);
            result.put("dataAge", latest != null ? calculateDataAge(latest.getTimestamp()) : null);
            result.put("sourcesReporting", createCurrentVesselSourcesStatus(rawData));
            result.put("isActive", latest != null && isDataRecent(latest.getTimestamp(), 60)); // Active if updated
                                                                                               // within 60 minutes

            return ResponseEntity.ok(MyApiResponse.success(result, "Current vessel status retrieved successfully"));

        } catch (Exception e) {
            log.error("Error retrieving current vessel status for {}: {}", mmsi, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(MyApiResponse.error("Failed to retrieve current status: " + e.getMessage()));
        }
    }

    // Helper methods for formatting aircraft data
    private Map<String, Object> formatProcessedAircraftData(List<FlightTracking> data) {
        if (data.isEmpty()) {
            return Map.of("recordCount", 0, "message", "No processed data available");
        }

        FlightTracking latest = data.stream()
                .max(Comparator.comparing(FlightTracking::getTimestamp))
                .orElse(null);

        return Map.of(
                "recordCount", data.size(),
                "latestRecord", latest != null ? formatSingleFlightTracking(latest) : null,
                "allRecords", data.stream()
                        .sorted(Comparator.comparing(FlightTracking::getTimestamp).reversed())
                        .limit(50)
                        .map(this::formatSingleFlightTracking)
                        .collect(Collectors.toList()));
    }

    private Map<String, Object> formatSingleFlightTracking(FlightTracking tracking) {
        return Map.of(
                "timestamp", tracking.getTimestamp(),
                "hexident", tracking.getHexident(),
                "callsign", tracking.getCallsign() != null ? tracking.getCallsign() : "",
                "latitude", tracking.getLatitude(),
                "longitude", tracking.getLongitude(),
                "altitude", tracking.getAltitude() != null ? tracking.getAltitude() : 0,
                "speed", tracking.getSpeed() != null ? tracking.getSpeed() : 0,
                "track", tracking.getTrack() != null ? tracking.getTrack() : 0,
                "dataSource", "Merged Data");
    }

    private Map<String, Object> formatRawAircraftDataBySources(List<RawAircraftData> rawData) {
        Map<String, List<RawAircraftData>> dataBySource = rawData.stream()
                .collect(Collectors.groupingBy(RawAircraftData::getDataSource));

        Map<String, Object> result = new HashMap<>();

        dataBySource.forEach((source, data) -> {
            List<Map<String, Object>> formattedData = data.stream()
                    .sorted(Comparator.comparing(RawAircraftData::getReceivedAt).reversed())
                    .limit(20)
                    .map(this::formatSingleRawAircraftData)
                    .collect(Collectors.toList());

            result.put(source, Map.of(
                    "source", source,
                    "totalRecords", data.size(),
                    "priority", data.isEmpty() ? null : data.get(0).getSourcePriority(),
                    "records", formattedData));
        });

        return result;
    }

    private Map<String, Object> formatSingleRawAircraftData(RawAircraftData raw) {
        return Map.of(
                "receivedAt", raw.getReceivedAt(),
                "hexident", raw.getHexident() != null ? raw.getHexident() : "",
                "callsign", raw.getCallsign() != null ? raw.getCallsign() : "",
                "latitude", raw.getLatitude(),
                "longitude", raw.getLongitude(),
                "altitude", raw.getAltitude() != null ? raw.getAltitude() : 0,
                "speed", raw.getGroundSpeed() != null ? raw.getGroundSpeed() : 0,
                "dataQuality", raw.getDataQuality(),
                "dataSource", raw.getDataSource());
    }

    private Map<String, Object> createAircraftSourceSummary(List<RawAircraftData> rawData) {
        Map<String, List<RawAircraftData>> dataBySource = rawData.stream()
                .collect(Collectors.groupingBy(RawAircraftData::getDataSource));

        Map<String, Object> summary = new HashMap<>();

        dataBySource.forEach((source, data) -> {
            summary.put(source, Map.of(
                    "recordCount", data.size(),
                    "averageQuality",
                    data.stream().mapToDouble(d -> d.getDataQuality() != null ? d.getDataQuality() : 0.0).average()
                            .orElse(0.0),
                    "latestUpdate", data.stream().map(RawAircraftData::getReceivedAt).max(Comparator.naturalOrder()),
                    "priority", data.isEmpty() ? null : data.get(0).getSourcePriority()));
        });

        return summary;
    }

    // Helper methods for formatting vessel data
    private Map<String, Object> formatProcessedVesselData(List<ShipTracking> data) {
        if (data.isEmpty()) {
            return Map.of("recordCount", 0, "message", "No processed data available");
        }

        ShipTracking latest = data.stream()
                .max(Comparator.comparing(ShipTracking::getTimestamp))
                .orElse(null);

        return Map.of(
                "recordCount", data.size(),
                "latestRecord", latest != null ? formatSingleShipTracking(latest) : null,
                "allRecords", data.stream()
                        .sorted(Comparator.comparing(ShipTracking::getTimestamp).reversed())
                        .limit(50)
                        .map(this::formatSingleShipTracking)
                        .collect(Collectors.toList()));
    }

    private Map<String, Object> formatSingleShipTracking(ShipTracking tracking) {
        return Map.of(
                "timestamp", tracking.getTimestamp(),
                "mmsi", tracking.getMmsi(),
                "latitude", tracking.getLatitude(),
                "longitude", tracking.getLongitude(),
                "speed", tracking.getSpeed() != null ? tracking.getSpeed() : 0.0,
                "course", tracking.getCourse() != null ? tracking.getCourse() : 0.0,
                "navigationStatus", tracking.getNavigationStatus() != null ? tracking.getNavigationStatus() : "",
                "dataSource", "Merged Data");
    }

    private Map<String, Object> formatRawVesselDataBySources(List<RawVesselData> rawData) {
        Map<String, List<RawVesselData>> dataBySource = rawData.stream()
                .collect(Collectors.groupingBy(RawVesselData::getDataSource));

        Map<String, Object> result = new HashMap<>();

        dataBySource.forEach((source, data) -> {
            List<Map<String, Object>> formattedData = data.stream()
                    .sorted(Comparator.comparing(RawVesselData::getReceivedAt).reversed())
                    .limit(20)
                    .map(this::formatSingleRawVesselData)
                    .collect(Collectors.toList());

            result.put(source, Map.of(
                    "source", source,
                    "totalRecords", data.size(),
                    "priority", data.isEmpty() ? null : data.get(0).getSourcePriority(),
                    "records", formattedData));
        });

        return result;
    }

    private Map<String, Object> formatSingleRawVesselData(RawVesselData raw) {
        return Map.of(
                "receivedAt", raw.getReceivedAt(),
                "mmsi", raw.getMmsi() != null ? raw.getMmsi() : "",
                "vesselName", raw.getVesselName() != null ? raw.getVesselName() : "",
                "latitude", raw.getLatitude(),
                "longitude", raw.getLongitude(),
                "speed", raw.getSpeed() != null ? raw.getSpeed() : 0.0,
                "destination", raw.getDestination() != null ? raw.getDestination() : "",
                "vesselType", raw.getVesselType() != null ? raw.getVesselType() : "",
                "dataQuality", raw.getDataQuality(),
                "dataSource", raw.getDataSource());
    }

    private Map<String, Object> createVesselSourceSummary(List<RawVesselData> rawData) {
        Map<String, List<RawVesselData>> dataBySource = rawData.stream()
                .collect(Collectors.groupingBy(RawVesselData::getDataSource));

        Map<String, Object> summary = new HashMap<>();

        dataBySource.forEach((source, data) -> {
            summary.put(source, Map.of(
                    "recordCount", data.size(),
                    "averageQuality",
                    data.stream().mapToDouble(d -> d.getDataQuality() != null ? d.getDataQuality() : 0.0).average()
                            .orElse(0.0),
                    "latestUpdate", data.stream().map(RawVesselData::getReceivedAt).max(Comparator.naturalOrder()),
                    "priority", data.isEmpty() ? null : data.get(0).getSourcePriority()));
        });

        return summary;
    }

    // Utility methods
    private Map<String, Object> createAircraftDataAvailability(List<FlightTracking> processedData,
            List<RawAircraftData> rawData) {
        Set<String> availableSources = rawData.stream()
                .map(RawAircraftData::getDataSource)
                .collect(Collectors.toSet());

        return Map.of(
                "hasProcessedData", !processedData.isEmpty(),
                "availableSources", availableSources,
                "sourceCount", availableSources.size(),
                "totalRawRecords", rawData.size(),
                "totalProcessedRecords", processedData.size());
    }

    private Map<String, Object> createVesselDataAvailability(List<ShipTracking> processedData,
            List<RawVesselData> rawData) {
        Set<String> availableSources = rawData.stream()
                .map(RawVesselData::getDataSource)
                .collect(Collectors.toSet());

        return Map.of(
                "hasProcessedData", !processedData.isEmpty(),
                "availableSources", availableSources,
                "sourceCount", availableSources.size(),
                "totalRawRecords", rawData.size(),
                "totalProcessedRecords", processedData.size());
    }

    private Map<String, Object> createCurrentSourcesStatus(List<RawAircraftData> rawData) {
        LocalDateTime recent = LocalDateTime.now().minusMinutes(30);

        Map<String, List<RawAircraftData>> recentBySource = rawData.stream()
                .filter(d -> d.getReceivedAt().isAfter(recent))
                .collect(Collectors.groupingBy(RawAircraftData::getDataSource));

        Map<String, Object> status = new HashMap<>();
        recentBySource.forEach((source, data) -> {
            status.put(source, Map.of(
                    "isActive", true,
                    "lastUpdate", data.stream().map(RawAircraftData::getReceivedAt).max(Comparator.naturalOrder()),
                    "recentRecords", data.size()));
        });

        return status;
    }

    private Map<String, Object> createCurrentVesselSourcesStatus(List<RawVesselData> rawData) {
        LocalDateTime recent = LocalDateTime.now().minusMinutes(60);

        Map<String, List<RawVesselData>> recentBySource = rawData.stream()
                .filter(d -> d.getReceivedAt().isAfter(recent))
                .collect(Collectors.groupingBy(RawVesselData::getDataSource));

        Map<String, Object> status = new HashMap<>();
        recentBySource.forEach((source, data) -> {
            status.put(source, Map.of(
                    "isActive", true,
                    "lastUpdate", data.stream().map(RawVesselData::getReceivedAt).max(Comparator.naturalOrder()),
                    "recentRecords", data.size()));
        });

        return status;
    }

    private String calculateDataAge(LocalDateTime timestamp) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(timestamp, now).toMinutes();

        if (minutes < 1)
            return "Less than 1 minute ago";
        if (minutes < 60)
            return minutes + " minutes ago";

        long hours = minutes / 60;
        if (hours < 24)
            return hours + " hours ago";

        long days = hours / 24;
        return days + " days ago";
    }

    private boolean isDataRecent(LocalDateTime timestamp, int maxMinutes) {
        return timestamp.isAfter(LocalDateTime.now().minusMinutes(maxMinutes));
    }
}