package com.phamnam.tracking_vessel_flight.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AircraftTrackingRequest {
    private String hexident;
    private Double latitude;
    private Double longitude;
    private Integer altitude;
    private Integer groundSpeed;
    private Integer track;
    private Integer verticalRate;
    private String squawk;
    private String aircraftType;
    private String registration;
    private String callsign;
    private String origin;
    private String destination;
    private String flightNumber;
    private Boolean onGround;
    private LocalDateTime timestamp;
    private Double dataQuality;

    // Additional fields for enhanced tracking
    private String airline;
    private String route;
    private Boolean emergency;
    private String flightStatus;
    private Integer heading;
    private Double magneticHeading;
    private Double trueAirspeed;
    private Integer windDirection;
    private Integer windSpeed;
    private Double temperature;
    private String transponderCode;
}