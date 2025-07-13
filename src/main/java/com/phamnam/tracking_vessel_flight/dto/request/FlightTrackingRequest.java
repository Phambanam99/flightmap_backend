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

    // Missing fields for IntelligentStorageService
    private String hexIdent; // Aircraft identifier
    private Double groundSpeed; // Ground speed in knots
    private LocalDateTime lastSeen; // Last seen timestamp
    private String track; // Track/heading as string
    private Double verticalRate; // Vertical rate
    private String alert; // Alert status
    private String emergency; // Emergency status
    private String spi; // SPI status
    private Boolean isOnGround; // On ground flag

    private Double altitude; // Changed from Float to Double
    private String altitudeType;
    private Double targetAlt; // Changed from Float to Double
    private String callsign;
    private Double speed; // Changed from Float to Double (for compatibility)
    private String speedType;
    private Double verticalSpeed; // Changed from Float to Double
    private String squawk; // Changed from Integer to String for emergency codes
    private Double distance; // Changed from Float to Double
    private Double bearing; // Changed from Float to Double
    private Long unixTime;
    private LocalDateTime updateTime;

    // For location (since JTS Point can't be directly deserialized)
    private Double longitude;
    private Double latitude;

    private Long landingUnixTimes;
    private LocalDateTime landingTimes;
}
