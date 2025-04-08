package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.VesselRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.models.Vessel;
import com.phamnam.tracking_vessel_flight.service.VesselService;
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
@Tag(name = "Vessel Controller", description = "APIs for vessel management")
public class VesselController {
        @Autowired
        private VesselService vesselService;

        @Operation(summary = "Get all vessels", description = "Retrieves a list of all vessels")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved vessels"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<MyApiResponse<List<Vessel>>> getAllVessels() {
                return ResponseEntity.ok(MyApiResponse.success(vesselService.getAll()));
        }

        @Operation(summary = "Get all vessels with pagination", description = "Retrieves a paginated list of vessels")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved vessels"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<Vessel>>> getAllVesselsPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
                Page<Vessel> vesselPage = vesselService.getAllPaginated(pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(vesselPage),
                                "Vessels retrieved successfully"));
        }

        @Operation(summary = "Get vessel by ID", description = "Retrieves a specific vessel by its ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the vessel"),
                        @ApiResponse(responseCode = "404", description = "Vessel not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<MyApiResponse<Vessel>> getVesselById(@PathVariable Long id) {
                return ResponseEntity.ok(MyApiResponse.success(vesselService.getVesselById(id)));
        }

        @Operation(summary = "Create new vessel", description = "Creates a new vessel record")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Vessel created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<MyApiResponse<Vessel>> createVessel(
                        @Valid @RequestBody VesselRequest vesselRequest,
                        @RequestParam(required = false) Long userId) {
                Vessel savedVessel = vesselService.save(vesselRequest, userId);
                return new ResponseEntity<>(
                                MyApiResponse.success(savedVessel, "Vessel created successfully"),
                                HttpStatus.CREATED);
        }

        @Operation(summary = "Update vessel", description = "Updates an existing vessel by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Vessel updated successfully"),
                        @ApiResponse(responseCode = "404", description = "Vessel not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PutMapping("/{id}")
        public ResponseEntity<MyApiResponse<Vessel>> updateVessel(
                        @PathVariable Long id,
                        @Valid @RequestBody VesselRequest vesselRequest,
                        @RequestParam(required = false) Long userId) {
                Vessel updatedVessel = vesselService.updateVessel(id, vesselRequest, userId);
                return ResponseEntity.ok(MyApiResponse.success(updatedVessel, "Vessel updated successfully"));
        }

        @Operation(summary = "Delete vessel", description = "Deletes a vessel by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Vessel deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Vessel not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<MyApiResponse<Void>> deleteVessel(@PathVariable Long id) {
                vesselService.deleteVessel(id);
                return ResponseEntity.ok(MyApiResponse.success(null, "Vessel deleted successfully"));
        }
}
