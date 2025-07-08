package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.response.FlightTrackingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IFlightTrackingService {
    List<FlightTrackingResponse> getAll();

    Page<FlightTrackingResponse> getAllPaginated(Pageable pageable);

    FlightTrackingResponse getById(Long id);

    List<FlightTrackingResponse> findWithinRadius(double longitude, double latitude, double radiusInMeters);

    Page<FlightTrackingResponse> findWithinRadius(double longitude, double latitude, double radiusInMeters,
            Pageable pageable);

    // FlightTracking save(FlightTrackingRequest request, Long userId);

    FlightTrackingResponse update(Long id, FlightTrackingRequest request, Long userId);

    void delete(Long id);

    List<FlightTrackingResponse> getByFlightId(Long flightId);

    Page<FlightTrackingResponse> getByFlightId(Long flightId, Pageable pageable);

    /**
     * Process a new flight tracking data point and assign it to the appropriate
     * flight.
     * If no active flight exists for the aircraft, a new flight will be created.
     *
     * @param trackingData The new tracking data (location, altitude, speed, etc.)
     * @param userId       The user ID for audit purposes (optional)
     * @return The saved FlightTracking entity
     */
    // FlightTracking processNewTrackingData(Long aircraftId, FlightTrackingRequest
    // trackingData, Long userId);

    @Transactional
    FlightTrackingResponse processNewTrackingData(FlightTrackingRequestDTO trackingData, Long userId);
}
