package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.ShipMonitoringRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.dto.response.ShipMonitoringResponse;
import com.phamnam.tracking_vessel_flight.models.ShipMonitoring;
import com.phamnam.tracking_vessel_flight.service.rest.ShipMonitoringService;
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
@RequestMapping("/api/ship-monitoring")
@Tag(name = "Ship Monitoring Controller", description = "APIs for ship monitoring management")
public class ShipMonitoringController {

        @Autowired
        private ShipMonitoringService shipMonitoringService;

        @Operation(summary = "Get all ship monitoring entries", description = "Retrieves a list of all ship monitoring entries")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved ship monitoring entries"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<MyApiResponse<List<ShipMonitoringResponse>>> getAll() {
                return ResponseEntity.ok(MyApiResponse.success(shipMonitoringService.getAll()));
        }

        @Operation(summary = "Get all ship monitoring entries with pagination", description = "Retrieves a paginated list of ship monitoring entries")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved ship monitoring entries"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<ShipMonitoringResponse>>> getAllPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
                Page<ShipMonitoringResponse> monitoringPage = shipMonitoringService.getAllPaginated(pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(monitoringPage),
                                "Ship monitoring retrieved successfully"));
        }

        @Operation(summary = "Get ship monitoring by ID", description = "Retrieves a specific ship monitoring entry by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the ship monitoring entry"),
                        @ApiResponse(responseCode = "404", description = "Ship monitoring entry not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<MyApiResponse<ShipMonitoringResponse>> getById(@PathVariable Long id) {
                return ResponseEntity.ok(MyApiResponse.success(shipMonitoringService.getById(id)));
        }

        @Operation(summary = "Get ship monitoring entries by user ID", description = "Retrieves all ship monitoring entries for a specific user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved monitoring entries for the user")
        })
        @GetMapping("/user/{userId}")
        public ResponseEntity<MyApiResponse<List<ShipMonitoringResponse>>> getByUserId(@PathVariable Long userId) {
                List<ShipMonitoringResponse> monitoringEntries = shipMonitoringService.getByUserId(userId);
                return ResponseEntity.ok(MyApiResponse.success(monitoringEntries,
                                "User ship monitoring retrieved successfully"));
        }

        @Operation(summary = "Get ship monitoring entries by ship ID", description = "Retrieves all monitoring entries for a specific ship")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved monitoring entries for the ship")
        })
        @GetMapping("/ship/{shipId}")
        public ResponseEntity<MyApiResponse<List<ShipMonitoringResponse>>> getByShipId(@PathVariable Long shipId) {
                List<ShipMonitoringResponse> monitoringEntries = shipMonitoringService.getByShipId(shipId);
                return ResponseEntity
                                .ok(MyApiResponse.success(monitoringEntries, "Ship monitoring retrieved successfully"));
        }

        @Operation(summary = "Create new ship monitoring entry", description = "Creates a new ship monitoring entry")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Ship monitoring entry created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<MyApiResponse<ShipMonitoringResponse>> save(
                        @Valid @RequestBody ShipMonitoringRequest request) {
                ShipMonitoringResponse savedMonitoring = shipMonitoringService.save(request);
                return new ResponseEntity<>(
                                MyApiResponse.success(savedMonitoring, "Ship monitoring created successfully"),
                                HttpStatus.CREATED);
        }

        @Operation(summary = "Delete ship monitoring", description = "Deletes a ship monitoring entry by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Ship monitoring entry deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Ship monitoring entry not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<MyApiResponse<Void>> deleteShipMonitoring(@PathVariable Long id) {
                shipMonitoringService.delete(id);
                return ResponseEntity.ok(MyApiResponse.success(null, "Ship monitoring entry deleted successfully"));
        }

        @Operation(summary = "Delete ship monitoring by user and ship", description = "Deletes monitoring entries for a specific user and ship")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Ship monitoring entries deleted successfully")
        })
        @DeleteMapping("/user/{userId}/ship/{shipId}")
        public ResponseEntity<MyApiResponse<Void>> deleteShipMonitoringByUserAndShip(
                        @PathVariable Long userId, @PathVariable Long shipId) {
                shipMonitoringService.deleteByUserIdAndShipId(userId, shipId);
                return ResponseEntity.ok(MyApiResponse.success(null, "Ship monitoring entries deleted successfully"));
        }
}
