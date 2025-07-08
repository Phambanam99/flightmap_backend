package com.phamnam.tracking_vessel_flight.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightRequest {
    @NotNull(message = "Aircraft ID is required")
    private Long aircraftId;

    private String callsign;

    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    private String status;

    private String originAirport;
    private String destinationAirport;
}
