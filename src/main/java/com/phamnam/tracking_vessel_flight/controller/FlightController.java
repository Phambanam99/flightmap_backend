package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.service.FlightService;
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
        public ResponseEntity<MyApiResponse<List<Aircraft>>> getAllFlights() {
                return ResponseEntity.ok(MyApiResponse.success(flightService.getAll()));
        }

        @Operation(summary = "Get all flights with pagination", description = "Retrieves a paginated list of flights")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved flights"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<Aircraft>>> getAllFlightsPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
                Page<Aircraft> flightPage = flightService.getAllPaginated(pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(flightPage),
                                "Flights retrieved successfully"));
        }

        @Operation(summary = "Get flight by ID", description = "Retrieves a specific flight by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the flight"),
                        @ApiResponse(responseCode = "404", description = "Flight not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<MyApiResponse<Aircraft>> getFlightById(@PathVariable Long id) {
                return ResponseEntity.ok(MyApiResponse.success(flightService.getFlightById(id)));
        }

        @Operation(summary = "Create new flight", description = "Creates a new flight record")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Flight created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<MyApiResponse<Aircraft>> createFlight(
                        @Valid @RequestBody FlightRequest flightRequest,
                        @RequestParam(required = false) Long userId) {
                Aircraft savedAircraft = flightService.save(flightRequest, userId);
                return new ResponseEntity<>(
                                MyApiResponse.success(savedAircraft, "Flight created successfully"),
                                HttpStatus.CREATED);
        }

        @Operation(summary = "Update flight", description = "Updates an existing flight by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Flight updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Flight not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PutMapping("/{id}")
        public ResponseEntity<MyApiResponse<Aircraft>> updateFlight(
                        @PathVariable Long id,
                        @Valid @RequestBody FlightRequest flightRequest,
                        @RequestParam(required = false) Long userId) {
                Aircraft updatedAircraft = flightService.updateFlight(id, flightRequest, userId);
                return ResponseEntity.ok(MyApiResponse.success(updatedAircraft, "Flight updated successfully"));
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