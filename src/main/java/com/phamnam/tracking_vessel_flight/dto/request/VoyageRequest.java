package com.phamnam.tracking_vessel_flight.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class VoyageRequest {
    private String voyageNumber;

    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    @NotBlank(message = "Departure port is required")
    private String departurePort;

    @NotBlank(message = "Arrival port is required")
    private String arrivalPort;

    @NotNull(message = "Ship ID is required")
    private Long shipId;
}
