package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.dto.response.ShipTrackingResponse;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import com.phamnam.tracking_vessel_flight.service.rest.ShipTrackingService;
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
@RequestMapping("/api/ship-trackings")
@Tag(name = "Ship Tracking Controller", description = "APIs for ship tracking management")
public class ShipTrackingController {
        @Autowired
        private ShipTrackingService shipTrackingService;

        @Operation(summary = "Get all ship trackings", description = "Retrieves a list of all ship trackings")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved ship trackings"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<MyApiResponse<List<ShipTrackingResponse>>> getAll() {
                return ResponseEntity.ok(MyApiResponse.success(shipTrackingService.getAll()));
        }

        @Operation(summary = "Get all ship trackings with pagination", description = "Retrieves a paginated list of ship trackings")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved ship trackings"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<ShipTrackingResponse>>> getAllPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
                Page<ShipTrackingResponse> shipTrackingPage = shipTrackingService.getAllPaginated(pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(shipTrackingPage),
                                "Ship tracking retrieved successfully"));
        }

        @Operation(summary = "Get ship tracking by ID", description = "Retrieves a specific ship tracking by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the ship tracking"),
                        @ApiResponse(responseCode = "404", description = "Ship tracking not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<MyApiResponse<ShipTrackingResponse>> getShipTrackingById(@PathVariable Long id) {
                return ResponseEntity.ok(MyApiResponse.success(shipTrackingService.getShipTrackingById(id)));
        }

        @Operation(summary = "Get trackings by voyage ID", description = "Retrieves all trackings for a specific voyage")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved trackings"),
                        @ApiResponse(responseCode = "404", description = "Voyage not found")
        })
        @GetMapping("/voyage/{voyageId}")
        public ResponseEntity<MyApiResponse<List<ShipTrackingResponse>>> getShipTrackingsByVoyageId(
                        @PathVariable Long voyageId) {
                return ResponseEntity.ok(MyApiResponse.success(shipTrackingService.getTrackingsByVoyageId(voyageId)));
        }

        @Operation(summary = "Create new ship tracking", description = "Creates a new ship tracking record")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Ship tracking created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<MyApiResponse<ShipTrackingResponse>> createShipTracking(
                        @Valid @RequestBody ShipTrackingRequest shipTrackingRequest,
                        @RequestParam(required = false) Long userId) {
                ShipTrackingResponse savedShipTracking = shipTrackingService.save(shipTrackingRequest, userId);
                return new ResponseEntity<>(
                                MyApiResponse.success(savedShipTracking, "Ship tracking created successfully"),
                                HttpStatus.CREATED);
        }

        @Operation(summary = "Update ship tracking", description = "Updates an existing ship tracking by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Ship tracking updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Ship tracking not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PutMapping("/{id}")
        public ResponseEntity<MyApiResponse<ShipTrackingResponse>> updateShipTracking(
                        @PathVariable Long id,
                        @Valid @RequestBody ShipTrackingRequest shipTrackingRequest,
                        @RequestParam(required = false) Long userId) {
                ShipTrackingResponse updatedShipTracking = shipTrackingService.updateShipTracking(id,
                                shipTrackingRequest, userId);
                return ResponseEntity
                                .ok(MyApiResponse.success(updatedShipTracking, "Ship tracking updated successfully"));
        }

        @Operation(summary = "Delete ship tracking", description = "Deletes a ship tracking by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Ship tracking deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Ship tracking not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<MyApiResponse<Void>> deleteShipTracking(@PathVariable Long id) {
                shipTrackingService.deleteShipTracking(id);
                return ResponseEntity.ok(MyApiResponse.success(null, "Ship tracking deleted successfully"));
        }

        @Operation(summary = "Process new ship tracking data", description = "Processes new tracking data for a ship, creates voyage if needed")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Ship tracking processed successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "404", description = "Ship not found")
        })
        @PostMapping("/process")
        public ResponseEntity<MyApiResponse<ShipTrackingResponse>> processNewTrackingData(
                        @RequestParam Long shipId,
                        @Valid @RequestBody ShipTrackingRequest trackingData,
                        @RequestParam(required = false) Long userId) {
                ShipTrackingResponse savedShipTracking = shipTrackingService.processNewTrackingData(
                                shipId, trackingData, userId);
                return new ResponseEntity<>(
                                MyApiResponse.success(savedShipTracking, "Ship tracking processed successfully"),
                                HttpStatus.CREATED);
        }
}
