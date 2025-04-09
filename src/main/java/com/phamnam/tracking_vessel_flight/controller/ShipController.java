package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.ShipRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.models.Ship;
import com.phamnam.tracking_vessel_flight.service.ShipService;
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
@RequestMapping("/api/ships")
@Tag(name = "Ship Controller", description = "APIs for ship management")
public class ShipController {
    @Autowired
    private ShipService shipService;

    @Operation(summary = "Get all ships", description = "Retrieves a list of all ships")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved ships"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<MyApiResponse<List<Ship>>> getAllShips() {
        return ResponseEntity.ok(MyApiResponse.success(shipService.getAll()));
    }

    @Operation(summary = "Get all ships with pagination", description = "Retrieves a paginated list of ships")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved ships"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/paginated")
    public ResponseEntity<MyApiResponse<PageResponse<Ship>>> getAllShipsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<Ship> shipPage = shipService.getAllPaginated(pageable);

        return ResponseEntity.ok(MyApiResponse.success(
                PageResponse.fromPage(shipPage),
                "Ships retrieved successfully"));
    }

    @Operation(summary = "Get ship by ID", description = "Retrieves a specific ship by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the ship"),
            @ApiResponse(responseCode = "404", description = "Ship not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<MyApiResponse<Ship>> getShipById(@PathVariable Long id) {
        return ResponseEntity.ok(MyApiResponse.success(shipService.getShipById(id)));
    }

    @Operation(summary = "Create new ship", description = "Creates a new ship record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ship created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<MyApiResponse<Ship>> createShip(
            @Valid @RequestBody ShipRequest shipRequest,
            @RequestParam(required = false) Long userId) {
        Ship savedShip = shipService.save(shipRequest, userId);
        return new ResponseEntity<>(
                MyApiResponse.success(savedShip, "Ship created successfully"),
                HttpStatus.CREATED);
    }

    @Operation(summary = "Update ship", description = "Updates an existing ship by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ship updated successfully"),
            @ApiResponse(responseCode = "404", description = "Ship not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<MyApiResponse<Ship>> updateShip(
            @PathVariable Long id,
            @Valid @RequestBody ShipRequest shipRequest,
            @RequestParam(required = false) Long userId) {
        Ship updatedShip = shipService.updateShip(id, shipRequest, userId);
        return ResponseEntity.ok(MyApiResponse.success(updatedShip, "Ship updated successfully"));
    }

    @Operation(summary = "Delete ship", description = "Deletes a ship by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ship deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Ship not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<MyApiResponse<Void>> deleteShip(@PathVariable Long id) {
        shipService.deleteShip(id);
        return ResponseEntity.ok(MyApiResponse.success(null, "Ship deleted successfully"));
    }
}
