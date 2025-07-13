package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import com.phamnam.tracking_vessel_flight.service.realtime.RealTimeDataQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking/history")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tracking History", description = "APIs for accessing historical tracking data for flights and vessels")
public class TrackingHistoryController {

    private final RealTimeDataQueryService queryService;

    // ============================================================================
    // FLIGHT HISTORY ENDPOINTS
    // ============================================================================

    @Operation(summary = "Get flight tracking history", description = "Retrieves historical tracking data for a specific flight by hexident within a time range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved flight history"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "No flight data found for the given hexident")
    })
    @GetMapping("/flight/{hexident}")
    public ResponseEntity<MyApiResponse<List<FlightTracking>>> getFlightHistory(
            @Parameter(description = "Aircraft hexident (ICAO24 code)", example = "A12345") @PathVariable String hexident,

            @Parameter(description = "Start time for history query", example = "2024-12-01T00:00:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,

            @Parameter(description = "End time for history query", example = "2024-12-01T23:59:59") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime) {

        try {
            List<FlightTracking> history = queryService.getFlightHistory(hexident, fromTime, toTime);

            return ResponseEntity.ok(MyApiResponse.<List<FlightTracking>>builder()
                    .success(true)
                    .message(String.format("Found %d tracking points for flight %s", history.size(), hexident))
                    .data(history)
                    .timestamp(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving flight history for hexident: {}", hexident, e);
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.<List<FlightTracking>>builder()
                            .success(false)
                            .message("Failed to retrieve flight history: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Get recent flight tracking data", description = "Retrieves recent tracking data for a specific flight (last 24 hours)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved recent flight data"),
            @ApiResponse(responseCode = "404", description = "No recent flight data found")
    })
    @GetMapping("/flight/{hexident}/recent")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getRecentFlightData(
            @Parameter(description = "Aircraft hexident (ICAO24 code)", example = "A12345") @PathVariable String hexident) {

        try {
            Map<String, Object> recentData = queryService.getRecentFlightData(hexident);

            return ResponseEntity.ok(MyApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Recent flight data retrieved successfully")
                    .data(recentData)
                    .timestamp(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving recent flight data for hexident: {}", hexident, e);
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Failed to retrieve recent flight data: " + e.getMessage())
                            .build());
        }
    }

    // ============================================================================
    // SHIP HISTORY ENDPOINTS
    // ============================================================================

    @Operation(summary = "Get ship tracking history", description = "Retrieves historical tracking data for a specific ship by MMSI within a time range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved ship history"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "No ship data found for the given MMSI")
    })
    @GetMapping("/ship/{mmsi}")
    public ResponseEntity<MyApiResponse<List<ShipTracking>>> getShipHistory(
            @Parameter(description = "Ship MMSI (Maritime Mobile Service Identity)", example = "574123456") @PathVariable String mmsi,

            @Parameter(description = "Start time for history query", example = "2024-12-01T00:00:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,

            @Parameter(description = "End time for history query", example = "2024-12-01T23:59:59") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime) {

        try {
            List<ShipTracking> history = queryService.getShipHistory(mmsi, fromTime, toTime);

            return ResponseEntity.ok(MyApiResponse.<List<ShipTracking>>builder()
                    .success(true)
                    .message(String.format("Found %d tracking points for ship %s", history.size(), mmsi))
                    .data(history)
                    .timestamp(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving ship history for MMSI: {}", mmsi, e);
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.<List<ShipTracking>>builder()
                            .success(false)
                            .message("Failed to retrieve ship history: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Get recent ship tracking data", description = "Retrieves recent tracking data for a specific ship (last 24 hours)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved recent ship data"),
            @ApiResponse(responseCode = "404", description = "No recent ship data found")
    })
    @GetMapping("/ship/{mmsi}/recent")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getRecentShipData(
            @Parameter(description = "Ship MMSI (Maritime Mobile Service Identity)", example = "574123456") @PathVariable String mmsi) {

        try {
            Map<String, Object> recentData = queryService.getRecentShipData(mmsi);

            return ResponseEntity.ok(MyApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Recent ship data retrieved successfully")
                    .data(recentData)
                    .timestamp(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving recent ship data for MMSI: {}", mmsi, e);
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Failed to retrieve recent ship data: " + e.getMessage())
                            .build());
        }
    }

    // ============================================================================
    // GEOGRAPHIC QUERY ENDPOINTS
    // ============================================================================

    @Operation(summary = "Get flights in area", description = "Retrieves current flights within a specified geographic area")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved flights in area"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates")
    })
    @GetMapping("/flights/area")
    public ResponseEntity<MyApiResponse<List<FlightTrackingRequest>>> getFlightsInArea(
            @Parameter(description = "Minimum latitude", example = "10.0") @RequestParam Double minLat,

            @Parameter(description = "Maximum latitude", example = "11.0") @RequestParam Double maxLat,

            @Parameter(description = "Minimum longitude", example = "106.0") @RequestParam Double minLon,

            @Parameter(description = "Maximum longitude", example = "107.0") @RequestParam Double maxLon) {

        try {
            // Validate coordinates
            if (minLat >= maxLat || minLon >= maxLon) {
                return ResponseEntity.badRequest()
                        .body(MyApiResponse.<List<FlightTrackingRequest>>builder()
                                .success(false)
                                .message("Invalid coordinates: min values must be less than max values")
                                .build());
            }

            List<FlightTrackingRequest> flights = queryService.getFlightsInArea(minLat, maxLat, minLon, maxLon);

            return ResponseEntity.ok(MyApiResponse.<List<FlightTrackingRequest>>builder()
                    .success(true)
                    .message(String.format("Found %d flights in specified area", flights.size()))
                    .data(flights)
                    .timestamp(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving flights in area", e);
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.<List<FlightTrackingRequest>>builder()
                            .success(false)
                            .message("Failed to retrieve flights in area: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Get ships in area", description = "Retrieves current ships within a specified geographic area")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved ships in area"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates")
    })
    @GetMapping("/ships/area")
    public ResponseEntity<MyApiResponse<List<ShipTrackingRequest>>> getShipsInArea(
            @Parameter(description = "Minimum latitude", example = "10.0") @RequestParam Double minLat,

            @Parameter(description = "Maximum latitude", example = "11.0") @RequestParam Double maxLat,

            @Parameter(description = "Minimum longitude", example = "106.0") @RequestParam Double minLon,

            @Parameter(description = "Maximum longitude", example = "107.0") @RequestParam Double maxLon) {

        try {
            // Validate coordinates
            if (minLat >= maxLat || minLon >= maxLon) {
                return ResponseEntity.badRequest()
                        .body(MyApiResponse.<List<ShipTrackingRequest>>builder()
                                .success(false)
                                .message("Invalid coordinates: min values must be less than max values")
                                .build());
            }

            List<ShipTrackingRequest> ships = queryService.getShipsInArea(minLat, maxLat, minLon, maxLon);

            return ResponseEntity.ok(MyApiResponse.<List<ShipTrackingRequest>>builder()
                    .success(true)
                    .message(String.format("Found %d ships in specified area", ships.size()))
                    .data(ships)
                    .timestamp(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving ships in area", e);
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.<List<ShipTrackingRequest>>builder()
                            .success(false)
                            .message("Failed to retrieve ships in area: " + e.getMessage())
                            .build());
        }
    }

    // ============================================================================
    // SYSTEM STATISTICS ENDPOINT
    // ============================================================================

    @Operation(summary = "Get system tracking statistics", description = "Retrieves overall system statistics including active flights, ships, and recent activity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved system statistics")
    })
    @GetMapping("/statistics")
    public ResponseEntity<MyApiResponse<Map<String, Object>>> getSystemStatistics() {

        try {
            Map<String, Object> statistics = queryService.getSystemStatistics();

            return ResponseEntity.ok(MyApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("System statistics retrieved successfully")
                    .data(statistics)
                    .timestamp(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving system statistics", e);
            return ResponseEntity.internalServerError()
                    .body(MyApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Failed to retrieve system statistics: " + e.getMessage())
                            .build());
        }
    }
}