package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.UserRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.dto.response.PageResponse;
import com.phamnam.tracking_vessel_flight.dto.response.UserResponse;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.service.rest.UserService;
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
@RequestMapping("/api/users")
@Tag(name = "User Controller", description = "APIs for user management")
public class UserController {

        @Autowired
        private UserService userService;

        @Operation(summary = "Get all users", description = "Retrieves a list of all users")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping
        public ResponseEntity<MyApiResponse<List<UserResponse>>> getAllUsers() {
                return ResponseEntity.ok(MyApiResponse.success(userService.getAllUsers()));
        }

        @Operation(summary = "Get all users with pagination", description = "Retrieves a paginated list of users")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        @GetMapping("/paginated")
        public ResponseEntity<MyApiResponse<PageResponse<UserResponse>>> getAllUsersPaginated(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                                : Sort.Direction.ASC;

                Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
                Page<UserResponse> userPage = userService.getAllPaginated(pageable);

                return ResponseEntity.ok(MyApiResponse.success(
                                PageResponse.fromPage(userPage),
                                "Users retrieved successfully"));
        }

        @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the user"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        @GetMapping("/{id}")
        public ResponseEntity<MyApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
                return ResponseEntity.ok(MyApiResponse.success(userService.getUserById(id)));
        }

        @Operation(summary = "Create new user", description = "Creates a new user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "User created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<MyApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest userRequest) {
                UserResponse savedUser = userService.createUser(userRequest);
                return new ResponseEntity<>(
                                MyApiResponse.success(savedUser, "User created successfully"),
                                HttpStatus.CREATED);
        }

        @Operation(summary = "Update user", description = "Updates an existing user by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User updated successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found"),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PutMapping("/{id}")
        public ResponseEntity<MyApiResponse<UserResponse>> updateUser(
                        @PathVariable Long id,
                        @Valid @RequestBody UserRequest userRequest) {
                UserResponse updatedUser = userService.updateUser(id, userRequest);
                return ResponseEntity.ok(MyApiResponse.success(updatedUser, "User updated successfully"));
        }

        @Operation(summary = "Delete user", description = "Deletes a user by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<MyApiResponse<Void>> deleteUser(@PathVariable Long id) {
                userService.deleteUser(id);
                return ResponseEntity.ok(MyApiResponse.success(null, "User deleted successfully"));
        }
}
