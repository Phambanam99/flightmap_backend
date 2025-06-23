package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.*;
import com.phamnam.tracking_vessel_flight.models.enums.EntityType;
import com.phamnam.tracking_vessel_flight.repository.*;
import com.phamnam.tracking_vessel_flight.service.kafka.TrackingKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ShipRepository shipRepository;
    private final FlightTrackingRepository flightTrackingRepository;
    private final ShipTrackingRepository shipTrackingRepository;

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

    @Async
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
                // Flight flight = createOrUpdateFlight(request);
                // Create tracking record
                FlightTracking tracking = createFlightTracking(request, aircraft);

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
        Aircraft aircraft = aircraftRepository.findByHexident(request.getHexident())
                .orElse(Aircraft.builder()
                        .hexident(request.getHexident())
                        .build());

        // Update aircraft information if available
        if (request.getRegistration() != null) {
            aircraft.setRegister(request.getRegistration());
        }

        if (request.getAircraftType() != null) {
            aircraft.setType(request.getAircraftType());
        }

        return aircraftRepository.save(aircraft);
    }

    private FlightTracking createFlightTracking(AircraftTrackingRequest request, Aircraft aircraft) {
        // Need to find or create a flight for this aircraft
        Flight flight = getOrCreateFlightForAircraft(aircraft, request);

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

    /**
     * Get or create a flight for aircraft tracking
     */
    private Flight getOrCreateFlightForAircraft(Aircraft aircraft, AircraftTrackingRequest request) {
        // Try to find an active flight for this aircraft
        // For simplicity, we'll create a basic flight record
        // In a real implementation, you might want more sophisticated flight management

        String callsign = request.getCallsign() != null ? request.getCallsign()
                : aircraft.getOperatorCode() + "-" + System.currentTimeMillis() % 10000;

        return Flight.builder()
                .aircraft(aircraft)
                .callsign(callsign)
                .departureTime(request.getTimestamp())
                .originAirport("Unknown")
                .destinationAirport("Unknown")
                .build();
    }

    // ============================================================================
    // VESSEL DATA PROCESSING
    // ============================================================================

    @Async
    @Transactional
    public CompletableFuture<Void> processVesselData(List<VesselTrackingRequest> vesselData) {
        try {
            // Filter out duplicate/stale data
            List<VesselTrackingRequest> filteredData = vesselData.stream()
                    .filter(this::isVesselDataNew)
                    .collect(Collectors.toList());

            if (filteredData.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }

            // Process in batches
            for (int i = 0; i < filteredData.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, filteredData.size());
                List<VesselTrackingRequest> batch = filteredData.subList(i, endIndex);

                processBatchVesselData(batch);
            }

            log.debug("Successfully processed {} vessel records", filteredData.size());
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Error processing vessel data", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void processBatchVesselData(List<VesselTrackingRequest> batch) {
        for (VesselTrackingRequest request : batch) {
            try {
                // Create or update ship
                Ship ship = createOrUpdateShip(request);

                // Create tracking record
                ShipTracking tracking = createShipTracking(request, ship);

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
        Ship ship = shipRepository.findByMmsi(request.getMmsi())
                .orElse(Ship.builder()
                        .mmsi(request.getMmsi())
                        .build());

        // Update ship information if available
        if (request.getVesselName() != null) {
            ship.setName(request.getVesselName());
        }
        if (request.getVesselType() != null) {
            ship.setShipType(request.getVesselType());
        }
        if (request.getImo() != null) {
            ship.setImo(request.getImo());
        }
        if (request.getCallsign() != null) {
            ship.setCallsign(request.getCallsign());
        }
        ship.setLastSeen(request.getTimestamp());

        if (enablePersistence) {
            return shipRepository.save(ship);
        }
        return ship;
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
        // For simplicity, we'll create a basic voyage record
        // In a real implementation, you might want more sophisticated voyage management

        return Voyage.builder()
                .ship(ship)
                .departureTime(request.getTimestamp())
                .arrivalPort(request.getDestination() != null ? request.getDestination() : "Unknown")
                .build();
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