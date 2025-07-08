package com.phamnam.tracking_vessel_flight.service.kafka;

import com.phamnam.tracking_vessel_flight.models.raw.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.raw.RawVesselData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Raw Data Topics Consumer
 * 
 * This service provides monitoring, logging, and metrics for the new raw data
 * topics.
 * It consumes from source-specific raw data topics to:
 * 1. Track message processing metrics
 * 2. Log data flow for debugging
 * 3. Monitor data quality and volume
 * 4. Provide backup consumption for resilience
 * 
 * Note: The primary consumption and fusion is handled by
 * ConsumerBasedDataFusionService.
 * This service provides supplementary monitoring and analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RawDataTopicsConsumer {

    // Metrics tracking
    private final AtomicLong totalAircraftMessages = new AtomicLong(0);
    private final AtomicLong totalVesselMessages = new AtomicLong(0);

    // Source-specific metrics
    private final Map<String, AtomicLong> sourceMetrics = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastMessageTime = new ConcurrentHashMap<>();

    // Data quality metrics
    private final AtomicLong validMessages = new AtomicLong(0);
    private final AtomicLong invalidMessages = new AtomicLong(0);

    // ============================================================================
    // AIRCRAFT RAW DATA MONITORING CONSUMERS
    // ============================================================================

    @KafkaListener(topics = "${app.kafka.topics.raw-flightradar24-data}", groupId = "raw-data-monitoring-group", containerFactory = "kafkaListenerContainerFactory")
    public void monitorFlightRadar24Data(
            @Payload RawAircraftData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        processAircraftDataMonitoring("flightradar24", key, rawData, topic, partition, offset, acknowledgment);
    }

    @KafkaListener(topics = "${app.kafka.topics.raw-adsbexchange-data}", groupId = "raw-data-monitoring-group", containerFactory = "kafkaListenerContainerFactory")
    public void monitorAdsbExchangeData(
            @Payload RawAircraftData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        processAircraftDataMonitoring("adsbexchange", key, rawData, topic, partition, offset, acknowledgment);
    }

    // ============================================================================
    // VESSEL RAW DATA MONITORING CONSUMERS
    // ============================================================================

    @KafkaListener(topics = "${app.kafka.topics.raw-marinetraffic-data}", groupId = "raw-data-monitoring-group", containerFactory = "kafkaListenerContainerFactory")
    public void monitorMarineTrafficData(
            @Payload RawVesselData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        processVesselDataMonitoring("marinetraffic", key, rawData, topic, partition, offset, acknowledgment);
    }

    @KafkaListener(topics = "${app.kafka.topics.raw-vesselfinder-data}", groupId = "raw-data-monitoring-group", containerFactory = "kafkaListenerContainerFactory")
    public void monitorVesselFinderData(
            @Payload RawVesselData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        processVesselDataMonitoring("vesselfinder", key, rawData, topic, partition, offset, acknowledgment);
    }

    @KafkaListener(topics = "${app.kafka.topics.raw-chinaports-data}", groupId = "raw-data-monitoring-group", containerFactory = "kafkaListenerContainerFactory")
    public void monitorChinaportsData(
            @Payload RawVesselData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        processVesselDataMonitoring("chinaports", key, rawData, topic, partition, offset, acknowledgment);
    }

    @KafkaListener(topics = "${app.kafka.topics.raw-marinetrafficv2-data}", groupId = "raw-data-monitoring-group", containerFactory = "kafkaListenerContainerFactory")
    public void monitorMarineTrafficV2Data(
            @Payload RawVesselData rawData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        processVesselDataMonitoring("marinetrafficv2", key, rawData, topic, partition, offset, acknowledgment);
    }

    // ============================================================================
    // MONITORING PROCESSORS
    // ============================================================================

    /**
     * Process aircraft data monitoring
     */
    private void processAircraftDataMonitoring(String source, String key, RawAircraftData rawData,
            String topic, int partition, long offset,
            Acknowledgment acknowledgment) {
        try {
            // Update metrics
            totalAircraftMessages.incrementAndGet();
            sourceMetrics.computeIfAbsent(source, k -> new AtomicLong(0)).incrementAndGet();
            lastMessageTime.put(source, LocalDateTime.now());

            // Validate data quality
            boolean isValid = validateAircraftData(rawData);
            if (isValid) {
                validMessages.incrementAndGet();
            } else {
                invalidMessages.incrementAndGet();
                log.warn("⚠️ Invalid aircraft data from {}: key={}, hexident={}",
                        source, key, rawData.getHexident());
            }

            // Log monitoring info (debug level to avoid spam)
            log.debug("📊 MONITOR [{}] Aircraft: key={}, topic={}, partition={}, offset={}, quality={}",
                    source, key, topic, partition, offset, rawData.getDataQuality());

            // Periodic summary logging (every 100 messages per source)
            long sourceCount = sourceMetrics.get(source).get();
            if (sourceCount % 100 == 0) {
                log.info("📈 SUMMARY [{}]: {} aircraft messages processed, last quality: {}",
                        source, sourceCount, rawData.getDataQuality());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error monitoring aircraft data from {}: {}", source, e.getMessage(), e);
            acknowledgment.acknowledge(); // Acknowledge to prevent reprocessing
        }
    }

    /**
     * Process vessel data monitoring
     */
    private void processVesselDataMonitoring(String source, String key, RawVesselData rawData,
            String topic, int partition, long offset,
            Acknowledgment acknowledgment) {
        try {
            // Update metrics
            totalVesselMessages.incrementAndGet();
            sourceMetrics.computeIfAbsent(source, k -> new AtomicLong(0)).incrementAndGet();
            lastMessageTime.put(source, LocalDateTime.now());

            // Validate data quality
            boolean isValid = validateVesselData(rawData);
            if (isValid) {
                validMessages.incrementAndGet();
            } else {
                invalidMessages.incrementAndGet();
                log.warn("⚠️ Invalid vessel data from {}: key={}, mmsi={}",
                        source, key, rawData.getMmsi());
            }

            // Log monitoring info (debug level to avoid spam)
            log.debug("📊 MONITOR [{}] Vessel: key={}, topic={}, partition={}, offset={}, quality={}",
                    source, key, topic, partition, offset, rawData.getDataQuality());

            // Periodic summary logging (every 50 messages per source)
            long sourceCount = sourceMetrics.get(source).get();
            if (sourceCount % 50 == 0) {
                log.info("📈 SUMMARY [{}]: {} vessel messages processed, last quality: {}",
                        source, sourceCount, rawData.getDataQuality());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error monitoring vessel data from {}: {}", source, e.getMessage(), e);
            acknowledgment.acknowledge(); // Acknowledge to prevent reprocessing
        }
    }

    // ============================================================================
    // DATA VALIDATION
    // ============================================================================

    /**
     * Validate aircraft raw data
     */
    private boolean validateAircraftData(RawAircraftData data) {
        if (data == null)
            return false;
        if (data.getHexident() == null || data.getHexident().trim().isEmpty())
            return false;
        if (data.getLatitude() == null || data.getLongitude() == null)
            return false;
        if (data.getLatitude() < -90 || data.getLatitude() > 90)
            return false;
        if (data.getLongitude() < -180 || data.getLongitude() > 180)
            return false;
        if (data.getDataQuality() == null || data.getDataQuality() < 0 || data.getDataQuality() > 1)
            return false;
        return true;
    }

    /**
     * Validate vessel raw data
     */
    private boolean validateVesselData(RawVesselData data) {
        if (data == null)
            return false;
        if (!data.hasValidMmsi())
            return false;
        if (!data.hasValidPosition())
            return false;
        if (data.getDataQuality() == null || data.getDataQuality() < 0 || data.getDataQuality() > 1)
            return false;
        return true;
    }

    // ============================================================================
    // METRICS AND STATUS
    // ============================================================================

    /**
     * Get comprehensive monitoring metrics
     */
    public Map<String, Object> getMonitoringMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();

        // Overall metrics
        metrics.put("totalAircraftMessages", totalAircraftMessages.get());
        metrics.put("totalVesselMessages", totalVesselMessages.get());
        metrics.put("totalMessages", totalAircraftMessages.get() + totalVesselMessages.get());
        metrics.put("validMessages", validMessages.get());
        metrics.put("invalidMessages", invalidMessages.get());

        // Calculate success rate
        long total = validMessages.get() + invalidMessages.get();
        double successRate = total > 0 ? (double) validMessages.get() / total : 0.0;
        metrics.put("dataQualityRate", successRate);

        // Source-specific metrics
        Map<String, Object> sourceCounts = new ConcurrentHashMap<>();
        Map<String, Object> sourceTimestamps = new ConcurrentHashMap<>();

        sourceMetrics.forEach((source, count) -> {
            sourceCounts.put(source, count.get());
            sourceTimestamps.put(source, lastMessageTime.get(source));
        });

        metrics.put("sourceMetrics", sourceCounts);
        metrics.put("lastMessageTimes", sourceTimestamps);

        // Service info
        metrics.put("serviceName", "RawDataTopicsConsumer");
        metrics.put("status", "RUNNING");
        metrics.put("timestamp", LocalDateTime.now());

        return metrics;
    }

    /**
     * Get source-specific status
     */
    public Map<String, Object> getSourceStatus(String source) {
        return Map.of(
                "source", source,
                "messageCount", sourceMetrics.getOrDefault(source, new AtomicLong(0)).get(),
                "lastMessageTime", lastMessageTime.get(source),
                "isActive", isSourceActive(source),
                "timestamp", LocalDateTime.now());
    }

    /**
     * Check if source is active (received message in last 5 minutes)
     */
    private boolean isSourceActive(String source) {
        LocalDateTime lastTime = lastMessageTime.get(source);
        if (lastTime == null)
            return false;
        return lastTime.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    /**
     * Get data quality report
     */
    public Map<String, Object> getDataQualityReport() {
        long total = validMessages.get() + invalidMessages.get();

        return Map.of(
                "totalProcessed", total,
                "validMessages", validMessages.get(),
                "invalidMessages", invalidMessages.get(),
                "qualityRate", total > 0 ? (double) validMessages.get() / total : 0.0,
                "qualityStatus",
                total > 0 && ((double) validMessages.get() / total) > 0.95 ? "GOOD" : "NEEDS_ATTENTION",
                "timestamp", LocalDateTime.now());
    }

    /**
     * Reset metrics (for testing or periodic reset)
     */
    public void resetMetrics() {
        totalAircraftMessages.set(0);
        totalVesselMessages.set(0);
        validMessages.set(0);
        invalidMessages.set(0);
        sourceMetrics.clear();
        lastMessageTime.clear();
        log.info("📊 Raw data monitoring metrics reset");
    }

    /**
     * Log comprehensive status summary
     */
    public void logStatusSummary() {
        Map<String, Object> metrics = getMonitoringMetrics();

        log.info("📊 RAW DATA MONITORING SUMMARY:");
        log.info("  📈 Total Messages: {}", metrics.get("totalMessages"));
        log.info("  ✈️ Aircraft Messages: {}", metrics.get("totalAircraftMessages"));
        log.info("  🚢 Vessel Messages: {}", metrics.get("totalVesselMessages"));
        log.info("  ✅ Data Quality Rate: {:.2f}%", (Double) metrics.get("dataQualityRate") * 100);
        log.info("  📊 Source Breakdown:");

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceCounts = (Map<String, Object>) metrics.get("sourceMetrics");
        sourceCounts.forEach((source, count) -> log.info("    - {}: {} messages", source, count));
    }
}