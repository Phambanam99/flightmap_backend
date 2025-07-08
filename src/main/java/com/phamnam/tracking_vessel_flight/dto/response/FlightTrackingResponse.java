package com.phamnam.tracking_vessel_flight.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightTrackingResponse {
    private Long id;

    // Flight information
    private Long flightId;
    private String callsign;
    private String aircraftRegistration;
    private String aircraftModel;

    // Location data
    private Double longitude;
    private Double latitude;
    private Float altitude;
    private String altitudeType;
    private Float targetAlt;

    // Speed and direction
    private Float speed;
    private String speedType;
    private Float verticalSpeed;
    private Float bearing;

    // Technical data
    private Integer squawk;
    private Float distance;
    private Long unixTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String updatedByUsername;

    // Landing data (simplified)
    private String landingUnixTimes;
    private String landingTimes;
}