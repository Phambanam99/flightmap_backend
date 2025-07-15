package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.dto.response.ShipTrackingResponse;
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
@RequestMapping("/api/vessels")
@Tag(name = "Vessel Controller", description = "APIs for vessel tracking management (alias for ship tracking)")
public class VesselController {
    @Autowired
    private ShipTrackingService shipTrackingService;

    @Operation(summary = "Get all vessels", description = "Retrieves a list of all vessel tracking records")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved vessels"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<MyApiResponse<List<ShipTrackingResponse>>> getAllVessels() {
        return ResponseEntity.ok(MyApiResponse.success(shipTrackingService.getAll()));
    }

    @Operation(summary = "Get all vessels with pagination", description = "Retrieves a paginated list of vessel tracking records")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved vessels"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/paginated")
    public ResponseEntity<MyApiResponse<PageResponse<ShipTrackingResponse>>> getAllVesselsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ShipTrackingResponse> vesselPage = shipTrackingService.getAllPaginated(pageable);

        return ResponseEntity.ok(MyApiResponse.success(
                PageResponse.fromPage(vesselPage),
                "Vessels retrieved successfully"));
    }

    @Operation(summary = "Get vessel by ID", description = "Retrieves a specific vessel tracking record by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the vessel"),
            @ApiResponse(responseCode = "404", description = "Vessel not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<ShipTrackingResponse>> getVesselById(@PathVariable Long id) {
        return ResponseEntity.ok(MyApiResponse.success(shipTrackingService.getShipTrackingById(id)));
    }

    @Operation(summary = "Create new vessel tracking", description = "Creates a new vessel tracking record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vessel tracking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<MyApiResponse<ShipTrackingResponse>> createVessel(
            @Valid @RequestBody ShipTrackingRequest vesselRequest,
            @RequestParam(required = false) Long userId) {
        ShipTrackingResponse savedVessel = shipTrackingService.save(vesselRequest, userId);
        return new ResponseEntity<>(
                MyApiResponse.success(savedVessel, "Vessel tracking created successfully"),
                HttpStatus.CREATED);
    }

    @Operation(summary = "Update vessel tracking", description = "Updates an existing vessel tracking by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vessel tracking updated successfully"),
            @ApiResponse(responseCode = "404", description = "Vessel tracking not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<MyApiResponse<ShipTrackingResponse>> updateVessel(
            @PathVariable Long id,
            @Valid @RequestBody ShipTrackingRequest vesselRequest,
            @RequestParam(required = false) Long userId) {
        ShipTrackingResponse updatedVessel = shipTrackingService.updateShipTracking(id, vesselRequest, userId);
        return ResponseEntity.ok(MyApiResponse.success(updatedVessel, "Vessel tracking updated successfully"));
    }

    @Operation(summary = "Delete vessel tracking", description = "Deletes a vessel tracking by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vessel tracking deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Vessel tracking not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<MyApiResponse<Void>> deleteVessel(@PathVariable Long id) {
        shipTrackingService.deleteShipTracking(id);
        return ResponseEntity.ok(MyApiResponse.success(null, "Vessel tracking deleted successfully"));
    }
}
