
package com.phamnam.tracking_vessel_flight.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaSubscriptionRequest {
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
}
