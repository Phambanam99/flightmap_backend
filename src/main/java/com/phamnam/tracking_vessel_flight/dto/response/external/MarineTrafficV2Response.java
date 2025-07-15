package com.phamnam.tracking_vessel_flight.dto.response.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for MarineTraffic V2 API
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarineTrafficV2Response {

    @JsonProperty("vessels")
    private List<MarineTrafficV2VesselData> vessels;

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;
}
