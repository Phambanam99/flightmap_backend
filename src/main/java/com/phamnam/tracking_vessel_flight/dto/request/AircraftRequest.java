package com.phamnam.tracking_vessel_flight.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AircraftRequest {
    @NotBlank(message = "Hex identifier (icao24) is required")
    private String hexident;

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
