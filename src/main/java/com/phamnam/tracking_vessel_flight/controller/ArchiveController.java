package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.ColdStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/archive")
@RequiredArgsConstructor
@Tag(name = "Archive Controller", description = "APIs for accessing and managing archived data")
public class ArchiveController {

    private final ColdStorageService coldStorageService;

    @Operation(summary = "Get archived flight tracking data", description = "Retrieves historical flight tracking data for a specific flight within a time range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved archive data"),
            @ApiResponse(responseCode = "404", description = "Flight not found or no data available"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<MyApiResponse<List<FlightTracking>>> getFlightArchiveData(
            @PathVariable Long flightId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<FlightTracking> archiveData = coldStorageService.queryFlightTrackingHistory(flightId, startTime, endTime);

        return ResponseEntity.ok(
                MyApiResponse.<List<FlightTracking>>builder()
                        .success(true)
                        .data(archiveData)
                        .build());
    }

    @Operation(summary = "Trigger manual archiving process", description = "Initiates a manual archiving process for data older than the specified date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Archiving process initiated"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "400", description = "Invalid cutoff date")
    })
    @PostMapping("/manual-archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MyApiResponse<String>> triggerManualArchive(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cutoffDate) {

        coldStorageService.performDataArchiving();

        return ResponseEntity.ok(
                MyApiResponse.<String>builder()
                        .success(true)
                        .message("Manual archiving process initiated for data older than " + cutoffDate)
                        .build());
    }
}