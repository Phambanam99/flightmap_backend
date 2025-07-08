package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.raw.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.raw.RawVesselData;
import com.phamnam.tracking_vessel_flight.service.kafka.TrackingKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Consumer-Based Data Fusion Service
 * 
 * This service listens to raw data topics from different sources and performs
 * data fusion:
 * 1. Consumes raw data from source-specific Kafka topics
 * 2. Collects data in time windows for fusion
 * 3. Merges data from multiple sources using prioritization and quality
 * algorithms
 * 4. Publishes fused data to processed data topics
 * 
 * This approach separates data collection from data fusion, enabling:
 * - Better scalability (multiple fusion consumers)
 * - Raw data preservation for other use cases
 * - Independent processing of different data sources
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerBasedDataFusionService {

    private final TrackingKafkaProducer kafkaProducer;
    private final DataFusionService dataFusionService; // Reuse existing fusion logic

    @Value("${data.fusion.enabled:true}")
    private boolean fusionEnabled;

    @Value("${data.fusion.collection-window-ms:5000}")
    private long collectionWindowMs;

    @Value("${data.fusion.max-batch-size:1000}")
    private int maxBatchSize;

    // Data collection buffers for fusion
    private final Map<String, RawAircraftData> aircraftBuffer = new ConcurrentHashMap<>();
    private final Map<String, RawVesselData> vesselBuffer = new ConcurrentHashMap<>();

    // Time-based triggers for fusion
    private final ScheduledExecutorService fusionScheduler = Executors.newScheduledThreadPool(2);

    // Source tracking for fusion windows
    private final Map<String, LocalDateTime> lastAircraftFusion = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastVesselFusion = new ConcurrentHashMap<>();

    /**
     * Initialize scheduled fusion triggers
     */
    @PostConstruct
    public void initializeFusionTriggers() {
        // Aircraft fusion trigger - every 5 seconds
        fusionScheduler.scheduleAtFixedRate(
                this::triggerAircraftFusion,
                5, 5, TimeUnit.SECONDS);

        // Vessel fusion trigger - every 10 seconds
        fusionScheduler.scheduleAtFixedRate(
                this::triggerVesselFusion,
                10, 10, TimeUnit.SECONDS);

        log.info("✅ Consumer-based data fusion service initialized with {}ms collection window",
                collectionWindowMs);
    }

    // ============================================================================
    // AIRCRAFT DATA CONSUMERS
    // ============================================================================

    @KafkaListener(topics = "${app.kafka.topics.raw-flightradar24-data}", groupId = "aircraft-fusion-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeFlightRadar24Data(
            @Payload RawAircraftData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        processRawAircraftData("flightradar24", key, rawData, acknowledgment);
    }

    @KafkaListener(topics = "${app.kafka.topics.raw-adsbexchange-data}", groupId = "aircraft-fusion-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeAdsbExchangeData(
            @Payload RawAircraftData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        processRawAircraftData("adsbexchange", key, rawData, acknowledgment);
    }

    // ============================================================================
    // VESSEL DATA CONSUMERS
    // ============================================================================

    @KafkaListener(topics = "${app.kafka.topics.raw-marinetraffic-data}", groupId = "vessel-fusion-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeMarineTrafficData(
            @Payload RawVesselData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        processRawVesselData("marinetraffic", key, rawData, acknowledgment);
    }

    @KafkaListener(topics = "${app.kafka.topics.raw-vesselfinder-data}", groupId = "vessel-fusion-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeVesselFinderData(
            @Payload RawVesselData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        processRawVesselData("vesselfinder", key, rawData, acknowledgment);
    }

    @KafkaListener(topics = "${app.kafka.topics.raw-chinaports-data}", groupId = "vessel-fusion-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeChinaportsData(
            @Payload RawVesselData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        processRawVesselData("chinaports", key, rawData, acknowledgment);
    }

    @KafkaListener(topics = "${app.kafka.topics.raw-marinetrafficv2-data}", groupId = "vessel-fusion-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeMarineTrafficV2Data(
            @Payload RawVesselData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        processRawVesselData("marinetrafficv2", key, rawData, acknowledgment);
    }

    // ============================================================================
    // RAW DATA PROCESSING
    // ============================================================================

    /**
     * Process raw aircraft data - add to buffer for fusion
     */
    private void processRawAircraftData(String source, String key, RawAircraftData rawData,
            Acknowledgment acknowledgment) {
        try {
            log.debug("📡 Received aircraft data from {} for key: {}", source, key);

            // Validate raw data
            if (!isValidAircraftData(rawData)) {
                log.warn("⚠️ Invalid aircraft data from {} for key: {}", source, key);
                acknowledgment.acknowledge();
                return;
            }

            // Set source information
            rawData.setSource(source);
            rawData.markAsProcessed();

            // Add to buffer for fusion
            String bufferKey = source + ":" + key;
            aircraftBuffer.put(bufferKey, rawData);

            log.debug("✅ Added aircraft {} from {} to fusion buffer (buffer size: {})",
                    key, source, aircraftBuffer.size());

            // Trigger fusion if buffer is large or time window exceeded
            checkAndTriggerAircraftFusion();

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing aircraft data from {} for key {}: {}",
                    source, key, e.getMessage(), e);
        }
    }

    /**
     * Process raw vessel data - add to buffer for fusion
     */
    private void processRawVesselData(String source, String key, RawVesselData rawData,
            Acknowledgment acknowledgment) {
        try {
            log.debug("🚢 Received vessel data from {} for key: {}", source, key);

            // Validate raw data
            if (!isValidVesselData(rawData)) {
                log.warn("⚠️ Invalid vessel data from {} for key: {}", source, key);
                acknowledgment.acknowledge();
                return;
            }

            // Set source information
            rawData.setSource(source);
            rawData.markAsProcessed();

            // Add to buffer for fusion
            String bufferKey = source + ":" + key;
            vesselBuffer.put(bufferKey, rawData);

            log.debug("✅ Added vessel {} from {} to fusion buffer (buffer size: {})",
                    key, source, vesselBuffer.size());

            // Trigger fusion if buffer is large or time window exceeded
            checkAndTriggerVesselFusion();

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing vessel data from {} for key {}: {}",
                    source, key, e.getMessage(), e);
        }
    }

    // ============================================================================
    // FUSION TRIGGERS
    // ============================================================================

    /**
     * Check and trigger aircraft fusion based on buffer size or time
     */
    private void checkAndTriggerAircraftFusion() {
        if (aircraftBuffer.size() >= maxBatchSize) {
            log.info("🚀 Triggering aircraft fusion - buffer size reached: {}", aircraftBuffer.size());
            triggerAircraftFusion();
        }
    }

    /**
     * Check and trigger vessel fusion based on buffer size or time
     */
    private void checkAndTriggerVesselFusion() {
        if (vesselBuffer.size() >= maxBatchSize) {
            log.info("🚀 Triggering vessel fusion - buffer size reached: {}", vesselBuffer.size());
            triggerVesselFusion();
        }
    }

    /**
     * Trigger aircraft data fusion
     */
    public void triggerAircraftFusion() {
        if (aircraftBuffer.isEmpty()) {
            return;
        }

        try {
            Map<String, RawAircraftData> currentBuffer = new HashMap<>(aircraftBuffer);
            aircraftBuffer.clear();

            log.info("🔄 Starting aircraft fusion with {} raw data points", currentBuffer.size());

            // Group by hexident and convert to tracking requests
            Map<String, List<AircraftTrackingRequest>> dataBySource = groupAircraftDataBySource(currentBuffer);

            // Use existing fusion logic
            List<AircraftTrackingRequest> fusedData = dataFusionService.mergeAircraftData(dataBySource);

            // Publish fused data to processed topic
            for (AircraftTrackingRequest aircraft : fusedData) {
                kafkaProducer.publishProcessedAircraftData(aircraft.getHexident(), aircraft);
            }

            log.info("✅ Aircraft fusion completed: {} -> {} records published",
                    currentBuffer.size(), fusedData.size());

        } catch (Exception e) {
            log.error("❌ Error during aircraft fusion: {}", e.getMessage(), e);
        }
    }

    /**
     * Trigger vessel data fusion
     */
    public void triggerVesselFusion() {
        if (vesselBuffer.isEmpty()) {
            return;
        }

        try {
            Map<String, RawVesselData> currentBuffer = new HashMap<>(vesselBuffer);
            vesselBuffer.clear();

            log.info("🔄 Starting vessel fusion with {} raw data points", currentBuffer.size());

            // Group by source and convert to tracking requests
            Map<String, List<VesselTrackingRequest>> dataBySource = groupVesselDataBySource(currentBuffer);

            // Use existing fusion logic
            List<VesselTrackingRequest> fusedData = dataFusionService.mergeVesselData(dataBySource);

            // Publish fused data to processed topic
            for (VesselTrackingRequest vessel : fusedData) {
                kafkaProducer.publishProcessedVesselData(vessel.getMmsi(), vessel);
            }

            log.info("✅ Vessel fusion completed: {} -> {} records published",
                    currentBuffer.size(), fusedData.size());

        } catch (Exception e) {
            log.error("❌ Error during vessel fusion: {}", e.getMessage(), e);
        }
    }

    // ============================================================================
    // DATA CONVERSION AND GROUPING
    // ============================================================================

    /**
     * Group aircraft data by source for fusion
     */
    private Map<String, List<AircraftTrackingRequest>> groupAircraftDataBySource(
            Map<String, RawAircraftData> buffer) {

        Map<String, List<AircraftTrackingRequest>> grouped = new HashMap<>();

        for (Map.Entry<String, RawAircraftData> entry : buffer.entrySet()) {
            RawAircraftData rawData = entry.getValue();
            String source = rawData.getSource();

            AircraftTrackingRequest trackingRequest = convertToAircraftTrackingRequest(rawData);
            grouped.computeIfAbsent(source, k -> new ArrayList<>()).add(trackingRequest);
        }

        return grouped;
    }

    /**
     * Group vessel data by source for fusion
     */
    private Map<String, List<VesselTrackingRequest>> groupVesselDataBySource(
            Map<String, RawVesselData> buffer) {

        Map<String, List<VesselTrackingRequest>> grouped = new HashMap<>();

        for (Map.Entry<String, RawVesselData> entry : buffer.entrySet()) {
            RawVesselData rawData = entry.getValue();
            String source = rawData.getSource();

            VesselTrackingRequest trackingRequest = convertToVesselTrackingRequest(rawData);
            grouped.computeIfAbsent(source, k -> new ArrayList<>()).add(trackingRequest);
        }

        return grouped;
    }

    /**
     * Convert RawAircraftData to AircraftTrackingRequest
     */
    private AircraftTrackingRequest convertToAircraftTrackingRequest(RawAircraftData rawData) {
        return AircraftTrackingRequest.builder()
                .hexident(rawData.getHexident())
                .callsign(rawData.getCallsign())
                .latitude(rawData.getLatitude())
                .longitude(rawData.getLongitude())
                .altitude(rawData.getAltitude())
                .groundSpeed(rawData.getGroundSpeed())
                .track(rawData.getTrack())
                .verticalRate(rawData.getVerticalRate())
                .squawk(rawData.getSquawk())
                .aircraftType(rawData.getAircraftType())
                .registration(rawData.getRegistration())
                .onGround(rawData.getOnGround())
                .emergency(rawData.getEmergency())
                .timestamp(rawData.getTimestamp())
                .dataQuality(rawData.getDataQuality())
                .source(rawData.getSource())
                .build();
    }

    /**
     * Convert RawVesselData to VesselTrackingRequest
     */
    private VesselTrackingRequest convertToVesselTrackingRequest(RawVesselData rawData) {
        return VesselTrackingRequest.builder()
                .mmsi(rawData.getMmsi())
                .imo(rawData.getImo())
                .vesselName(rawData.getVesselName())
                .callsign(rawData.getCallsign())
                .latitude(rawData.getLatitude())
                .longitude(rawData.getLongitude())
                .speed(rawData.getSpeed())
                .course(rawData.getCourse() != null ? rawData.getCourse().intValue() : null)
                .heading(rawData.getHeading() != null ? rawData.getHeading().intValue() : null)
                .navigationStatus(rawData.getNavigationStatus())
                .vesselType(rawData.getVesselType())
                .length(rawData.getLength())
                .width(rawData.getWidth())
                .draught(rawData.getDraught())
                .flag(rawData.getFlag())
                .destination(rawData.getDestination())
                .eta(rawData.getEta())
                .timestamp(rawData.getTimestamp())
                .dataQuality(rawData.getDataQuality())
                .source(rawData.getSource())
                .build();
    }

    // ============================================================================
    // VALIDATION
    // ============================================================================

    /**
     * Validate aircraft data
     */
    private boolean isValidAircraftData(RawAircraftData data) {
        return data != null &&
                data.getHexident() != null &&
                data.getLatitude() != null &&
                data.getLongitude() != null &&
                data.getLatitude() >= -90 && data.getLatitude() <= 90 &&
                data.getLongitude() >= -180 && data.getLongitude() <= 180;
    }

    /**
     * Validate vessel data
     */
    private boolean isValidVesselData(RawVesselData data) {
        return data != null &&
                data.getMmsi() != null &&
                data.hasValidPosition() &&
                data.hasValidMmsi();
    }

    // ============================================================================
    // STATUS AND MONITORING
    // ============================================================================

    /**
     * Get fusion service status
     */
    public Map<String, Object> getFusionStatus() {
        return Map.of(
                "serviceName", "ConsumerBasedDataFusionService",
                "fusionEnabled", fusionEnabled,
                "collectionWindowMs", collectionWindowMs,
                "maxBatchSize", maxBatchSize,
                "aircraftBufferSize", aircraftBuffer.size(),
                "vesselBufferSize", vesselBuffer.size(),
                "status", "RUNNING");
    }

    /**
     * Cleanup resources on shutdown
     */
    public void shutdown() {
        fusionScheduler.shutdown();
        try {
            if (!fusionScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                fusionScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            fusionScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}