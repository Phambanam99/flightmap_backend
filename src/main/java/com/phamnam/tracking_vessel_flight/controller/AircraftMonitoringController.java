package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftMonitoringRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.models.AircraftMonitoring;
import com.phamnam.tracking_vessel_flight.service.AircraftMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aircraft-monitoring")
@Tag(name = "Aircraft Monitoring Controller", description = "APIs for aircraft monitoring management")
public class AircraftMonitoringController {

    @Autowired
    private AircraftMonitoringService aircraftMonitoringService;

    @Operation(summary = "Get all aircraft monitoring entries", description = "Retrieves a list of all aircraft monitoring entries")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved aircraft monitoring entries"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<MyApiResponse<List<AircraftMonitoring>>> getAllAircraftMonitoring() {
        return ResponseEntity.ok(MyApiResponse.success(aircraftMonitoringService.getAll()));
    }

    @Operation(summary = "Get all aircraft monitoring entries with pagination", description = "Retrieves a paginated list of aircraft monitoring entries")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved aircraft monitoring entries"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/paginated")
    public ResponseEntity<MyApiResponse<PageResponse<AircraftMonitoring>>> getAllAircraftMonitoringPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<AircraftMonitoring> monitoringPage = aircraftMonitoringService.getAllPaginated(pageable);

        return ResponseEntity.ok(MyApiResponse.success(
                PageResponse.fromPage(monitoringPage),
                "Aircraft monitoring entries retrieved successfully"));
    }

    @Operation(summary = "Get aircraft monitoring by ID", description = "Retrieves a specific aircraft monitoring entry by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the aircraft monitoring entry"),
            @ApiResponse(responseCode = "404", description = "Aircraft monitoring entry not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<AircraftMonitoring>> getAircraftMonitoringById(@PathVariable Long id) {
        return ResponseEntity.ok(MyApiResponse.success(aircraftMonitoringService.getById(id)));
    }

    @Operation(summary = "Get aircraft monitoring entries by user ID", description = "Retrieves all aircraft monitoring entries for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved monitoring entries for the user")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<MyApiResponse<List<AircraftMonitoring>>> getAircraftMonitoringByUserId(
            @PathVariable Long userId) {
        List<AircraftMonitoring> monitoringEntries = aircraftMonitoringService.getByUserId(userId);
        return ResponseEntity.ok(MyApiResponse.success(monitoringEntries, "Monitoring entries retrieved successfully"));
    }

    @Operation(summary = "Get aircraft monitoring entries by aircraft ID", description = "Retrieves all monitoring entries for a specific aircraft")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved monitoring entries for the aircraft")
    })
    @GetMapping("/aircraft/{aircraftId}")
    public ResponseEntity<MyApiResponse<List<AircraftMonitoring>>> getAircraftMonitoringByAircraftId(
            @PathVariable Long aircraftId) {
        List<AircraftMonitoring> monitoringEntries = aircraftMonitoringService.getByAircraftId(aircraftId);
        return ResponseEntity.ok(MyApiResponse.success(monitoringEntries, "Monitoring entries retrieved successfully"));
    }

    @Operation(summary = "Create new aircraft monitoring entry", description = "Creates a new aircraft monitoring entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Aircraft monitoring entry created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<MyApiResponse<AircraftMonitoring>> createAircraftMonitoring(
            @Valid @RequestBody AircraftMonitoringRequest request) {
        AircraftMonitoring savedMonitoring = aircraftMonitoringService.save(request);
        return new ResponseEntity<>(
                MyApiResponse.success(savedMonitoring, "Aircraft monitoring entry created successfully"),
                HttpStatus.CREATED);
    }

    @Operation(summary = "Delete aircraft monitoring", description = "Deletes an aircraft monitoring entry by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aircraft monitoring entry deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Aircraft monitoring entry not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<MyApiResponse<Void>> deleteAircraftMonitoring(@PathVariable Long id) {
        aircraftMonitoringService.delete(id);
        return ResponseEntity.ok(MyApiResponse.success(null, "Aircraft monitoring entry deleted successfully"));
    }

    @Operation(summary = "Delete aircraft monitoring by user and aircraft", description = "Deletes monitoring entries for a specific user and aircraft")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aircraft monitoring entries deleted successfully")
    })
    @DeleteMapping("/user/{userId}/aircraft/{aircraftId}")
    public ResponseEntity<MyApiResponse<Void>> deleteAircraftMonitoringByUserAndAircraft(
            @PathVariable Long userId, @PathVariable Long aircraftId) {
        aircraftMonitoringService.deleteByUserIdAndAircraftId(userId, aircraftId);
        return ResponseEntity.ok(MyApiResponse.success(null, "Aircraft monitoring entries deleted successfully"));
    }
}
