package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.VoyageRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.dto.response.VoyageResponse;
import com.phamnam.tracking_vessel_flight.models.Voyage;
import com.phamnam.tracking_vessel_flight.service.rest.VoyageService;
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
@RequestMapping("/api/voyages")
@Tag(name = "Voyage Controller", description = "APIs for voyage management")
public class VoyageController {
        @Autowired
        private VoyageService voyageService;

        @Operation(summary = "Get all voyages", description = "Retrieves a list of all voyages")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved voyages"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<MyApiResponse<List<VoyageResponse>>> getAll() {
                return ResponseEntity.ok(MyApiResponse.success(voyageService.getAll()));
        }

        @Operation(summary = "Get all voyages with pagination", description = "Retrieves a paginated list of voyages")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved voyages"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<VoyageResponse>>> getAllPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
                Page<VoyageResponse> voyagePage = voyageService.getAllPaginated(pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(voyagePage),
                                "Voyages retrieved successfully"));
        }

        @Operation(summary = "Get voyage by ID", description = "Retrieves a specific voyage by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the voyage"),
                        @ApiResponse(responseCode = "404", description = "Voyage not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<MyApiResponse<VoyageResponse>> getVoyageById(@PathVariable Long id) {
                return ResponseEntity.ok(MyApiResponse.success(voyageService.getVoyageById(id)));
        }

        @Operation(summary = "Get voyages by ship ID", description = "Retrieves all voyages for a specific ship")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved voyages"),
                        @ApiResponse(responseCode = "404", description = "Ship not found")
        })
        @GetMapping("/ship/{shipId}")
        public ResponseEntity<MyApiResponse<List<VoyageResponse>>> getVoyagesByShipId(@PathVariable Long shipId) {
                return ResponseEntity.ok(MyApiResponse.success(voyageService.getVoyagesByShipId(shipId)));
        }

        @Operation(summary = "Create new voyage", description = "Creates a new voyage record")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Voyage created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<MyApiResponse<VoyageResponse>> createVoyage(
                        @Valid @RequestBody VoyageRequest voyageRequest,
                        @RequestParam(required = false) Long userId) {
                VoyageResponse savedVoyage = voyageService.save(voyageRequest, userId);
                return new ResponseEntity<>(
                                MyApiResponse.success(savedVoyage, "Voyage created successfully"),
                                HttpStatus.CREATED);
        }

        @Operation(summary = "Update voyage", description = "Updates an existing voyage by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Voyage updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Voyage not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PutMapping("/{id}")
        public ResponseEntity<MyApiResponse<VoyageResponse>> updateVoyage(
                        @PathVariable Long id,
                        @Valid @RequestBody VoyageRequest voyageRequest,
                        @RequestParam(required = false) Long userId) {
                VoyageResponse updatedVoyage = voyageService.updateVoyage(id, voyageRequest, userId);
                return ResponseEntity.ok(MyApiResponse.success(updatedVoyage, "Voyage updated successfully"));
        }

        @Operation(summary = "Delete voyage", description = "Deletes a voyage by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Voyage deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Voyage not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<MyApiResponse<Void>> deleteVoyage(@PathVariable Long id) {
                voyageService.deleteVoyage(id);
                return ResponseEntity.ok(MyApiResponse.success(null, "Voyage deleted successfully"));
        }
}
