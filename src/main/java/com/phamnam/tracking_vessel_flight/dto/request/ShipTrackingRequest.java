package com.phamnam.tracking_vessel_flight.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipTrackingRequest {
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    // Missing fields for IntelligentStorageService
    @NotNull(message = "MMSI is required")
    private String mmsi; // Maritime Mobile Service Identity
    private Double heading; // Ship heading
    private String navStatus; // Navigation status

    private Double speed;
    private Double course;
    private Double draught;

    @NotNull(message = "Voyage ID is required")
    private Long voyageId;
}
