package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftRequest;
import com.phamnam.tracking_vessel_flight.dto.response.AircraftResponse;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.service.rest.AircraftService;
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
@RequestMapping("/api/aircraft")
@Tag(name = "Aircraft Controller", description = "APIs for aircraft management")
public class AircraftController {

        @Autowired
        private AircraftService aircraftService;

        @Operation(summary = "Get all aircraft", description = "Retrieves a list of all aircraft")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved aircraft"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<MyApiResponse<List<AircraftResponse>>> getAll() {
                return ResponseEntity.ok(MyApiResponse.success(aircraftService.getAll()));
        }

        @Operation(summary = "Get all aircraft with pagination", description = "Retrieves a paginated list of aircraft")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved aircraft"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<AircraftResponse>>> getAllPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
                Page<AircraftResponse> aircraftPage = aircraftService.getAllPaginated(pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(aircraftPage),
                                "Aircraft retrieved successfully"));
        }

        @Operation(summary = "Get aircraft by ID", description = "Retrieves a specific aircraft by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the aircraft"),
                        @ApiResponse(responseCode = "404", description = "Aircraft not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<MyApiResponse<AircraftResponse>> getAircraftById(@PathVariable Long id) {
                return ResponseEntity.ok(MyApiResponse.success(aircraftService.getAircraftById(id)));
        }

        @Operation(summary = "Create new aircraft", description = "Creates a new aircraft record")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Aircraft created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<MyApiResponse<AircraftResponse>> createAircraft(
                        @Valid @RequestBody AircraftRequest aircraftRequest,
                        @RequestParam(required = false) Long userId) {
                AircraftResponse savedAircraft = aircraftService.save(aircraftRequest, userId);
                return new ResponseEntity<>(
                                MyApiResponse.success(savedAircraft, "Aircraft created successfully"),
                                HttpStatus.CREATED);
        }

        @Operation(summary = "Update aircraft", description = "Updates an existing aircraft by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Aircraft updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Aircraft not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PutMapping("/{id}")
        public ResponseEntity<MyApiResponse<AircraftResponse>> updateAircraft(
                        @PathVariable Long id,
                        @Valid @RequestBody AircraftRequest aircraftRequest,
                        @RequestParam(required = false) Long userId) {
                AircraftResponse updatedAircraft = aircraftService.updateAircraft(id, aircraftRequest, userId);
                return ResponseEntity.ok(MyApiResponse.success(updatedAircraft, "Aircraft updated successfully"));
        }

        @Operation(summary = "Delete aircraft", description = "Deletes an aircraft by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Aircraft deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Aircraft not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<MyApiResponse<Void>> deleteAircraft(@PathVariable Long id) {
                aircraftService.deleteAircraft(id);
                return ResponseEntity.ok(MyApiResponse.success(null, "Aircraft deleted successfully"));
        }

        @Operation(summary = "Find aircraft by hex identifier", description = "Retrieves an aircraft by its hex identifier (icao24)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the aircraft"),
                        @ApiResponse(responseCode = "404", description = "Aircraft not found")
        })
        @GetMapping("/search/{hexident}")
        public ResponseEntity<MyApiResponse<AircraftResponse>> getAircraftByHexident(@PathVariable String hexident) {
                return ResponseEntity.ok(MyApiResponse.success(aircraftService.findByHexident(hexident)));
        }
}
