package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Flight;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.repository.FlightRepository;
import com.phamnam.tracking_vessel_flight.repository.FlightTrackingRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.IFlightTrackingService;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Slf4j
@Service
public class FlightTrackingService implements IFlightTrackingService {
    @Autowired
    private FlightTrackingRepository flightTrackingRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlightRepository aircraftRepository;

    @Autowired
    private FlightService flightService;

    // Using SRID 4326 for WGS84 (standard for geographic coordinates)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    // Maximum time a flight can be inactive (for example, 2 hours)
    private static final Duration MAX_FLIGHT_INACTIVITY = Duration.ofHours(2);

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

    public FlightTracking save(FlightTrackingRequestDTO request, Long userId) {
        Flight flight = flightRepository.findById(request.getFlightId())
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
                .flight(flight)
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
        Flight flight = null;

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        if (request.getFlightId() != null) {
            flight = flightRepository.findById(request.getFlightId())
                    .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", request.getFlightId()));
            tracking.setFlight(flight);
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

        return flightTrackingRepository.findByFlight_id(flightId);
    }

    public Page<FlightTracking> getByFlightId(Long flightId, Pageable pageable) {
        return flightTrackingRepository.findByFlight_id(flightId, pageable);
    }

    /**
     * Process a new flight tracking data point and assign it to the appropriate
     * flight.
     * If no active flight exists for the aircraft, a new flight will be created.
     * 

     * @param trackingData The new tracking data (location, altitude, speed, etc.)
     * @param userId       The user ID for audit purposes (optional)
     * @return The saved FlightTracking entity
     */
    @Transactional
    @Override
    public FlightTracking processNewTrackingData(FlightTrackingRequestDTO trackingData, Long userId) {
        // Find the aircraft
        Long aircraftId = trackingData.getAircraftId();
        Aircraft aircraft = null;
        if (aircraftId == null) {
          aircraft = Aircraft.builder()
                    .hexident(trackingData.getHexident())
                    .register(trackingData.getRegister())
                    .isMilitary(trackingData.getIsMilitary())
                    .country(trackingData.getCountry())
                    .type(trackingData.getType())
                    .manufacture(trackingData.getManufacture())
                    .operator(trackingData.getOperator())
                    .operatorCode(trackingData.getOperatorCode())
                    .engines(trackingData.getEngines())
                    .engineType(trackingData.getEngineType())
                    .transponderType(trackingData.getTransponderType())
                    .year(trackingData.getYear())
                    .source(trackingData.getSource())
                    .itemType(trackingData.getItemType())
                    .build();
        } else {
            aircraft = aircraftRepository.findById(aircraftId)
                    .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", aircraftId))
                    .getAircraft();
        }


        // Get user for audit if needed
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        // Find an active flight for this aircraft
        Flight flight = findActiveFlightForAircraft(aircraftId);

        // If no active flight exists, create a new one
        if (flight == null) {
            flight = createNewFlight(aircraft, trackingData, userId);
        } else {
            // Update flight status if needed based on tracking data
            updateFlightStatus(flight, trackingData, userId);
        }

        // Update the tracking data to use the flight ID
        trackingData.setFlight(flight.getId().toString());

        // Create and save the tracking data
        FlightTracking tracking = save(trackingData, userId);

        return tracking;
    }

    /**
     * Find an active flight for the given aircraft.
     * A flight is considered active if it has a departure time but no arrival time,
     * or if the current time is between departure and arrival times.
     * 
     * @param aircraftId the ID of the aircraft
     * @return the active Flight or null if none exists
     */
    private Flight findActiveFlightForAircraft(Long aircraftId) {
        // Use the repository method to get the latest flight for this aircraft
        Flight latestFlight = flightRepository.findLatestByAircraftId(aircraftId);

        // If no flight exists or it's null, return null
        if (latestFlight == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        // Flight has departed but not arrived
        if (latestFlight.getDepartureTime() != null && latestFlight.getArrivalTime() == null) {
            // Check if flight is not too old (within the inactivity period)
            if (latestFlight.getDepartureTime().isAfter(now.minus(MAX_FLIGHT_INACTIVITY))) {
                return latestFlight;
            }
        }

        // Flight has both departure and arrival times - check if current time is in
        // between
        if (latestFlight.getDepartureTime() != null && latestFlight.getArrivalTime() != null) {
            if (now.isAfter(latestFlight.getDepartureTime()) && now.isBefore(latestFlight.getArrivalTime())) {
                return latestFlight;
            }
        }

        return null;
    }


    // Thêm vào implementation class của bạn
    @Scheduled(cron = "0 0 2 * * ?") // Chạy vào 2h sáng mỗi ngày
    public void archiveOldData() {
        log.info("Starting archival of old flight tracking data");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        // 1. Truy vấn dữ liệu cũ
        List<FlightTracking> oldData = flightTrackingRepository
                .findByUpdateTimeBefore(cutoffDate);

        if (oldData.isEmpty()) {
            log.info("No old data to archive");
            return;
        }

        log.info("Found {} records to archive", oldData.size());

        // 2. Lưu vào cold storage (có thể là bảng riêng hoặc hệ thống lưu trữ khác)
        try {
            archiveToDatabase(oldData);
            // hoặc
            // archiveToFileSystem(oldData);

            // 3. Xóa dữ liệu đã được lưu trữ khỏi warm storage
            flightTrackingRepository.deleteByUpdateTimeBefore(cutoffDate);

            log.info("Successfully archived and cleaned up {} old records", oldData.size());
        } catch (Exception e) {
            log.error("Error archiving old data: {}", e.getMessage(), e);
        }
    }


    private void archiveToDatabase(List<FlightTracking> oldData) {
        // Triển khai lưu trữ vào cold storage database
        // Ví dụ: coldStorageRepository.saveAll(oldData);
    }
    /**
     * Update flight status based on tracking data
     * 
     * @param flight       the flight to update
     * @param trackingData the new tracking data
     * @param userId       the user ID for audit
     * @return the updated Flight
     */
    private Flight updateFlightStatus(Flight flight, FlightTrackingRequestDTO trackingData, Long userId) {
        boolean updated = false;

        // Get the latest tracking for this flight to compare with new data
        Optional<FlightTracking> latestTracking = flightTrackingRepository.findLastTrackingByFlightId(flight.getId());

        // If there's previous tracking data, we can make some decisions based on
        // changes
        if (latestTracking.isPresent()) {
            FlightTracking prevTracking = latestTracking.get();

            // Example: If altitude has significantly decreased and is below threshold,
            // we might assume the flight is landing
            if (trackingData.getAltitude() != null && prevTracking.getAltitude() != null &&
                    prevTracking.getAltitude() - trackingData.getAltitude() > 5000 &&
                    trackingData.getAltitude() < 1000) {

                // Update flight status to landing or landed
                flight.setStatus("Landing");
                updated = true;
            }

            // More conditions can be added based on business logic
        }

        // If callsign is provided in tracking data but flight has no callsign, update
        // it
        if (trackingData.getCallsign() != null && !trackingData.getCallsign().isEmpty() &&
                (flight.getCallsign() == null || flight.getCallsign().isEmpty() ||
                        flight.getCallsign().startsWith("UNK-"))) {

            flight.setCallsign(trackingData.getCallsign());
            updated = true;
        }

        // If any updates were made, save the flight
        if (updated) {
            flight = flightRepository.save(flight);
        }

        return flight;
    }

    /**
     * Create a new flight for an aircraft based on tracking data.
     * 
     * @param aircraft     the aircraft
     * @param trackingData the tracking data
     * @param userId       the user ID for audit
     * @return the newly created Flight
     */
    private Flight createNewFlight(Aircraft aircraft, FlightTrackingRequestDTO trackingData, Long userId) {
        // Generate a callsign if not provided in tracking data
        String callsign = trackingData.getCallsign();
        if (callsign == null || callsign.isEmpty()) {
            // Generate a default callsign based on aircraft registration or other data
            String operatorCode = aircraft.getOperatorCode() != null ? aircraft.getOperatorCode() : "UNK";
            callsign = operatorCode + "-" + System.currentTimeMillis() % 10000; // Simple unique ID
        }

        // Create a flight request
        FlightRequest flightRequest = FlightRequest.builder()
                .aircraftId(aircraft.getId())
                .callsign(callsign)
                .departureTime(
                        trackingData.getUpdateTime() != null ? trackingData.getUpdateTime() : LocalDateTime.now())
                .status("In Air")
                .originAirport("Unknown") // These can be updated later with more precise information
                .destinationAirport("Unknown")
                .build();

        // Save the new flight
        return flightService.save(flightRequest, userId);
    }
}
