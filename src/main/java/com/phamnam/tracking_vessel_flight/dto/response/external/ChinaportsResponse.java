package com.phamnam.tracking_vessel_flight.dto.response.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Chinaports API
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChinaportsResponse {

    @JsonProperty("vessels")
    private List<ChinaportsVesselData> vessels;

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("status")
    private String status;
}
