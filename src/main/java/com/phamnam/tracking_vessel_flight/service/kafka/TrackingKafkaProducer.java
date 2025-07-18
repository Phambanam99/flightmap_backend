package com.phamnam.tracking_vessel_flight.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.models.raw.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.raw.RawVesselData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TrackingKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String rawAircraftDataTopic;
    private final String rawVesselDataTopic;

    // Raw Data Topics by Source - New Implementation
    private final String rawFlightRadar24DataTopic;
    private final String rawAdsbExchangeDataTopic;
    private final String rawMarineTrafficDataTopic;
    private final String rawVesselFinderDataTopic;
    private final String rawChinaportsDataTopic;
    private final String rawMarineTrafficV2DataTopic;

    private final String processedAircraftDataTopic;
    private final String processedVesselDataTopic;
    private final String realtimePositionsTopic;
    private final String alertsTopic;
    private final String notificationsTopic;
    private final String websocketUpdatesTopic;

    public TrackingKafkaProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            @Qualifier("rawAircraftDataTopicName") String rawAircraftDataTopic,
            @Qualifier("rawVesselDataTopicName") String rawVesselDataTopic,
            @Qualifier("rawFlightRadar24DataTopicName") String rawFlightRadar24DataTopic,
            @Qualifier("rawAdsbExchangeDataTopicName") String rawAdsbExchangeDataTopic,
            @Qualifier("rawMarineTrafficDataTopicName") String rawMarineTrafficDataTopic,
            @Qualifier("rawVesselFinderDataTopicName") String rawVesselFinderDataTopic,
            @Qualifier("rawChinaportsDataTopicName") String rawChinaportsDataTopic,
            @Qualifier("rawMarineTrafficV2DataTopicName") String rawMarineTrafficV2DataTopic,
            @Qualifier("processedAircraftDataTopicName") String processedAircraftDataTopic,
            @Qualifier("processedVesselDataTopicName") String processedVesselDataTopic,
            @Qualifier("realtimePositionsTopicName") String realtimePositionsTopic,
            @Qualifier("alertsTopicName") String alertsTopic,
            @Qualifier("notificationsTopicName") String notificationsTopic,
            @Qualifier("websocketUpdatesTopicName") String websocketUpdatesTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.rawAircraftDataTopic = rawAircraftDataTopic;
        this.rawVesselDataTopic = rawVesselDataTopic;
        this.rawFlightRadar24DataTopic = rawFlightRadar24DataTopic;
        this.rawAdsbExchangeDataTopic = rawAdsbExchangeDataTopic;
        this.rawMarineTrafficDataTopic = rawMarineTrafficDataTopic;
        this.rawVesselFinderDataTopic = rawVesselFinderDataTopic;
        this.rawChinaportsDataTopic = rawChinaportsDataTopic;
        this.rawMarineTrafficV2DataTopic = rawMarineTrafficV2DataTopic;
        this.processedAircraftDataTopic = processedAircraftDataTopic;
        this.processedVesselDataTopic = processedVesselDataTopic;
        this.realtimePositionsTopic = realtimePositionsTopic;
        this.alertsTopic = alertsTopic;
        this.notificationsTopic = notificationsTopic;
        this.websocketUpdatesTopic = websocketUpdatesTopic;
    }

    // Raw data publishing
    public CompletableFuture<SendResult<String, Object>> publishRawAircraftData(String key, Object data) {
        return sendMessage(rawAircraftDataTopic, key, data, "raw aircraft data");
    }

    public CompletableFuture<SendResult<String, Object>> publishRawVesselData(String key, Object data) {
        return sendMessage(rawVesselDataTopic, key, data, "raw vessel data");
    }

    // Raw Data by Source Publishing - New Implementation

    // Aircraft Raw Data Methods
    public CompletableFuture<SendResult<String, Object>> publishRawFlightRadar24Data(String key, RawAircraftData data) {
        return sendMessage(rawFlightRadar24DataTopic, key, data, "raw FlightRadar24 data");
    }

    public CompletableFuture<SendResult<String, Object>> publishRawAdsbExchangeData(String key, RawAircraftData data) {
        return sendMessage(rawAdsbExchangeDataTopic, key, data, "raw ADS-B Exchange data");
    }

    // Vessel Raw Data Methods
    public CompletableFuture<SendResult<String, Object>> publishRawMarineTrafficData(String key, RawVesselData data) {
        return sendMessage(rawMarineTrafficDataTopic, key, data, "raw MarineTraffic data");
    }

    public CompletableFuture<SendResult<String, Object>> publishRawVesselFinderData(String key, RawVesselData data) {
        return sendMessage(rawVesselFinderDataTopic, key, data, "raw VesselFinder data");
    }

    public CompletableFuture<SendResult<String, Object>> publishRawChinaportsData(String key, RawVesselData data) {
        return sendMessage(rawChinaportsDataTopic, key, data, "raw Chinaports data");
    }

    public CompletableFuture<SendResult<String, Object>> publishRawMarineTrafficV2Data(String key, RawVesselData data) {
        return sendMessage(rawMarineTrafficV2DataTopic, key, data, "raw MarineTraffic V2 data");
    }

    // Generic methods for raw data publishing by source
    public CompletableFuture<SendResult<String, Object>> publishRawAircraftDataBySource(String source, String key,
            RawAircraftData data) {
        return switch (source.toLowerCase()) {
            case "flightradar24" -> publishRawFlightRadar24Data(key, data);
            case "adsbexchange" -> publishRawAdsbExchangeData(key, data);
            default -> {
                log.warn("Unknown aircraft source: {}, publishing to generic raw aircraft topic", source);
                yield publishRawAircraftData(key, data);
            }
        };
    }

    public CompletableFuture<SendResult<String, Object>> publishRawVesselDataBySource(String source, String key,
            RawVesselData data) {
        return switch (source.toLowerCase()) {
            case "marinetraffic" -> publishRawMarineTrafficData(key, data);
            case "vesselfinder" -> publishRawVesselFinderData(key, data);
            case "chinaports" -> publishRawChinaportsData(key, data);
            case "marinetrafficv2" -> publishRawMarineTrafficV2Data(key, data);
            default -> {
                log.warn("Unknown vessel source: {}, publishing to generic raw vessel topic", source);
                yield publishRawVesselData(key, data);
            }
        };
    }

    // Processed data publishing
    public CompletableFuture<SendResult<String, Object>> publishProcessedAircraftData(String key, Object data) {
        return sendMessage(processedAircraftDataTopic, key, data, "processed aircraft data");
    }

    public CompletableFuture<SendResult<String, Object>> publishProcessedVesselData(String key, Object data) {
        return sendMessage(processedVesselDataTopic, key, data, "processed vessel data");
    }

    // Real-time position updates
    public CompletableFuture<SendResult<String, Object>> publishRealtimePosition(String entityId, Object positionData) {
        return sendMessage(realtimePositionsTopic, entityId, positionData, "realtime position");
    }

    // Alert publishing
    public CompletableFuture<SendResult<String, Object>> publishAlert(String alertId, Object alertData) {
        return sendMessage(alertsTopic, alertId, alertData, "alert");
    }

    // Notification publishing
    public CompletableFuture<SendResult<String, Object>> publishNotification(String userId, Object notification) {
        return sendMessage(notificationsTopic, userId, notification, "notification");
    }

    // WebSocket update publishing
    public CompletableFuture<SendResult<String, Object>> publishWebSocketUpdate(String sessionId, Object update) {
        return sendMessage(websocketUpdatesTopic, sessionId, update, "websocket update");
    }

    // Generic method for sending messages
    private CompletableFuture<SendResult<String, Object>> sendMessage(String topic, String key, Object data,
            String dataType) {
        try {
            // Validate input data before sending
            if (data == null) {
                log.warn("⚠️ Attempted to send null data to topic: {} with key: {}. Skipping send operation.",
                        topic, key);
                CompletableFuture<SendResult<String, Object>> skippedFuture = new CompletableFuture<>();
                skippedFuture.completeExceptionally(
                        new IllegalArgumentException("Cannot send null data to Kafka topic: " + topic));
                return skippedFuture;
            }

            if (key == null || key.trim().isEmpty()) {
                log.warn("⚠️ Attempted to send data with null/empty key to topic: {}. Skipping send operation.", topic);
                CompletableFuture<SendResult<String, Object>> skippedFuture = new CompletableFuture<>();
                skippedFuture.completeExceptionally(
                        new IllegalArgumentException("Cannot send data with null/empty key to Kafka topic: " + topic));
                return skippedFuture;
            }

            log.debug("Publishing {} to topic: {} with key: {}", dataType, topic, key);

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, data);

            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.debug("Successfully published {} to topic: {} with key: {}, offset: {}",
                            dataType, topic, key, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish {} to topic: {} with key: {}", dataType, topic, key, exception);
                }
            });

            return future;
        } catch (Exception e) {
            log.error("Error publishing {} to topic: {} with key: {}", dataType, topic, key, e);
            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    // Batch publishing methods
    public void publishBatchAircraftData(java.util.List<java.util.Map.Entry<String, Object>> batchData) {
        batchData.forEach(entry -> publishRawAircraftData(entry.getKey(), entry.getValue()));
    }

    public void publishBatchVesselData(java.util.List<java.util.Map.Entry<String, Object>> batchData) {
        batchData.forEach(entry -> publishRawVesselData(entry.getKey(), entry.getValue()));
    }

    // Health check method
    public boolean isHealthy() {
        try {
            // Simple health check - try to get metadata
            kafkaTemplate.getProducerFactory().createProducer().partitionsFor(rawAircraftDataTopic);
            return true;
        } catch (Exception e) {
            log.error("Kafka producer health check failed", e);
            return false;
        }
    }
}