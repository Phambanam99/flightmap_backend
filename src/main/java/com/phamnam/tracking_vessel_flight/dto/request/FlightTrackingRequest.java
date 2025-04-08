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
public class FlightTrackingRequest {
    @NotNull(message = "Flight ID is required")
    private Long flightId;

    private Float altitude;
    private String altitudeType;
    private Float targetAlt;
    private String callsign;
    private Float speed;
    private String speedType;
    private Float verticalSpeed;
    private Integer squawk;
    private Float distance;
    private Float bearing;
    private Long unixTime;
    private LocalDateTime updateTime;

    // For location (since JTS Point can't be directly deserialized)
    private Double longitude;
    private Double latitude;

    private Long landingUnixTimes;
    private LocalDateTime landingTimes;
}
