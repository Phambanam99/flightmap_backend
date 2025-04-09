package com.phamnam.tracking_vessel_flight.service.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IShipTrackingService {
    List<ShipTracking> getAll();

    Page<ShipTracking> getAllPaginated(Pageable pageable);

    ShipTracking getShipTrackingById(Long id);

    ShipTracking save(ShipTrackingRequest shipTrackingRequest, Long userId);

    void deleteShipTracking(Long id);

    ShipTracking updateShipTracking(Long id, ShipTrackingRequest shipTrackingRequest, Long userId);

    List<ShipTracking> getTrackingsByVoyageId(Long voyageId);

    /**
     * Process a new ship tracking data point and assign it to the appropriate
     * voyage.
     * If no active voyage exists for the ship, a new voyage will be created.
     * 
     * @param shipId       The ID of the ship being tracked
     * @param trackingData The new tracking data (lat, long, speed, etc.)
     * @param userId       The user ID for audit purposes (optional)
     * @return The saved ShipTracking entity
     */
    ShipTracking processNewTrackingData(Long shipId, ShipTrackingRequest trackingData, Long userId);
}
