package com.phamnam.tracking_vessel_flight.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightRequest {
    @NotBlank(message = "Hexident is required")
    private String hexident;

    @NotBlank(message = "Register is required")
    private String register;

    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Manufacture is required")
    private String manufacture;

    private String constructorNumber;

    @NotBlank(message = "Operator is required")
    private String operator;

    private String operatorCode;

    @Pattern(regexp = "^[0-9]+$", message = "Engines must be a number")
    private String engines;

    private String engineType;

    private Boolean isMilitary;

    private String country;

    private String transponderType;

    private Integer year;

    private String source;

    private Integer itemType;
}
