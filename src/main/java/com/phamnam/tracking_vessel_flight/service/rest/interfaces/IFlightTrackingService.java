package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IFlightTrackingService {
    List<FlightTracking> getAll();

    Page<FlightTracking> getAllPaginated(Pageable pageable);

    FlightTracking getById(Long id);

    List<FlightTracking> findWithinRadius(double longitude, double latitude, double radiusInMeters);

    Page<FlightTracking> findWithinRadius(double longitude, double latitude, double radiusInMeters, Pageable pageable);

    FlightTracking save(FlightTrackingRequest request, Long userId);

    FlightTracking update(Long id, FlightTrackingRequest request, Long userId);

    void delete(Long id);

    List<FlightTracking> getByFlightId(Long flightId);

    Page<FlightTracking> getByFlightId(Long flightId, Pageable pageable);

    /**
     * Process a new flight tracking data point and assign it to the appropriate
     * flight.
     * If no active flight exists for the aircraft, a new flight will be created.
     * 
     * @param aircraftId   The ID of the aircraft being tracked
     * @param trackingData The new tracking data (location, altitude, speed, etc.)
     * @param userId       The user ID for audit purposes (optional)
     * @return The saved FlightTracking entity
     */
    FlightTracking processNewTrackingData(Long aircraftId, FlightTrackingRequest trackingData, Long userId);

    @Transactional
    FlightTracking processNewTrackingData(FlightTrackingRequestDTO trackingData, Long userId);
}
