package com.phamnam.tracking_vessel_flight.service.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
