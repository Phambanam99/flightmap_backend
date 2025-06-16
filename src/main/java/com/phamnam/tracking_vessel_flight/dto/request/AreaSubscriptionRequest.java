
package com.phamnam.tracking_vessel_flight.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaSubscriptionRequest {
    private float minLat;
    private float maxLat;
    private float minLon;
    private float maxLon;

}
