package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VoyageRequest;
import com.phamnam.tracking_vessel_flight.dto.response.ShipTrackingResponse;
import com.phamnam.tracking_vessel_flight.dto.response.VoyageResponse;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.models.Voyage;
import com.phamnam.tracking_vessel_flight.models.Ship;
import com.phamnam.tracking_vessel_flight.repository.ShipTrackingRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.repository.VoyageRepository;
import com.phamnam.tracking_vessel_flight.repository.ShipRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.IShipTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ShipTrackingService implements IShipTrackingService {
    @Autowired
    private ShipTrackingRepository shipTrackingRepository;

    @Autowired
    private VoyageRepository voyageRepository;

    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoyageService voyageService;

    // Thời gian tối đa giữa 2 tracking để coi là cùng 1 chuyến (ví dụ: 2 tiếng)
    private static final Duration MAX_INACTIVITY = Duration.ofHours(2);

    public List<ShipTrackingResponse> getAll() {
        List<ShipTracking> trackings = shipTrackingRepository.findAll();
        return trackings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<ShipTrackingResponse> getAllPaginated(Pageable pageable) {
        Page<ShipTracking> trackings = shipTrackingRepository.findAll(pageable);
        return trackings.map(this::convertToResponse);
    }

    public ShipTrackingResponse getShipTrackingById(Long id) {
        ShipTracking tracking = shipTrackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipTracking", "id", id));
        return convertToResponse(tracking);
    }

    @Transactional
    public ShipTrackingResponse save(ShipTrackingRequest shipTrackingRequest, Long userId) {
        Voyage voyage = voyageRepository.findById(shipTrackingRequest.getVoyageId())
                .orElseThrow(() -> new ResourceNotFoundException("Voyage", "id", shipTrackingRequest.getVoyageId()));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        ShipTracking shipTracking = ShipTracking.builder()
                .timestamp(shipTrackingRequest.getTimestamp())
                .mmsi(shipTrackingRequest.getMmsi()) // Add missing MMSI field
                .latitude(shipTrackingRequest.getLatitude())
                .longitude(shipTrackingRequest.getLongitude())
                .speed(shipTrackingRequest.getSpeed())
                .course(shipTrackingRequest.getCourse())
                .draught(shipTrackingRequest.getDraught())
                .voyage(voyage)
                .build();

        shipTracking.setUpdatedBy(user);
        ShipTracking savedTracking = shipTrackingRepository.save(shipTracking);

        return convertToResponse(savedTracking);
    }

    @Transactional
    public void deleteShipTracking(Long id) {
        ShipTracking shipTracking = shipTrackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipTracking", "id", id));
        shipTrackingRepository.delete(shipTracking);
    }

    @Transactional
    public ShipTrackingResponse updateShipTracking(Long id, ShipTrackingRequest shipTrackingRequest, Long userId) {
        ShipTracking shipTracking = shipTrackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipTracking", "id", id));
        Voyage voyage = voyageRepository.findById(shipTrackingRequest.getVoyageId())
                .orElseThrow(() -> new ResourceNotFoundException("Voyage", "id", shipTrackingRequest.getVoyageId()));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        shipTracking.setTimestamp(shipTrackingRequest.getTimestamp());
        shipTracking.setLatitude(shipTrackingRequest.getLatitude());
        shipTracking.setLongitude(shipTrackingRequest.getLongitude());
        shipTracking.setSpeed(shipTrackingRequest.getSpeed());
        shipTracking.setCourse(shipTrackingRequest.getCourse());
        shipTracking.setDraught(shipTrackingRequest.getDraught());
        shipTracking.setVoyage(voyage);
        shipTracking.setUpdatedBy(user);

        ShipTracking updatedTracking = shipTrackingRepository.save(shipTracking);
        return convertToResponse(updatedTracking);
    }

    public List<ShipTrackingResponse> getTrackingsByVoyageId(Long voyageId) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new ResourceNotFoundException("Voyage", "id", voyageId));

        // Use the repository method instead of stream filtering
        List<ShipTracking> trackings = shipTrackingRepository.findByVoyageIdOrderByTimestampDesc(voyageId);
        return trackings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Process a new ship tracking data point and assign it to the appropriate
     * voyage.
     * If no active voyage exists for the ship, a new voyage will be created.
     * A new voyage will also be created if:
     * - The new tracking data is more than 2 hours after the previous tracking OR
     * - The speed of the new tracking data is 0
     *
     * @param shipId       The ID of the ship being tracked
     * @param trackingData The new tracking data (lat, long, speed, etc.)
     * @param userId       The user ID for audit purposes (optional)
     * @return The saved ShipTracking entity (converted to DTO)
     */
    @Transactional
    public ShipTrackingResponse processNewTrackingData(Long shipId, ShipTrackingRequest trackingData, Long userId) {
        // Find the ship
        Ship ship = shipRepository.findById(shipId)
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", shipId));

        // Get user for audit if needed
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        // Find the latest tracking data for this ship using the repository method
        Optional<ShipTracking> latestTracking = shipTrackingRepository.findLastTrackingByShipId(shipId);

        // Determine if we need a new voyage
        boolean needNewVoyage = determineIfNewVoyageNeeded(latestTracking, trackingData);

        Voyage voyage;

        if (needNewVoyage) {
            // Create a new voyage
            voyage = createNewVoyage(ship, trackingData, userId);
        } else {
            // Get the latest voyage using the repository method
            voyage = voyageRepository.findLatestVoyageByShipId(shipId)
                    .orElseGet(() -> createNewVoyage(ship, trackingData, userId));
        }

        // Now create and save the tracking data
        ShipTracking shipTracking = ShipTracking.builder()
                .timestamp(trackingData.getTimestamp())
                .mmsi(trackingData.getMmsi()) // Add missing MMSI field
                .latitude(trackingData.getLatitude())
                .longitude(trackingData.getLongitude())
                .speed(trackingData.getSpeed())
                .course(trackingData.getCourse())
                .draught(trackingData.getDraught())
                .voyage(voyage)
                .build();

        shipTracking.setUpdatedBy(user);
        ShipTracking savedTracking = shipTrackingRepository.save(shipTracking);

        return convertToResponse(savedTracking);
    }

    /**
     * Determine if a new voyage is needed based on the latest tracking and new
     * tracking data.
     *
     * @param latestTracking  The latest tracking data (if available)
     * @param newTrackingData The new tracking data being processed
     * @return true if a new voyage should be created, false otherwise
     */
    private boolean determineIfNewVoyageNeeded(Optional<ShipTracking> latestTracking,
            ShipTrackingRequest newTrackingData) {
        // If no previous tracking exists, we need a new voyage
        if (latestTracking.isEmpty()) {
            return true;
        }

        // If the speed is 0, create a new voyage (ship has stopped)
        if (newTrackingData.getSpeed() != null && newTrackingData.getSpeed() == 0) {
            return true;
        }

        // If the time gap is more than the MAX_INACTIVITY, create a new voyage
        LocalDateTime lastTrackingTime = latestTracking.get().getTimestamp();
        LocalDateTime newTrackingTime = newTrackingData.getTimestamp();

        Duration timeDifference = Duration.between(lastTrackingTime, newTrackingTime);
        return timeDifference.compareTo(MAX_INACTIVITY) >= 0;

        // Otherwise, use the existing voyage
    }

    /**
     * Find an active voyage for the given ship.
     * A voyage is considered active if it has a departure time but no arrival time,
     * or if the current time is between departure and arrival times.
     */
    private Optional<Voyage> findActiveVoyageForShip(Long shipId) {
        List<Voyage> voyages = voyageRepository.findAll().stream()
                .filter(v -> v.getShip().getId().equals(shipId))
                .toList();

        LocalDateTime now = LocalDateTime.now();

        return voyages.stream()
                .filter(voyage -> {
                    // Case 1: Has departure time but no arrival time (ongoing voyage)
                    if (voyage.getDepartureTime() != null && voyage.getArrivalTime() == null) {
                        return true;
                    }

                    // Case 2: Current time is between departure and arrival
                    if (voyage.getDepartureTime() != null && voyage.getArrivalTime() != null) {
                        return now.isAfter(voyage.getDepartureTime()) &&
                                now.isBefore(voyage.getArrivalTime());
                    }

                    return false;
                })
                .findFirst();
    }

    /**
     * Create a new voyage for a ship based on tracking data.
     */
    private Voyage createNewVoyage(Ship ship, ShipTrackingRequest trackingData, Long userId) {
        // Create a minimal voyage with the information we have
        VoyageRequest voyageRequest = VoyageRequest.builder()
                .voyageNumber("AUTO-" + System.currentTimeMillis()) // Generate a unique voyage number
                .departureTime(trackingData.getTimestamp()) // Use tracking timestamp as departure
                .departurePort("Unknown") // We don't know the ports yet
                .arrivalPort("Unknown")
                .shipId(ship.getId())
                .build();

        // Save voyage and retrieve entity from repository
        VoyageResponse voyageResponse = voyageService.save(voyageRequest, userId);

        // Return the actual Voyage entity from the repository
        return voyageRepository.findById(voyageResponse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Voyage", "id", voyageResponse.getId()));
    }

    /**
     * Convert ShipTracking entity to ShipTrackingResponse DTO
     */
    private ShipTrackingResponse convertToResponse(ShipTracking tracking) {
        ShipTrackingResponse.ShipTrackingResponseBuilder builder = ShipTrackingResponse.builder()
                .id(tracking.getId())
                .mmsi(tracking.getMmsi())
                .latitude(tracking.getLatitude())
                .longitude(tracking.getLongitude())
                .speed(tracking.getSpeed() != null ? tracking.getSpeed().floatValue() : null)
                .course(tracking.getCourse() != null ? tracking.getCourse().floatValue() : null)
                .draught(tracking.getDraught() != null ? tracking.getDraught().floatValue() : null)
                .timestamp(tracking.getTimestamp())
                .createdAt(tracking.getCreatedAt());

        // Safely access voyage information
        if (tracking.getVoyage() != null) {
            builder.voyageId(tracking.getVoyage().getId())
                    .voyageNumber(tracking.getVoyage().getVoyageNumber());

            // Safely access ship information through voyage
            if (tracking.getVoyage().getShip() != null) {
                builder.shipName(tracking.getVoyage().getShip().getName())
                        .imo(tracking.getVoyage().getShip().getImo());
            }
        }

        // Safely access user information
        if (tracking.getUpdatedBy() != null) {
            builder.updatedByUsername(tracking.getUpdatedBy().getUsername());
        }

        return builder.build();
    }
}
