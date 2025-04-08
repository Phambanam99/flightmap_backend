package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.service.FlightTrackingService;
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
@RequestMapping("/api/flight-tracking")
@Tag(name = "Flight Tracking Controller", description = "APIs for flight tracking management")
public class FlightTrackingController {

        @Autowired
        private FlightTrackingService flightTrackingService;

        @Operation(summary = "Get all flight tracking data", description = "Retrieves a list of all flight tracking records")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved flight tracking data"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<MyApiResponse<List<FlightTracking>>> getAllFlightTrackings() {
                return ResponseEntity.ok(MyApiResponse.success(flightTrackingService.getAll()));
        }

        @Operation(summary = "Get all flight tracking data with pagination", description = "Retrieves a paginated list of flight tracking records")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved flight tracking data"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<FlightTracking>>> getAllFlightTrackingsPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
                Page<FlightTracking> trackingPage = flightTrackingService.getAllPaginated(pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(trackingPage),
                                "Flight tracking data retrieved successfully"));
        }

        @Operation(summary = "Get flight tracking by ID", description = "Retrieves a specific flight tracking record by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the flight tracking"),
                        @ApiResponse(responseCode = "404", description = "Flight tracking not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<MyApiResponse<FlightTracking>> getFlightTrackingById(@PathVariable Long id) {
                return ResponseEntity.ok(MyApiResponse.success(flightTrackingService.getById(id)));
        }

        @Operation(summary = "Find flights within radius", description = "Finds all flight tracking records within specified radius from a point")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved flights within radius"),
                        @ApiResponse(responseCode = "400", description = "Invalid parameters")
        })
        @GetMapping("/radius")
        public ResponseEntity<MyApiResponse<List<FlightTracking>>> findWithinRadius(
                        @RequestParam double longitude,
                        @RequestParam double latitude,
                        @RequestParam double radiusInMeters) {
                return ResponseEntity.ok(MyApiResponse.success(
                                flightTrackingService.findWithinRadius(longitude, latitude, radiusInMeters)));
        }

        @Operation(summary = "Find flights within radius with pagination", description = "Finds all flight tracking records within specified radius with pagination")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved flights within radius"),
                        @ApiResponse(responseCode = "400", description = "Invalid parameters")
        })
        @GetMapping("/radius/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<FlightTracking>>> findWithinRadiusPaginated(
                        @RequestParam double longitude,
                        @RequestParam double latitude,
                        @RequestParam double radiusInMeters,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                Pageable pageable = PageRequest.of(page, size);
                Page<FlightTracking> trackingPage = flightTrackingService.findWithinRadius(
                                longitude, latitude, radiusInMeters, pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(trackingPage),
                                "Flight tracking data within radius retrieved successfully"));
        }

        @Operation(summary = "Create new flight tracking", description = "Creates a new flight tracking record")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Flight tracking created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<MyApiResponse<FlightTracking>> createFlightTracking(
                        @Valid @RequestBody FlightTrackingRequest request,
                        @RequestParam(required = false) Long userId) {
                FlightTracking savedTracking = flightTrackingService.save(request, userId);
                return new ResponseEntity<>(
                                MyApiResponse.success(savedTracking, "Flight tracking created successfully"),
                                HttpStatus.CREATED);
        }

        @Operation(summary = "Update flight tracking", description = "Updates an existing flight tracking record by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Flight tracking updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Flight tracking not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PutMapping("/{id}")
        public ResponseEntity<MyApiResponse<FlightTracking>> updateFlightTracking(
                        @PathVariable Long id,
                        @Valid @RequestBody FlightTrackingRequest request,
                        @RequestParam(required = false) Long userId) {
                FlightTracking updatedTracking = flightTrackingService.update(id, request, userId);
                return ResponseEntity
                                .ok(MyApiResponse.success(updatedTracking, "Flight tracking updated successfully"));
        }

        @Operation(summary = "Delete flight tracking", description = "Deletes a flight tracking record by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Flight tracking deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Flight tracking not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<MyApiResponse<Void>> deleteFlightTracking(@PathVariable Long id) {
                flightTrackingService.delete(id);
                return ResponseEntity.ok(MyApiResponse.success(null, "Flight tracking deleted successfully"));
        }

        @Operation(summary = "Get tracking data by flight ID", description = "Retrieves all tracking records for a specific flight")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved tracking data for the flight"),
                        @ApiResponse(responseCode = "404", description = "Flight not found")
        })
        @GetMapping("/flight/{flightId}")
        public ResponseEntity<MyApiResponse<List<FlightTracking>>> getTrackingByFlightId(@PathVariable Long flightId) {
                List<FlightTracking> trackingData = flightTrackingService.getByFlightId(flightId);
                return ResponseEntity.ok(MyApiResponse.success(trackingData, "Tracking data retrieved successfully"));
        }

        @Operation(summary = "Get tracking data by flight ID with pagination", description = "Retrieves paginated tracking records for a specific flight")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved tracking data for the flight"),
                        @ApiResponse(responseCode = "404", description = "Flight not found")
        })
        @GetMapping("/flight/{flightId}/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<FlightTracking>>> getTrackingByFlightIdPaginated(
                        @PathVariable Long flightId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "timestamp") String sortBy,
                        @RequestParam(defaultValue = "desc") String direction) {

                Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
                Page<FlightTracking> trackingPage = flightTrackingService.getByFlightId(flightId, pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(trackingPage),
                                "Tracking data for flight retrieved successfully"));
        }
}
