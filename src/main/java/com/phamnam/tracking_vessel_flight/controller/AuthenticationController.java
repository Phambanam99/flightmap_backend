package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.models.auth.AuthenticationRequest;
import com.phamnam.tracking_vessel_flight.models.auth.AuthenticationResponse;
import com.phamnam.tracking_vessel_flight.models.auth.LogoutRequest;
import com.phamnam.tracking_vessel_flight.models.auth.RefreshTokenRequest;
import com.phamnam.tracking_vessel_flight.models.auth.RegisterRequest;
import com.phamnam.tracking_vessel_flight.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "Login", description = "Authenticates a user and returns JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<MyApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(
                MyApiResponse.success(authenticationService.authenticate(request), "Login successful"));
    }

    @Operation(summary = "Register", description = "Registers a new user and returns JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or user already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<MyApiResponse<AuthenticationResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return new ResponseEntity<>(
                MyApiResponse.success(authenticationService.register(request), "Registration successful"),
                HttpStatus.CREATED);
    }

    @Operation(summary = "Refresh token", description = "Gets a new access token using a refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<MyApiResponse<AuthenticationResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(
                MyApiResponse.success(authenticationService.refreshToken(request), "Token refreshed successfully"));
    }

    @Operation(summary = "Logout", description = "Invalidates the current JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token")
    })
    @PostMapping("/logout")
    public ResponseEntity<MyApiResponse<Void>> logout(@RequestBody LogoutRequest request) {
        authenticationService.logout(request);
        return ResponseEntity.ok(MyApiResponse.success(null, "Logged out successfully"));
    }
}
