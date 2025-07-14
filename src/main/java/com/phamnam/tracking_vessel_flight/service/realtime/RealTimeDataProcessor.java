package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.*;
import com.phamnam.tracking_vessel_flight.repository.*;
import com.phamnam.tracking_vessel_flight.service.kafka.TrackingKafkaProducer;
import com.phamnam.tracking_vessel_flight.service.realtime.externalApi.ExternalApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeDataProcessor {

    private final ExternalApiService externalApiService;
    private final TrackingKafkaProducer kafkaProducer;
    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;
    private final ShipRepository shipRepository;
    private final FlightTrackingRepository flightTrackingRepository;
    private final ShipTrackingRepository shipTrackingRepository;
    private final VoyageRepository voyageRepository;

    @Value("${tracking.data.processing.batch-size:100}")
    private int batchSize;

    @Value("${tracking.data.processing.enable-persistence:true}")
    private boolean enablePersistence;

    @Value("${tracking.data.processing.enable-kafka:true}")
    private boolean enableKafka;

    // In-memory cache for tracking last known positions
    private final Map<String, LocalDateTime> lastAircraftUpdate = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastVesselUpdate = new ConcurrentHashMap<>();

    // ============================================================================
    // SCHEDULED DATA COLLECTION
    // ============================================================================

    // DISABLED: Now handled by MultiSourceExternalApiService with 6 sources + data
    // fusion
    // @Scheduled(fixedRate = 30000) // Every 30 seconds
    // @Async
    public void collectAndProcessRealTimeData() {
        log.debug("Starting real-time data collection...");

        try {
            // Collect data from all sources asynchronously
            CompletableFuture<List<AircraftTrackingRequest>> aircraftFuture = externalApiService.fetchAircraftData();

            CompletableFuture<List<VesselTrackingRequest>> vesselFuture = externalApiService.fetchVesselData();

            // Wait for both to complete
            CompletableFuture.allOf(aircraftFuture, vesselFuture).join();

            // Process the results
            List<AircraftTrackingRequest> aircraftData = aircraftFuture.get();
            List<VesselTrackingRequest> vesselData = vesselFuture.get();

            // Process aircraft data
            if (!aircraftData.isEmpty()) {
                processAircraftData(aircraftData);
                log.info("Processed {} aircraft records", aircraftData.size());
            }

            // Process vessel data
            if (!vesselData.isEmpty()) {
                processVesselData(vesselData);
                log.info("Processed {} vessel records", vesselData.size());
            }

        } catch (Exception e) {
            log.error("Error during real-time data collection", e);
        }
    }

    // ============================================================================
    // AIRCRAFT DATA PROCESSING
    // ============================================================================

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> processAircraftData(List<AircraftTrackingRequest> aircraftData) {
        try {
            // Filter out duplicate/stale data
            List<AircraftTrackingRequest> filteredData = aircraftData.stream()
                    .filter(this::isAircraftDataNew)
                    .collect(Collectors.toList());

            if (filteredData.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            // Process in batches
            for (int i = 0; i < filteredData.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, filteredData.size());
                List<AircraftTrackingRequest> batch = filteredData.subList(i, endIndex);

                processBatchAircraftData(batch);
            }

            log.debug("Successfully processed {} aircraft records", filteredData.size());
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Error processing aircraft data", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void processBatchAircraftData(List<AircraftTrackingRequest> batch) {
        for (AircraftTrackingRequest request : batch) {
            try {
                // Create or update aircraft
                Aircraft aircraft = createOrUpdateAircraft(request);

                // Create or update flight - UNCOMMENTED AND IMPLEMENTED
                Flight flight = createOrUpdateFlight(request, aircraft);

                // Create tracking record
                FlightTracking tracking = createFlightTracking(request, aircraft, flight);

                // Save tracking record to database
                if (enablePersistence) {
                    try {
                        flightTrackingRepository.save(tracking);
                        log.debug("✅ SAVED flight tracking for flight ID: {} and hexident: {}",
                                flight.getId(), request.getHexident());
                    } catch (Exception e) {
                        log.warn("Failed to save flight tracking for flight ID: {}: {}",
                                flight.getId(), e.getMessage());
                    }
                }

                // Send to Kafka for real-time processing
                if (enableKafka) {
                    kafkaProducer.publishRawAircraftData(request.getHexident(), request);
                    kafkaProducer.publishProcessedAircraftData(tracking.getHexident(), tracking);
                }

                // Update cache
                lastAircraftUpdate.put(request.getHexident(), request.getTimestamp());

            } catch (Exception e) {
                log.warn("Failed to process aircraft record for {}: {}",
                        request.getHexident(), e.getMessage());
            }
        }
    }

    private Aircraft createOrUpdateAircraft(AircraftTrackingRequest request) {
        // Use synchronized block to prevent race conditions for the same hexident
        synchronized (("aircraft_" + request.getHexident()).intern()) {
            try {
                // Try to find existing aircraft first
                Optional<Aircraft> existingAircraft = aircraftRepository.findByHexident(request.getHexident());

                Aircraft aircraft;
                if (existingAircraft.isPresent()) {
                    aircraft = existingAircraft.get();
                    log.debug("Found existing aircraft with ID: {} for hexident: {}", aircraft.getId(),
                            request.getHexident());
                } else {
                    // Create new aircraft
                    aircraft = Aircraft.builder()
                            .hexident(request.getHexident())
                            .build();
                    log.debug("Creating new aircraft for hexident: {}", request.getHexident());
                }

                // Update aircraft information if available
                if (request.getRegistration() != null) {
                    aircraft.setRegister(request.getRegistration());
                }

                if (request.getAircraftType() != null) {
                    aircraft.setType(request.getAircraftType());
                }

                aircraft.setLastSeen(request.getTimestamp());

                if (enablePersistence) {
                    try {
                        Aircraft savedAircraft = aircraftRepository.save(aircraft);

                        // Verify the aircraft was saved with a valid ID
                        if (savedAircraft.getId() == null) {
                            log.error("Aircraft saved but ID is null for hexident: {}", request.getHexident());
                            // Try to fetch it from database again
                            return aircraftRepository.findByHexident(request.getHexident())
                                    .orElseThrow(() -> new RuntimeException(
                                            "Aircraft not found after save for hexident: " + request.getHexident()));
                        }

                        log.debug("✅ SAVED aircraft with ID: {} for hexident: {}", savedAircraft.getId(),
                                request.getHexident());
                        return savedAircraft;

                    } catch (Exception e) {
                        log.warn("Failed to save aircraft for hexident: {}, attempting to fetch existing: {}",
                                request.getHexident(), e.getMessage());

                        // If save failed due to constraint violation, try to fetch the existing
                        // aircraft
                        return aircraftRepository.findByHexident(request.getHexident())
                                .orElseThrow(() -> new RuntimeException(
                                        "Cannot create or find aircraft for hexident: " + request.getHexident(), e));
                    }
                }
                return aircraft;

            } catch (Exception e) {
                log.error("Critical error creating/updating aircraft for hexident: {}: {}", request.getHexident(),
                        e.getMessage(), e);
                throw new RuntimeException("Failed to create or update aircraft for hexident: " + request.getHexident(),
                        e);
            }
        }
    }

    private FlightTracking createFlightTracking(AircraftTrackingRequest request, Aircraft aircraft, Flight flight) {
        return FlightTracking.builder()
                .flight(flight)
                .hexident(request.getHexident())
                .callsign(request.getCallsign())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .altitude(request.getAltitude() != null ? request.getAltitude().floatValue() : null)
                .speed(request.getGroundSpeed() != null ? request.getGroundSpeed().floatValue() : null)
                .track(request.getTrack() != null ? request.getTrack().floatValue() : null)
                .verticalSpeed(request.getVerticalRate() != null ? request.getVerticalRate().floatValue() : null)
                .squawk(request.getSquawk() != null ? Integer.parseInt(request.getSquawk()) : null)
                .onGround(request.getOnGround())
                .emergency(request.getEmergency())
                .timestamp(request.getTimestamp())
                .updateTime(LocalDateTime.now())
                .dataSource("External API")
                .build();
    }

    private Flight createOrUpdateFlight(AircraftTrackingRequest request, Aircraft aircraft) {
        synchronized (("flight_" + request.getHexident()).intern()) {
            try {
                String callsign = request.getCallsign();

                // Try to find existing active flight for this aircraft with same callsign
                Optional<Flight> existingFlight = Optional.empty();
                if (callsign != null && !callsign.trim().isEmpty()) {
                    try {
                        existingFlight = flightRepository.findFirstByAircraftAndCallsignAndStatusOrderByCreatedAtDesc(
                                aircraft, callsign, Flight.FlightStatus.IN_AIR);
                    } catch (Exception e) {
                        log.warn("Error finding existing flight for callsign: {}, will create new one: {}",
                                callsign, e.getMessage());
                        existingFlight = Optional.empty();
                    }
                }

                Flight flight;
                if (existingFlight.isPresent()) {
                    flight = existingFlight.get();
                    log.debug("Found existing flight with ID: {} for callsign: {}", flight.getId(), callsign);
                } else {
                    // Create new flight
                    String flightCallsign = callsign != null && !callsign.trim().isEmpty()
                            ? callsign
                            : "FLIGHT-" + aircraft.getHexident();

                    flight = Flight.builder()
                            .aircraft(aircraft)
                            .callsign(flightCallsign)
                            .registration(aircraft.getRegister())
                            .aircraftType(aircraft.getType())
                            .departureTime(request.getTimestamp())
                            .status(Flight.FlightStatus.IN_AIR)
                            .originAirport("Unknown")
                            .destinationAirport("Unknown")
                            .build();

                    log.debug("Creating new flight for callsign: {} and aircraft: {}", flightCallsign,
                            aircraft.getHexident());
                }

                // Update flight status and timestamp
                flight.setUpdatedAt(LocalDateTime.now());

                if (enablePersistence) {
                    try {
                        Flight savedFlight = flightRepository.save(flight);

                        if (savedFlight.getId() == null) {
                            log.error("Flight saved but ID is null for callsign: {}", callsign);
                            return flightRepository.findFirstByAircraftAndCallsignAndStatusOrderByCreatedAtDesc(
                                    aircraft, callsign, Flight.FlightStatus.IN_AIR)
                                    .orElseThrow(() -> new RuntimeException(
                                            "Flight not found after save for callsign: " + callsign));
                        }

                        log.debug("✅ SAVED flight with ID: {} for callsign: {}", savedFlight.getId(), callsign);
                        return savedFlight;

                    } catch (Exception e) {
                        log.warn("Failed to save flight for callsign: {}, attempting to fetch existing: {}",
                                callsign, e.getMessage());

                        // If save failed, try to fetch existing flight
                        return flightRepository.findFirstByAircraftAndCallsignAndStatusOrderByCreatedAtDesc(
                                aircraft, callsign, Flight.FlightStatus.IN_AIR)
                                .orElseThrow(() -> new RuntimeException(
                                        "Cannot create or find flight for callsign: " + callsign, e));
                    }
                }
                return flight;

            } catch (Exception e) {
                log.error("Critical error creating/updating flight for callsign: {}: {}",
                        request.getCallsign(), e.getMessage(), e);
                throw new RuntimeException("Failed to create or update flight for callsign: " + request.getCallsign(),
                        e);
            }
        }
    }

    // ============================================================================
    // VESSEL DATA PROCESSING
    // ============================================================================

    public CompletableFuture<Void> processVesselData(List<VesselTrackingRequest> vesselData) {
        try {
            // Filter out duplicate/stale data
            List<VesselTrackingRequest> filteredData = vesselData.stream()
                    .filter(this::isVesselDataNew)
                    .collect(Collectors.toList());

            if (filteredData.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            // Process each vessel record in its own transaction to prevent rollback issues
            int successCount = 0;
            for (VesselTrackingRequest request : filteredData) {
                try {
                    processVesselRecordWithTransaction(request);
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to process vessel record for MMSI {}: {}", request.getMmsi(), e.getMessage());
                }
            }

            log.debug("Successfully processed {}/{} vessel records", successCount, filteredData.size());
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Error processing vessel data", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processVesselRecordWithTransaction(VesselTrackingRequest request) {
        // Create or update ship
        Ship ship = createOrUpdateShip(request);
        log.debug("Processing vessel ship {}", ship);

        // Validate that ship has a valid ID
        if (ship == null || ship.getId() == null) {
            throw new IllegalStateException("Ship is null or has null ID for MMSI: " + request.getMmsi());
        }

        // Create tracking record
        ShipTracking tracking = createShipTracking(request, ship);
        log.debug("Processing vessel tracking {}", tracking);

        // Save tracking record to database
        if (enablePersistence) {
            ShipTracking savedTracking = shipTrackingRepository.save(tracking);
            log.debug("✅ SAVED ship_tracking with ID: {} for MMSI: {}", savedTracking.getId(), request.getMmsi());
        }

        // Send to Kafka for real-time processing
        if (enableKafka) {
            kafkaProducer.publishRawVesselData(request.getMmsi(), request);
            kafkaProducer.publishProcessedVesselData(tracking.getMmsi(), tracking);
        }

        // Update cache
        lastVesselUpdate.put(request.getMmsi(), request.getTimestamp());
    }

    private void processBatchVesselData(List<VesselTrackingRequest> batch) {
        for (VesselTrackingRequest request : batch) {
            try {
                // Create or update ship
                Ship ship = createOrUpdateShip(request);
                log.debug("Processing vessel ship {}", ship);

                // Validate that ship has a valid ID
                if (ship == null) {
                    log.error("Ship is null for MMSI: {}, skipping vessel record", request.getMmsi());
                    continue;
                }

                if (ship.getId() == null) {
                    log.error("Ship ID is null for MMSI: {}, skipping vessel record", request.getMmsi());
                    continue;
                }

                // Create tracking record
                ShipTracking tracking = createShipTracking(request, ship);
                log.debug("Processing vessel tracking {}", tracking);

                // Save tracking record to database
                if (enablePersistence) {
                    ShipTracking savedTracking = shipTrackingRepository.save(tracking);
                    shipTrackingRepository.flush(); // Force immediate database write
                    log.debug("✅ FORCE SAVED ship_tracking with ID: {} for MMSI: {}", savedTracking.getId(),
                            request.getMmsi());
                }

                // Send to Kafka for real-time processing
                if (enableKafka) {
                    kafkaProducer.publishRawVesselData(request.getMmsi(), request);
                    kafkaProducer.publishProcessedVesselData(tracking.getMmsi(), tracking);
                }

                // Update cache
                lastVesselUpdate.put(request.getMmsi(), request.getTimestamp());

            } catch (Exception e) {
                log.warn("Failed to process vessel record for {}: {}",
                        request.getMmsi(), e.getMessage());
            }
        }
    }

    private Ship createOrUpdateShip(VesselTrackingRequest request) {
        // Use synchronized block to prevent race conditions for the same MMSI or IMO
        String lockKey = request.getImo() != null ? ("ship_imo_" + request.getImo()).intern()
                : ("ship_mmsi_" + request.getMmsi()).intern();

        synchronized (lockKey) {
            try {
                // Try to find existing ship first by MMSI
                Optional<Ship> existingShip = shipRepository.findByMmsi(request.getMmsi());

                // If not found by MMSI and IMO is provided, check by IMO
                if (!existingShip.isPresent() && request.getImo() != null) {
                    existingShip = shipRepository.findByImo(request.getImo());
                    if (existingShip.isPresent()) {
                        log.debug("Found existing ship by IMO: {} for MMSI: {}, updating MMSI",
                                request.getImo(), request.getMmsi());
                        // Update the MMSI of the existing ship
                        Ship ship = existingShip.get();
                        ship.setMmsi(request.getMmsi());
                    }
                }

                Ship ship;
                if (existingShip.isPresent()) {
                    ship = existingShip.get();
                    log.debug("Found existing ship with ID: {} for MMSI: {}", ship.getId(), request.getMmsi());
                } else {
                    // Create new ship
                    ship = Ship.builder()
                            .mmsi(request.getMmsi())
                            .build();
                    log.debug("Creating new ship for MMSI: {}", request.getMmsi());
                }

                // Update ship information if available
                if (request.getVesselName() != null) {
                    ship.setName(request.getVesselName());
                }
                if (request.getVesselType() != null) {
                    ship.setShipType(request.getVesselType());
                }
                if (request.getImo() != null) {
                    // Check if IMO is already set and different, or if another ship has this IMO
                    if (ship.getImo() == null || !ship.getImo().equals(request.getImo())) {
                        // Only set IMO if no other ship has it (except current ship)
                        Optional<Ship> shipWithSameImo = shipRepository.findByImo(request.getImo());
                        if (!shipWithSameImo.isPresent() || shipWithSameImo.get().getId().equals(ship.getId())) {
                            ship.setImo(request.getImo());
                        } else {
                            log.warn(
                                    "IMO {} already exists for different ship (ID: {}), not updating IMO for ship ID: {}",
                                    request.getImo(), shipWithSameImo.get().getId(), ship.getId());
                        }
                    }
                }
                if (request.getCallsign() != null) {
                    ship.setCallsign(request.getCallsign());
                }
                ship.setLastSeen(request.getTimestamp());

                if (enablePersistence) {
                    try {
                        Ship savedShip = shipRepository.save(ship);

                        // Verify the ship was saved with a valid ID
                        if (savedShip.getId() == null) {
                            log.error("Ship saved but ID is null for MMSI: {}", request.getMmsi());
                            // Try to fetch it from database again
                            return shipRepository.findByMmsi(request.getMmsi())
                                    .orElseThrow(() -> new RuntimeException(
                                            "Ship not found after save for MMSI: " + request.getMmsi()));
                        }

                        log.debug("✅ SAVED ship with ID: {} for MMSI: {}", savedShip.getId(), request.getMmsi());
                        return savedShip;

                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        log.warn("Constraint violation when saving ship for MMSI: {}, attempting to find existing: {}",
                                request.getMmsi(), e.getMessage());

                        // Handle specific constraint violations
                        if (e.getMessage().contains("uk_5qjjlnahsbq9ghqty2hunc06") || e.getMessage().contains("imo")) {
                            // IMO constraint violation - try to find by IMO
                            if (request.getImo() != null) {
                                Optional<Ship> existingByImo = shipRepository.findByImo(request.getImo());
                                if (existingByImo.isPresent()) {
                                    Ship shipByImo = existingByImo.get();
                                    log.info(
                                            "Found existing ship by IMO: {} with different MMSI: {}, updating to new MMSI: {}",
                                            request.getImo(), shipByImo.getMmsi(), request.getMmsi());
                                    shipByImo.setMmsi(request.getMmsi());
                                    // Update other fields
                                    if (request.getVesselName() != null) {
                                        shipByImo.setName(request.getVesselName());
                                    }
                                    if (request.getVesselType() != null) {
                                        shipByImo.setShipType(request.getVesselType());
                                    }
                                    if (request.getCallsign() != null) {
                                        shipByImo.setCallsign(request.getCallsign());
                                    }
                                    shipByImo.setLastSeen(request.getTimestamp());
                                    return shipRepository.save(shipByImo);
                                }
                            }
                        }

                        // If still can't resolve, try to fetch by MMSI
                        return shipRepository.findByMmsi(request.getMmsi())
                                .orElseThrow(() -> new RuntimeException(
                                        "Cannot create or find ship for MMSI: " + request.getMmsi(), e));

                    } catch (Exception e) {
                        log.warn("Failed to save ship for MMSI: {}, attempting to fetch existing: {}",
                                request.getMmsi(), e.getMessage());

                        // If save failed, try to fetch the existing ship
                        return shipRepository.findByMmsi(request.getMmsi())
                                .orElseThrow(() -> new RuntimeException(
                                        "Cannot create or find ship for MMSI: " + request.getMmsi(), e));
                    }
                }
                return ship;

            } catch (Exception e) {
                log.error("Critical error creating/updating ship for MMSI: {}: {}", request.getMmsi(), e.getMessage(),
                        e);
                throw new RuntimeException("Failed to create or update ship for MMSI: " + request.getMmsi(), e);
            }
        }
    }

    private ShipTracking createShipTracking(VesselTrackingRequest request, Ship ship) {
        // Need to find or create a voyage for this ship
        Voyage voyage = getOrCreateVoyageForShip(ship, request);

        return ShipTracking.builder()
                .voyage(voyage)
                .mmsi(request.getMmsi())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .speed(request.getSpeed())
                .course(request.getCourse() != null ? request.getCourse().doubleValue() : null)
                .heading(request.getHeading() != null ? request.getHeading().doubleValue() : null)
                .navigationStatus(request.getNavigationStatus())
                .timestamp(request.getTimestamp())
                .updateTime(LocalDateTime.now())
                .dataSource("External API")
                .build();
    }

    /**
     * Get or create a voyage for ship tracking
     */
    private Voyage getOrCreateVoyageForShip(Ship ship, VesselTrackingRequest request) {
        // Validate ship parameter
        if (ship == null || ship.getId() == null) {
            throw new IllegalArgumentException("Ship must not be null and must have a valid ID");
        }

        // Use synchronized block to prevent race conditions for the same ship
        synchronized (("voyage_" + ship.getId()).intern()) {
            try {
                // Try to find an active voyage for this ship
                Optional<Voyage> activeVoyage = voyageRepository.findLatestVoyageByShipId(ship.getId());
                if (activeVoyage.isPresent()) {
                    Voyage voyage = activeVoyage.get();
                    // Check if voyage is still active (no arrival time or recent)
                    if (voyage.getArrivalTime() == null ||
                            voyage.getLastSeen() != null &&
                                    voyage.getLastSeen().isAfter(LocalDateTime.now().minusHours(2))) {

                        // Update voyage info and return existing
                        voyage.setLastSeen(request.getTimestamp());
                        if (enablePersistence) {
                            try {
                                Voyage savedVoyage = voyageRepository.save(voyage);
                                log.debug("✅ SAVED existing voyage with ID: {}", savedVoyage.getId());
                                return savedVoyage;
                            } catch (Exception e) {
                                log.warn("Failed to update existing voyage for ship ID: {}: {}", ship.getId(),
                                        e.getMessage());
                                return voyage; // Return unsaved voyage if update fails
                            }
                        }
                        return voyage;
                    }
                }

                // Create new voyage if no active voyage found
                Voyage newVoyage = Voyage.builder()
                        .ship(ship)
                        .voyageNumber("AUTO-" + System.currentTimeMillis())
                        .departureTime(request.getTimestamp())
                        .departurePort("Unknown")
                        .arrivalPort(request.getDestination() != null ? request.getDestination() : "Unknown")
                        .status(Voyage.VoyageStatus.UNDERWAY)
                        .voyagePhase(Voyage.VoyagePhase.OPEN_SEA)
                        .firstSeen(request.getTimestamp())
                        .lastSeen(request.getTimestamp())
                        .currentSpeed(request.getSpeed())
                        .currentHeading(request.getHeading() != null ? request.getHeading().doubleValue() : null)
                        .navigationStatus(request.getNavigationStatus())
                        .destinationEta(request.getTimestamp().plusDays(1)) // Estimate 1 day
                        .build();

                if (enablePersistence) {
                    try {
                        Voyage savedVoyage = voyageRepository.save(newVoyage);

                        // Verify the voyage was saved with a valid ID
                        if (savedVoyage.getId() == null) {
                            log.error("Voyage saved but ID is null for ship ID: {}", ship.getId());
                            return newVoyage; // Return unsaved voyage
                        }

                        log.debug("✅ SAVED new voyage with ID: {}", savedVoyage.getId());
                        return savedVoyage;

                    } catch (Exception e) {
                        log.warn("Failed to save new voyage for ship ID: {}: {}", ship.getId(), e.getMessage());
                        return newVoyage; // Return unsaved voyage if save fails
                    }
                }
                return newVoyage;

            } catch (Exception e) {
                log.error("Critical error creating/updating voyage for ship ID: {}: {}", ship.getId(), e.getMessage(),
                        e);
                throw new RuntimeException("Failed to create or update voyage for ship ID: " + ship.getId(), e);
            }
        }
    }

    // ============================================================================
    // DATA FILTERING AND VALIDATION
    // ============================================================================

    private boolean isAircraftDataNew(AircraftTrackingRequest request) {
        LocalDateTime lastUpdate = lastAircraftUpdate.get(request.getHexident());
        if (lastUpdate == null) {
            return true;
        }

        // Only process if data is newer than last update
        return request.getTimestamp().isAfter(lastUpdate);
    }

    private boolean isVesselDataNew(VesselTrackingRequest request) {
        LocalDateTime lastUpdate = lastVesselUpdate.get(request.getMmsi());
        if (lastUpdate == null) {
            return true;
        }

        // Only process if data is newer than last update
        return request.getTimestamp().isAfter(lastUpdate);
    }

    // ============================================================================
    // MONITORING AND STATISTICS
    // ============================================================================

    @Cacheable(value = "statistics", key = "'processing-stats'")
    public Map<String, Object> getProcessingStatistics() {
        return Map.of(
                "totalAircraftTracked", lastAircraftUpdate.size(),
                "totalVesselsTracked", lastVesselUpdate.size(),
                "lastUpdateTime", LocalDateTime.now().toString(),
                "batchSize", batchSize,
                "persistenceEnabled", enablePersistence,
                "kafkaEnabled", enableKafka);
    }

    public void clearStaleCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        lastAircraftUpdate.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));

        lastVesselUpdate.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));

        log.debug("Cleared stale cache entries");
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    public void scheduledCacheCleanup() {
        clearStaleCache();
    }

    // ============================================================================
    // MANUAL PROCESSING METHODS
    // ============================================================================

    public CompletableFuture<Void> processAircraftDataManually(List<AircraftTrackingRequest> data) {
        return processAircraftData(data);
    }

    public CompletableFuture<Void> processVesselDataManually(List<VesselTrackingRequest> data) {
        return processVesselData(data);
    }

    public void forceDataCollection() {
        log.info("Forcing immediate data collection...");
        collectAndProcessRealTimeData();
    }
}