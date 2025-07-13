package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.raw.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.raw.RawVesselData;
import com.phamnam.tracking_vessel_flight.service.kafka.TrackingKafkaProducer;
import com.phamnam.tracking_vessel_flight.service.realtime.externalApi.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simple Data Collection Service
 * 
 * This service is responsible ONLY for:
 * 1. Calling external APIs to fetch raw data
 * 2. Converting data to RawAircraftData/RawVesselData with metadata
 * 3. Publishing raw data to source-specific Kafka topics
 * 
 * It does NOT perform:
 * - Data fusion/merging
 * - Data processing/validation
 * - Business logic processing
 * 
 * This separation allows for better scalability and easier maintenance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleDataCollectionService {

    private final TrackingKafkaProducer kafkaProducer;

    // External API Services
    private final ExternalApiService externalApiService;
    private final AdsbExchangeApiService adsbExchangeApiService;
    private final MarineTrafficV2ApiService marineTrafficV2ApiService;
    private final VesselFinderApiService vesselFinderApiService;
    private final ChinaportsApiService chinaportsApiService;

    /**
     * Main scheduled method for data collection
     * Collects data from all sources in parallel and publishes to raw topics
     */
    @Scheduled(fixedDelayString = "${external.api.data-collection.poll-interval:30000}", initialDelay = 10000)
    @Async("scheduledTaskExecutor")
    public void collectAllDataSources() {
        String threadName = Thread.currentThread().getName();
        log.info("🚀 Starting simple data collection on thread: {}", threadName);

        try {
            // Collect aircraft data from all sources in parallel
            CompletableFuture<Void> aircraftCollection = CompletableFuture.allOf(
                    collectFlightRadar24Data(),
                    collectAdsbExchangeData());

            // Collect vessel data from all sources in parallel
            CompletableFuture<Void> vesselCollection = CompletableFuture.allOf(
                    collectMarineTrafficData(),
                    collectVesselFinderData(),
                    collectChinaportsData(),
                    collectMarineTrafficV2Data());

            // Wait for all collections to complete with timeout
            CompletableFuture.allOf(aircraftCollection, vesselCollection)
                    .get(120, TimeUnit.SECONDS);

            log.info("✅ Simple data collection completed successfully on thread: {}", threadName);

        } catch (Exception e) {
            log.error("❌ Error during simple data collection on thread {}: {}",
                    threadName, e.getMessage(), e);
        }
    }

    /**
     * Collect data from FlightRadar24 API
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> collectFlightRadar24Data() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("📡 Collecting data from FlightRadar24...");
                long startTime = System.currentTimeMillis();

                List<AircraftTrackingRequest> aircraftData = externalApiService.fetchAircraftData().join();
                long responseTime = System.currentTimeMillis() - startTime;

                if (aircraftData != null && !aircraftData.isEmpty()) {
                    log.info("📊 Collected {} aircraft from FlightRadar24", aircraftData.size());

                    for (AircraftTrackingRequest aircraft : aircraftData) {
                        RawAircraftData rawData = convertToRawAircraftData(
                                "flightradar24",
                                "/api/aircraft/flightradar24",
                                aircraft,
                                responseTime);

                        // Publish to FlightRadar24-specific topic
                        kafkaProducer.publishRawFlightRadar24Data(
                                aircraft.getHexident(),
                                rawData);
                    }
                } else {
                    log.debug("⚠️ No data received from FlightRadar24");
                }

            } catch (Exception e) {
                log.error("❌ Failed to collect data from FlightRadar24: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Collect data from ADS-B Exchange API
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> collectAdsbExchangeData() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("📡 Collecting data from ADS-B Exchange...");
                long startTime = System.currentTimeMillis();

                List<AircraftTrackingRequest> aircraftData = adsbExchangeApiService.fetchAircraftData().join();
                long responseTime = System.currentTimeMillis() - startTime;

                if (aircraftData != null && !aircraftData.isEmpty()) {
                    log.info("📊 Collected {} aircraft from ADS-B Exchange", aircraftData.size());

                    for (AircraftTrackingRequest aircraft : aircraftData) {
                        RawAircraftData rawData = convertToRawAircraftData(
                                "adsbexchange",
                                "/api/aircraft/adsbexchange",
                                aircraft,
                                responseTime);

                        // Publish to ADS-B Exchange-specific topic
                        kafkaProducer.publishRawAdsbExchangeData(
                                aircraft.getHexident(),
                                rawData);
                    }
                } else {
                    log.debug("⚠️ No data received from ADS-B Exchange");
                }

            } catch (Exception e) {
                log.error("❌ Failed to collect data from ADS-B Exchange: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Collect data from MarineTraffic API
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> collectMarineTrafficData() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("📡 Collecting data from MarineTraffic...");
                long startTime = System.currentTimeMillis();

                List<VesselTrackingRequest> vesselData = externalApiService.fetchVesselData().join();
                long responseTime = System.currentTimeMillis() - startTime;

                if (vesselData != null && !vesselData.isEmpty()) {
                    log.info("🚢 Collected {} vessels from MarineTraffic", vesselData.size());

                    for (VesselTrackingRequest vessel : vesselData) {
                        RawVesselData rawData = convertToRawVesselData(
                                "marinetraffic",
                                "/api/vessels/marinetraffic",
                                vessel,
                                responseTime);

                        // Publish to MarineTraffic-specific topic
                        kafkaProducer.publishRawMarineTrafficData(
                                vessel.getMmsi(),
                                rawData);
                    }
                } else {
                    log.debug("⚠️ No data received from MarineTraffic");
                }

            } catch (Exception e) {
                log.error("❌ Failed to collect data from MarineTraffic: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Collect data from VesselFinder API
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> collectVesselFinderData() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("📡 Collecting data from VesselFinder...");
                long startTime = System.currentTimeMillis();

                List<VesselTrackingRequest> vesselData = vesselFinderApiService.fetchVesselData().join();
                long responseTime = System.currentTimeMillis() - startTime;

                if (vesselData != null && !vesselData.isEmpty()) {
                    log.info("🚢 Collected {} vessels from VesselFinder", vesselData.size());

                    for (VesselTrackingRequest vessel : vesselData) {
                        RawVesselData rawData = convertToRawVesselData(
                                "vesselfinder",
                                "/api/vessels/vesselfinder",
                                vessel,
                                responseTime);

                        // Publish to VesselFinder-specific topic
                        kafkaProducer.publishRawVesselFinderData(
                                vessel.getMmsi(),
                                rawData);
                    }
                } else {
                    log.debug("⚠️ No data received from VesselFinder");
                }

            } catch (Exception e) {
                log.error("❌ Failed to collect data from VesselFinder: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Collect data from Chinaports API
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> collectChinaportsData() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("📡 Collecting data from Chinaports...");
                long startTime = System.currentTimeMillis();

                List<VesselTrackingRequest> vesselData = chinaportsApiService.fetchVesselData().join();
                long responseTime = System.currentTimeMillis() - startTime;

                if (vesselData != null && !vesselData.isEmpty()) {
                    log.info("🚢 Collected {} vessels from Chinaports", vesselData.size());

                    for (VesselTrackingRequest vessel : vesselData) {
                        RawVesselData rawData = convertToRawVesselData(
                                "chinaports",
                                "/api/vessels/chinaports",
                                vessel,
                                responseTime);

                        // Publish to Chinaports-specific topic
                        kafkaProducer.publishRawChinaportsData(
                                vessel.getMmsi(),
                                rawData);
                    }
                } else {
                    log.debug("⚠️ No data received from Chinaports");
                }

            } catch (Exception e) {
                log.error("❌ Failed to collect data from Chinaports: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Collect data from MarineTraffic V2 API
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> collectMarineTrafficV2Data() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("📡 Collecting data from MarineTraffic V2...");
                long startTime = System.currentTimeMillis();

                List<VesselTrackingRequest> vesselData = marineTrafficV2ApiService.fetchVesselData().join();
                long responseTime = System.currentTimeMillis() - startTime;

                if (vesselData != null && !vesselData.isEmpty()) {
                    log.info("🚢 Collected {} vessels from MarineTraffic V2", vesselData.size());

                    for (VesselTrackingRequest vessel : vesselData) {
                        RawVesselData rawData = convertToRawVesselData(
                                "marinetrafficv2",
                                "/api/vessels/marinetrafficv2",
                                vessel,
                                responseTime);

                        // Publish to MarineTraffic V2-specific topic
                        kafkaProducer.publishRawMarineTrafficV2Data(
                                vessel.getMmsi(),
                                rawData);
                    }
                } else {
                    log.debug("⚠️ No data received from MarineTraffic V2");
                }

            } catch (Exception e) {
                log.error("❌ Failed to collect data from MarineTraffic V2: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Convert AircraftTrackingRequest to RawAircraftData with metadata
     */
    private RawAircraftData convertToRawAircraftData(String source, String apiEndpoint,
            AircraftTrackingRequest aircraft, long responseTime) {
        RawAircraftData rawData = RawAircraftData.fromSource(source, apiEndpoint, aircraft, responseTime);

        // Map aircraft fields
        rawData.setHexident(aircraft.getHexident());
        rawData.setCallsign(aircraft.getCallsign());
        rawData.setLatitude(aircraft.getLatitude());
        rawData.setLongitude(aircraft.getLongitude());
        rawData.setAltitude(aircraft.getAltitude());
        rawData.setGroundSpeed(aircraft.getGroundSpeed());
        rawData.setTrack(aircraft.getTrack());
        rawData.setVerticalRate(aircraft.getVerticalRate());
        rawData.setSquawk(aircraft.getSquawk());
        rawData.setAircraftType(aircraft.getAircraftType());
        rawData.setRegistration(aircraft.getRegistration());
        rawData.setOnGround(aircraft.getOnGround());
        rawData.setEmergency(aircraft.getEmergency());
        rawData.setTimestamp(aircraft.getTimestamp());
        rawData.setDataQuality(aircraft.getDataQuality());

        return rawData;
    }

    /**
     * Convert VesselTrackingRequest to RawVesselData with metadata
     */
    private RawVesselData convertToRawVesselData(String source, String apiEndpoint,
            VesselTrackingRequest vessel, long responseTime) {
        RawVesselData rawData = RawVesselData.fromSource(source, apiEndpoint, vessel, responseTime);

        // Map vessel fields
        rawData.setMmsi(vessel.getMmsi());
        rawData.setImo(vessel.getImo());
        rawData.setVesselName(vessel.getVesselName());
        rawData.setCallsign(vessel.getCallsign());
        rawData.setLatitude(vessel.getLatitude());
        rawData.setLongitude(vessel.getLongitude());
        rawData.setSpeed(vessel.getSpeed());
        rawData.setCourse(vessel.getCourse() != null ? vessel.getCourse().doubleValue() : null);
        rawData.setHeading(vessel.getHeading() != null ? vessel.getHeading().doubleValue() : null);
        rawData.setNavigationStatus(vessel.getNavigationStatus());
        rawData.setVesselType(vessel.getVesselType());
        rawData.setLength(vessel.getLength());
        rawData.setWidth(vessel.getWidth());
        rawData.setDraught(vessel.getDraught());
        rawData.setFlag(vessel.getFlag());
        rawData.setDestination(vessel.getDestination());
        rawData.setEta(vessel.getEta());
        rawData.setTimestamp(vessel.getTimestamp());
        rawData.setDataQuality(vessel.getDataQuality());

        return rawData;
    }

    /**
     * Get collection statistics
     */
    public java.util.Map<String, Object> getCollectionStatistics() {
        return java.util.Map.of(
                "serviceName", "SimpleDataCollectionService",
                "description", "Collects raw data from external APIs and publishes to Kafka",
                "aircraftSources", java.util.List.of("flightradar24", "adsbexchange"),
                "vesselSources", java.util.List.of("marinetraffic", "vesselfinder", "chinaports", "marinetrafficv2"),
                "lastCollectionTime", LocalDateTime.now(),
                "status", "ACTIVE");
    }

    /**
     * Get comprehensive data collection status
     */
    public java.util.Map<String, Object> getDataCollectionStatus() {
        return java.util.Map.of(
            "serviceName", "SimpleDataCollectionService",
            "status", "RUNNING",
            "collectionsEnabled", true,
            "aircraftSources", java.util.Map.of(
                "flightradar24", java.util.Map.of("enabled", true, "lastCollection", LocalDateTime.now()),
                "adsbexchange", java.util.Map.of("enabled", true, "lastCollection", LocalDateTime.now())
            ),
            "vesselSources", java.util.Map.of(
                "marinetraffic", java.util.Map.of("enabled", true, "lastCollection", LocalDateTime.now()),
                "vesselfinder", java.util.Map.of("enabled", true, "lastCollection", LocalDateTime.now()),
                "chinaports", java.util.Map.of("enabled", true, "lastCollection", LocalDateTime.now()),
                "marinetrafficv2", java.util.Map.of("enabled", true, "lastCollection", LocalDateTime.now())
            ),
            "statistics", getCollectionStatistics(),
            "rawDataTopics", java.util.List.of(
                "raw-flightradar24-data", "raw-adsbexchange-data",
                "raw-marinetraffic-data", "raw-vesselfinder-data", 
                "raw-chinaports-data", "raw-marinetrafficv2-data"
            ),
            "totalSources", 6
        );
    }
}