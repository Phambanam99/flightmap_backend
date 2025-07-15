package com.phamnam.tracking_vessel_flight.dto.response.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for MarineTraffic Main API (used by ExternalApiService)
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarineTrafficResponse {

    @JsonProperty("vessels")
    private List<MarineTrafficVesselData> vessels;

    @JsonProperty("data")
    private List<MarineTrafficVesselData> data; // Some APIs use "data" instead of "vessels"

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("success")
    private Boolean success;

    // Helper method to get vessels from either field
    public List<MarineTrafficVesselData> getActualVessels() {
        return vessels != null ? vessels : data;
    }
}
