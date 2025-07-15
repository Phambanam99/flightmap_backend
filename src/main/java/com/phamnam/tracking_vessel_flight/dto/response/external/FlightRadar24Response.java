package com.phamnam.tracking_vessel_flight.dto.response.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * FlightRadar24 API response structure
 * Data format: {"full_count": 50000, "version": 4, "hexident":
 * [aircraft_data_array], ...}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightRadar24Response {

    @JsonProperty("full_count")
    private Integer fullCount;

    @JsonProperty("version")
    private Integer version;

    @JsonProperty("stats")
    private Object stats;

    /**
     * Get all aircraft data as a map where:
     * - Key: hex identifier (aircraft unique ID)
     * - Value: array of aircraft data in specific order
     */
    public Map<String, Object> getAllAircraftData() {
        // This will be handled by ObjectMapper to capture all remaining fields
        // that are not full_count, version, or stats
        return null; // Will be populated by custom deserialization if needed
    }
}
