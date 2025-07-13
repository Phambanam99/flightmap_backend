package com.phamnam.tracking_vessel_flight.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "MMSI number is required")
    private String mmsi;

    private String imo;

    private String callsign;

    @NotBlank(message = "Ship type is required")
    private String shipType;

    private String flag;

    private Double length;

    private Double width;

    private Integer buildYear;
}
