package com.phamnam.tracking_vessel_flight.dto.response.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for ADS-B Exchange API
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdsbExchangeResponse {

    @JsonProperty("aircraft")
    private List<AdsbExchangeAircraftData> aircraft;

    @JsonProperty("flights")
    private List<AdsbExchangeAircraftData> flights; // Some APIs use "flights"

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("now")
    private Long now;

    @JsonProperty("messages")
    private Long messages;

    // Helper method to get aircraft from either field
    public List<AdsbExchangeAircraftData> getActualAircraft() {
        return aircraft != null ? aircraft : flights;
    }
}
