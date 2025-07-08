package com.phamnam.tracking_vessel_flight.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipSubscriptionRequest {

    @NotEmpty(message = "MMSI is required")
    @JsonProperty("mmsi")
    private String mmsi;

    @JsonProperty("includeHistory")
    private Boolean includeHistory;

    @JsonProperty("historyHours")
    private Integer historyHours;
}