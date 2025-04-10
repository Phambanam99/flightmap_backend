package com.phamnam.tracking_vessel_flight.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
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
public class FlightReceiveDTO {


    @NotBlank(message = "Hex identifier (icao24) is required")
    private String hexident;

    private String callsign;

    // Timing Info
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String status;

    private Long unixTime;
    private LocalDateTime updateTime;

    private Long landingUnixTimes;
    private LocalDateTime landingTimes;

    // Flight Route
    private String originAirport;
    private String destinationAirport;

    // Flight Position & Movement
    private Float altitude;
    private String altitudeType;
    private Float targetAlt;
    private Float speed;
    private String speedType;
    private Float verticalSpeed;
    private Integer squawk;
    private Float distance;
    private Float bearing;

    // Location
    private Double longitude;
    private Double latitude;

    // Aircraft Info
    private String register;
    private String type;
    private String manufacture;
    private String constructorNumber;
    private String operator;
    private String operatorCode;
    private String engines;
    private String engineType;
    private Boolean isMilitary;
    private String country;
    private String transponderType;
    private Integer year;
    private String source;

    private Integer itemType;
}
