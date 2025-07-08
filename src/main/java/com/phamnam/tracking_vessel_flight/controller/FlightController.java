package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.dto.response.FlightResponse;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;

import com.phamnam.tracking_vessel_flight.models.Flight;
import com.phamnam.tracking_vessel_flight.service.rest.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/flights")
@Tag(name = "Flight Controller", description = "APIs for flight management")
public class FlightController {

        @Autowired
        private FlightService flightService;

        @Operation(summary = "Get all flights", description = "Retrieves a list of all flights")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved flights"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<MyApiResponse<List<FlightResponse>>> getAll() {
                try {
                        return ResponseEntity.ok(MyApiResponse.success(flightService.getAll()));
                } catch (Exception e) {
                        log.error("Error getting all flights", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(MyApiResponse.error("Error retrieving flights: " + e.getMessage()));
                }
        }

        @Operation(summary = "Get all flights with pagination", description = "Retrieves a paginated list of flights")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved flights"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/paginated")
        public ResponseEntity<MyApiResponse<Page<FlightResponse>>> getAllPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String sortDir) {
                try {
                        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending()
                                        : Sort.by(sortBy).ascending();
                        Pageable pageable = PageRequest.of(page, size, sort);

                        Page<FlightResponse> flightPage = flightService.getAllPaginated(pageable);
                        return ResponseEntity.ok(MyApiResponse.success(flightPage));
                } catch (Exception e) {
                        log.error("Error getting paginated flights", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(MyApiResponse.error("Error retrieving flights: " + e.getMessage()));
                }
        }

        @Operation(summary = "Get flight by ID", description = "Retrieves a specific flight by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the flight"),
                        @ApiResponse(responseCode = "404", description = "Flight not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<MyApiResponse<FlightResponse>> getFlightById(@PathVariable Long id) {
                try {
                        return ResponseEntity.ok(MyApiResponse.success(flightService.getFlightById(id)));
                } catch (ResourceNotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(MyApiResponse.error(e.getMessage()));
                }
        }

        @Operation(summary = "Get flights by aircraft ID", description = "Retrieves all flights for a specific aircraft")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved flights for the aircraft"),
                        @ApiResponse(responseCode = "404", description = "Aircraft not found")
        })
        @GetMapping("/aircraft/{aircraftId}")
        public ResponseEntity<MyApiResponse<List<FlightResponse>>> getFlightsByAircraftId(
                        @PathVariable Long aircraftId) {
                try {
                        return ResponseEntity.ok(MyApiResponse.success(
                                        flightService.getFlightsByAircraftId(aircraftId),
                                        "Flights retrieved successfully"));
                } catch (Exception e) {
                        log.error("Error getting flights by aircraft ID: {}", aircraftId, e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(MyApiResponse.error("Error retrieving flights: " + e.getMessage()));
                }
        }

        @Operation(summary = "Create new flight", description = "Creates a new flight record")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Flight created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<MyApiResponse<FlightResponse>> createFlight(
                        @Valid @RequestBody FlightRequest flightRequest, @RequestParam(required = false) Long userId) {
                try {
                        FlightResponse savedFlight = flightService.save(flightRequest, userId);
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(MyApiResponse.success(savedFlight, "Flight created successfully"));
                } catch (Exception e) {
                        log.error("Error creating flight", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(MyApiResponse.error("Error creating flight: " + e.getMessage()));
                }
        }

        @Operation(summary = "Update flight", description = "Updates an existing flight by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Flight updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Flight not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PutMapping("/{id}")
        public ResponseEntity<MyApiResponse<FlightResponse>> updateFlight(@PathVariable Long id,
                        @Valid @RequestBody FlightRequest flightRequest, @RequestParam(required = false) Long userId) {
                try {
                        FlightResponse updatedFlight = flightService.updateFlight(id, flightRequest, userId);
                        return ResponseEntity.ok(MyApiResponse.success(updatedFlight, "Flight updated successfully"));
                } catch (ResourceNotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(MyApiResponse.error(e.getMessage()));
                } catch (Exception e) {
                        log.error("Error updating flight with ID: {}", id, e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(MyApiResponse.error("Error updating flight: " + e.getMessage()));
                }
        }

        @Operation(summary = "Delete flight", description = "Deletes a flight by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Flight deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Flight not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<MyApiResponse<Void>> deleteFlight(@PathVariable Long id) {
                flightService.deleteFlight(id);
                return ResponseEntity.ok(MyApiResponse.success(null, "Flight deleted successfully"));
        }
}