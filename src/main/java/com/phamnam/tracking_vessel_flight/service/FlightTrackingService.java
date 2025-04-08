package com.phamnam.tracking_vessel_flight.service;

import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.repository.FlightRepository;
import com.phamnam.tracking_vessel_flight.repository.FlightTrackingRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.service.interfaces.IFlightTrackingService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlightTrackingService implements IFlightTrackingService {
    @Autowired
    private FlightTrackingRepository flightTrackingRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private UserRepository userRepository;

    // Using SRID 4326 for WGS84 (standard for geographic coordinates)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public List<FlightTracking> getAll() {
        return flightTrackingRepository.findAll();
    }

    public Page<FlightTracking> getAllPaginated(Pageable pageable) {
        return flightTrackingRepository.findAll(pageable);
    }

    public FlightTracking getById(Long id) {
        return flightTrackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FlightTracking", "id", id));
    }

    public List<FlightTracking> findWithinRadius(double longitude, double latitude, double radiusInMeters) {
        return flightTrackingRepository.findWithinRadius(longitude, latitude, radiusInMeters);
    }

    public Page<FlightTracking> findWithinRadius(double longitude, double latitude, double radiusInMeters,
            Pageable pageable) {
        return flightTrackingRepository.findWithinRadiusPaginated(longitude, latitude, radiusInMeters, pageable);
    }

    public FlightTracking save(FlightTrackingRequest request, Long userId) {
        Aircraft aircraft = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", request.getFlightId()));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        // Create point with correct SRID
        Point location = null;
        if (request.getLongitude() != null && request.getLatitude() != null) {
            // Make sure we're creating the coordinate in the right order (longitude first,
            // then latitude)
            Coordinate coordinate = new Coordinate(request.getLongitude(), request.getLatitude());
            location = geometryFactory.createPoint(coordinate);
            // Explicitly set SRID
            location.setSRID(4326);
        }

        FlightTracking tracking = FlightTracking.builder()
                .aircraft(aircraft)
                .altitude(request.getAltitude())
                .altitudeType(request.getAltitudeType())
                .targetAlt(request.getTargetAlt())
                .callsign(request.getCallsign())
                .speed(request.getSpeed())
                .speedType(request.getSpeedType())
                .verticalSpeed(request.getVerticalSpeed())
                .squawk(request.getSquawk())
                .distance(request.getDistance())
                .bearing(request.getBearing())
                .unixTime(request.getUnixTime())
                .updateTime(request.getUpdateTime() != null ? request.getUpdateTime() : LocalDateTime.now())
                .location(location)
                .landingUnixTimes(request.getLandingUnixTimes())
                .landingTimes(request.getLandingTimes())
                .build();

        tracking.setUpdatedBy(user);

        return flightTrackingRepository.save(tracking);
    }

    public FlightTracking update(Long id, FlightTrackingRequest request, Long userId) {
        FlightTracking tracking = getById(id);
        Aircraft aircraft = null;

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        if (request.getFlightId() != null) {
            aircraft = flightRepository.findById(request.getFlightId())
                    .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", request.getFlightId()));
            tracking.setAircraft(aircraft);
        }

        if (request.getLongitude() != null && request.getLatitude() != null) {
            // Create point with correct SRID
            Coordinate coordinate = new Coordinate(request.getLongitude(), request.getLatitude());
            Point location = geometryFactory.createPoint(coordinate);
            location.setSRID(4326);
            tracking.setLocation(location);
        }

        // Update other fields if provided
        if (request.getAltitude() != null)
            tracking.setAltitude(request.getAltitude());
        if (request.getAltitudeType() != null)
            tracking.setAltitudeType(request.getAltitudeType());
        if (request.getTargetAlt() != null)
            tracking.setTargetAlt(request.getTargetAlt());
        if (request.getCallsign() != null)
            tracking.setCallsign(request.getCallsign());
        if (request.getSpeed() != null)
            tracking.setSpeed(request.getSpeed());
        if (request.getSpeedType() != null)
            tracking.setSpeedType(request.getSpeedType());
        if (request.getVerticalSpeed() != null)
            tracking.setVerticalSpeed(request.getVerticalSpeed());
        if (request.getSquawk() != null)
            tracking.setSquawk(request.getSquawk());
        if (request.getDistance() != null)
            tracking.setDistance(request.getDistance());
        if (request.getBearing() != null)
            tracking.setBearing(request.getBearing());
        if (request.getUnixTime() != null)
            tracking.setUnixTime(request.getUnixTime());
        if (request.getUpdateTime() != null)
            tracking.setUpdateTime(request.getUpdateTime());
        if (request.getLandingUnixTimes() != null)
            tracking.setLandingUnixTimes(request.getLandingUnixTimes());
        if (request.getLandingTimes() != null)
            tracking.setLandingTimes(request.getLandingTimes());

        tracking.setUpdatedAt(LocalDateTime.now());
        tracking.setUpdatedBy(user);

        return flightTrackingRepository.save(tracking);
    }

    public void delete(Long id) {
        FlightTracking tracking = getById(id);
        flightTrackingRepository.delete(tracking);
    }

    /**
     * Find all tracking data for a specific flight by its ID
     * 
     * @param flightId the ID of the flight to get tracking data for
     * @return list of tracking data for the flight
     */
    public List<FlightTracking> getByFlightId(Long flightId) {
        // Verify flight exists first
        flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", flightId));

        return flightTrackingRepository.findByAircraft_id(flightId);
    }

    public Page<FlightTracking> getByFlightId(Long flightId, Pageable pageable) {
        return flightTrackingRepository.findByAircraft_id(flightId, pageable);
    }
}
