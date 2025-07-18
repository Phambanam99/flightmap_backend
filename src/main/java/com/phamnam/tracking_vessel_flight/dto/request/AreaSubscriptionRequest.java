
package com.phamnam.tracking_vessel_flight.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AreaSubscriptionRequest {
    @JsonProperty("requestId")
    private String requestId; // Add support for requestId from frontend

    private Double minLatitude; // Changed from minLat to match expected method names
    private Double maxLatitude; // Changed from maxLat to match expected method names
    private Double minLongitude; // Changed from minLon to match expected method names
    private Double maxLongitude; // Changed from maxLon to match expected method names

    // Legacy compatibility methods
    public Double getMinLat() {
        return minLatitude;
    }

    public Double getMaxLat() {
        return maxLatitude;
    }

    public Double getMinLon() {
        return minLongitude;
    }

    public Double getMaxLon() {
        return maxLongitude;
    }

    public void setMinLat(Double minLat) {
        this.minLatitude = minLat;
    }

    public void setMaxLat(Double maxLat) {
        this.maxLatitude = maxLat;
    }

    public void setMinLon(Double minLon) {
        this.minLongitude = minLon;
    }

    public void setMaxLon(Double maxLon) {
        this.maxLongitude = maxLon;
    }
}
