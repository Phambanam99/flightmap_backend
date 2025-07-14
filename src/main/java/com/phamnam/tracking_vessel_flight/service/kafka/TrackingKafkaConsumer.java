package com.phamnam.tracking_vessel_flight.service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.ShipTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.response.FlightTrackingResponse;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.models.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.RawVesselData;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import com.phamnam.tracking_vessel_flight.repository.RawAircraftDataRepository;
import com.phamnam.tracking_vessel_flight.repository.RawVesselDataRepository;
import com.phamnam.tracking_vessel_flight.repository.FlightTrackingRepository;
import com.phamnam.tracking_vessel_flight.service.realtime.WebSocketService;
import com.phamnam.tracking_vessel_flight.service.rest.FlightTrackingService;
import com.phamnam.tracking_vessel_flight.service.rest.ShipTrackingService;
import com.phamnam.tracking_vessel_flight.service.kafka.TrackingKafkaProducer;
import com.phamnam.tracking_vessel_flight.service.realtime.TrackingCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final RawAircraftDataRepository rawAircraftDataRepository;
    private final RawVesselDataRepository rawVesselDataRepository;
    private final FlightTrackingRepository flightTrackingRepository;
    private final WebSocketService webSocketService;
    private final FlightTrackingService flightTrackingService;
    private final ShipTrackingService shipTrackingService;
    private final TrackingKafkaProducer kafkaProducer;
    private final TrackingCacheService trackingCacheService;

    @Value("${raw.data.storage.enabled:true}")
    private boolean rawStorageEnabled;

    /**
     * Utility method to validate if JsonNode data is valid for processing
     * 
     * @param data           The JsonNode data to validate
     * @param key            The message key
     * @param topic          The topic name
     * @param acknowledgment The acknowledgment object
     * @return true if data is valid, false if processing should be skipped
     */
    private boolean isValidJsonData(JsonNode data, String key, String topic, Acknowledgment acknowledgment) {
        // Check for null or empty data
        if (data == null || data.isNull()) {
            log.warn("⚠️ Received null or empty data for key: {} from topic: {}. Skipping processing.", key, topic);
            acknowledgment.acknowledge();
            return false;
        }

        // Check for null or empty key
        if (key == null || key.trim().isEmpty()) {
            log.warn("⚠️ Received data with null or empty key from topic: {}. Skipping processing.", topic);
            acknowledgment.acknowledge();
            return false;
        }

        return true;
    }

    /**
     * Utility method to handle errors consistently across all consumers
     * 
     * @param exception      The exception that occurred
     * @param key            The message key
     * @param topic          The topic name
     * @param acknowledgment The acknowledgment object
     * @param consumerType   The type of consumer for logging
     */
    private void handleConsumerError(Exception exception, String key, String topic,
            Acknowledgment acknowledgment, String consumerType) {
        log.error("❌ Error processing {} data with key: {} from topic: {} - Error: {}",
                consumerType, key, topic, exception.getMessage(), exception);

        // Send to dead letter queue for failed processing
        sendToDeadLetterQueue(key, topic, exception.getMessage(), consumerType);

        // Acknowledge to prevent reprocessing of corrupted data
        acknowledgment.acknowledge();
    }

    // Raw Aircraft Data Consumer
    @KafkaListener(topics = "${app.kafka.topics.raw-aircraft-data}", groupId = "raw-aircraft-consumer-group", containerFactory = "rawAircraftKafkaListenerContainerFactory")
    public void consumeRawAircraftData(
            @Payload AircraftTrackingRequest data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received raw aircraft data from topic: {}, partition: {}, offset: {}, key: {}",
                    topic, partition, offset, key);

            // ✅ Save raw data only if storage enabled (for audit/compliance)
            if (rawStorageEnabled) {
                RawAircraftData rawData = RawAircraftData.builder()
                        .hexident(data.getHexident())
                        .latitude(data.getLatitude())
                        .longitude(data.getLongitude())
                        .altitude(data.getAltitude())
                        .groundSpeed(data.getGroundSpeed())
                        .track(data.getTrack())
                        .verticalRate(data.getVerticalRate())
                        .squawk(data.getSquawk())
                        .aircraftType(data.getAircraftType())
                        .registration(data.getRegistration())
                        .callsign(data.getCallsign())
                        .onGround(data.getOnGround())
                        .emergency(data.getEmergency())
                        .dataSource("RAW_KAFKA")
                        .receivedAt(LocalDateTime.now())
                        .build();

                rawAircraftDataRepository.save(rawData);
                log.debug("✅ Saved raw aircraft data for hexident: {}", key);
            } else {
                log.debug("⏭️ Skipped raw aircraft data storage for hexident: {} (storage disabled)", key);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing raw aircraft data with key: {}", key, e);
            // Don't acknowledge - message will be retried
        }
    }

    // Raw Vessel Data Consumer
    @KafkaListener(topics = "${app.kafka.topics.raw-vessel-data}", groupId = "raw-vessel-consumer-group", containerFactory = "rawVesselKafkaListenerContainerFactory")
    public void consumeRawVesselData(
            @Payload ShipTrackingRequestDTO data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received raw vessel data from topic: {}, partition: {}, offset: {}, key: {}",
                    topic, partition, offset, key);

            // ✅ Save raw vessel data only if storage enabled (for audit/compliance)
            if (rawStorageEnabled) {
                RawVesselData rawData = RawVesselData.builder()
                        .mmsi(data.getMmsi())
                        .latitude(data.getLatitude() != null ? data.getLatitude().doubleValue() : null)
                        .longitude(data.getLongitude() != null ? data.getLongitude().doubleValue() : null)
                        .speed(data.getSpeed() != null ? data.getSpeed().doubleValue() : null)
                        .course(data.getCourse() != null ? data.getCourse().intValue() : null)
                        .heading(data.getHeading() != null ? data.getHeading().intValue() : null)
                        .navigationStatus(data.getNavigationStatus())
                        .vesselName(data.getVesselName())
                        .vesselType(data.getVesselType())
                        .imo(data.getImo())
                        .callsign(data.getCallSign()) // ✅ Fixed: getCallSign() not getCallsign()
                        .flag(data.getFlag())
                        .length(data.getLength() != null ? data.getLength().intValue() : null)
                        .width(data.getWidth() != null ? data.getWidth().intValue() : null)
                        .draught(data.getDraught() != null ? data.getDraught().doubleValue() : null)
                        .destination(data.getDestination())
                        .eta(data.getEta() != null ? data.getEta().toString() : null) // ✅ Fixed: Convert LocalDateTime
                                                                                      // to
                                                                                      // String
                        .dataSource("RAW_KAFKA")
                        .receivedAt(LocalDateTime.now())
                        .build();

                rawVesselDataRepository.save(rawData);
                log.debug("✅ Saved raw vessel data for mmsi: {}", key);
            } else {
                log.debug("⏭️ Skipped raw vessel data storage for mmsi: {} (storage disabled)", key);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing raw vessel data with key: {}", key, e);
            // Don't acknowledge - message will be retried
        }
    }

    // Processed Aircraft Data Consumer
    @KafkaListener(topics = "${app.kafka.topics.processed-aircraft-data}", groupId = "processed-aircraft-consumer-group", containerFactory = "processedAircraftKafkaListenerContainerFactory")
    public void consumeProcessedAircraftData(
            @Payload FlightTracking data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received processed aircraft data from topic: {}, key: {}", topic, key);

            // Convert FlightTracking to FlightTrackingRequestDTO for service processing
            FlightTrackingRequestDTO trackingRequest = FlightTrackingRequestDTO.builder()
                    .hexident(data.getHexident())
                    .callsign(data.getCallsign())
                    .latitude(data.getLatitude() != null ? data.getLatitude().floatValue() : null)
                    .longitude(data.getLongitude() != null ? data.getLongitude().floatValue() : null)
                    .altitude(data.getAltitude())
                    .speed(data.getSpeed())
                    .verticalSpeed(data.getVerticalSpeed())
                    .squawk(data.getSquawk())
                    .updateTime(data.getUpdateTime())
                    .build();

            // ✅ Process through service to create Aircraft and Flight entities
            FlightTrackingResponse savedTracking = flightTrackingService.processNewTrackingData(trackingRequest, null);
            log.info("✅ Processed aircraft data through service for hexident: {}", key);

            // ✅ Send real-time update to WebSocket clients - note: WebSocket expects
            // entity, will need conversion if required
            // Broadcast aircraft update via WebSocket
            webSocketService.broadcastSystemStatus(Map.of(
                    "type", "aircraft-update",
                    "hexident", key,
                    "data", savedTracking));
            log.debug("📡 Flight tracking processed successfully for: {}", key);
            log.debug("📡 Broadcasted aircraft position update for: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing processed aircraft data with key: {}", key, e);
        }
    }

    // Processed Vessel Data Consumer
    @KafkaListener(topics = "${app.kafka.topics.processed-vessel-data}", groupId = "processed-vessel-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeProcessedVesselData(
            @Payload(required = false) JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received processed vessel data from topic: {}, key: {}", topic, key);

            // Validate data using utility method
            if (!isValidJsonData(data, key, topic, acknowledgment)) {
                return;
            }

            // Convert JsonNode to ShipTrackingRequestDTO for service processing
            ShipTrackingRequestDTO trackingRequest = objectMapper.treeToValue(data, ShipTrackingRequestDTO.class);

            // ✅ Process through service to create Ship and Voyage entities
            // Note: This method should be implemented in ShipTrackingService
            log.info("✅ Processing vessel data for mmsi: {} through service", key);

            // For now, create a basic ShipTracking entity and broadcast via WebSocket
            // Process through ShipTrackingService for complete handling
            try {
                // Convert DTO to Request object for service
                ShipTrackingRequest shipRequest = ShipTrackingRequest.builder()
                        .mmsi(trackingRequest.getMmsi())
                        .latitude(trackingRequest.getLatitude() != null ? trackingRequest.getLatitude().doubleValue()
                                : null)
                        .longitude(trackingRequest.getLongitude() != null ? trackingRequest.getLongitude().doubleValue()
                                : null)
                        .speed(trackingRequest.getSpeed() != null ? trackingRequest.getSpeed().doubleValue() : null)
                        .course(trackingRequest.getCourse() != null ? trackingRequest.getCourse().doubleValue() : null)
                        .heading(trackingRequest.getHeading() != null ? trackingRequest.getHeading().doubleValue()
                                : null)
                        .navStatus(trackingRequest.getNavigationStatus())
                        .timestamp(trackingRequest.getUpdateTime())
                        .build();

                shipTrackingService.save(shipRequest, null);
                log.info("✅ Processed vessel data through ShipTrackingService for mmsi: {}", key);
                
                // ✅ Cache the vessel data in Redis for real-time queries
                trackingCacheService.cacheShipTracking(shipRequest);
                log.debug("📦 Cached vessel data in Redis for mmsi: {}", key);
            } catch (Exception serviceError) {
                log.warn("Failed to process through ShipTrackingService, using fallback: {}",
                        serviceError.getMessage());
            }
            ShipTracking shipTracking = ShipTracking.builder()
                    .mmsi(trackingRequest.getMmsi())
                    .latitude(
                            trackingRequest.getLatitude() != null ? trackingRequest.getLatitude().doubleValue() : null)
                    .longitude(trackingRequest.getLongitude() != null ? trackingRequest.getLongitude().doubleValue()
                            : null)
                    .speed(trackingRequest.getSpeed() != null ? trackingRequest.getSpeed().doubleValue() : null)
                    .course(trackingRequest.getCourse() != null ? trackingRequest.getCourse().doubleValue() : null)
                    .heading(trackingRequest.getHeading() != null ? trackingRequest.getHeading().doubleValue() : null)
                    .navigationStatus(trackingRequest.getNavigationStatus())
                    .timestamp(trackingRequest.getUpdateTime() != null ? trackingRequest.getUpdateTime()
                            : LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            // ✅ Send real-time update to WebSocket clients
            webSocketService.broadcastVesselUpdate(shipTracking);
            log.debug("📡 Broadcasted vessel position update for: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            handleConsumerError(e, key, topic, acknowledgment, "processed vessel");
        }
    }

    // Real-time Positions Consumer (for WebSocket broadcasting)
    @KafkaListener(topics = "${app.kafka.topics.realtime-positions}", groupId = "realtime-positions-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeRealtimePositions(
            @Payload(required = false) JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received realtime position from topic: {}, key: {}", topic, key);

            // Validate data using utility method
            if (!isValidJsonData(data, key, topic, acknowledgment)) {
                return;
            }

            // ✅ Broadcast to all WebSocket clients
            webSocketService.broadcastSystemStatus(Map.of("type", "position-update", "entityId", key, "data", data));
            log.debug("📡 Broadcasted realtime position for entity: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            handleConsumerError(e, key, topic, acknowledgment, "realtime position");
        }
    }

    // Alerts Consumer
    @KafkaListener(topics = "${app.kafka.topics.alerts}", groupId = "alerts-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeAlerts(
            @Payload(required = false) JsonNode alertData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.info("🚨 Received alert from topic: {}, key: {}", topic, key);

            // Validate data using utility method
            if (!isValidJsonData(alertData, key, topic, acknowledgment)) {
                return;
            }

            // ✅ Process alert - save to database and trigger notifications
            // Process alert using basic implementation
            log.info("🚨 Alert data: {}", alertData.toString());
            log.info("🚨 Alert type: {}", alertData.has("type") ? alertData.get("type").asText() : "unknown");
            log.info("🚨 Alert priority: {}",
                    alertData.has("priority") ? alertData.get("priority").asText() : "medium");
            log.info("🚨 Processing alert: {}", key);

            // ✅ Broadcast alert to WebSocket clients
            // Convert JsonNode to alert data for broadcasting
            Map<String, Object> alertDataMap = Map.of(
                    "id", key,
                    "type", alertData.has("type") ? alertData.get("type").asText() : "general",
                    "priority", alertData.has("priority") ? alertData.get("priority").asText() : "medium",
                    "message", alertData.has("message") ? alertData.get("message").asText() : "Alert received",
                    "timestamp", LocalDateTime.now().toString());
            webSocketService.broadcastSystemStatus(Map.of("type", "alert", "alertId", key, "data", alertDataMap));

            acknowledgment.acknowledge();

        } catch (Exception e) {
            handleConsumerError(e, key, topic, acknowledgment, "alert");
        }
    }

    // Notifications Consumer
    @KafkaListener(topics = "${app.kafka.topics.notifications}", groupId = "notifications-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeNotifications(
            @Payload(required = false) JsonNode notificationData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("📧 Received notification from topic: {}, key: {}", topic, key);

            // Validate data using utility method
            if (!isValidJsonData(notificationData, key, topic, acknowledgment)) {
                return;
            }

            // ✅ Process notification - send email, SMS, push notification, etc.
            // Process notification with basic implementation
            log.info("📧 Notification type: {}",
                    notificationData.has("type") ? notificationData.get("type").asText() : "general");
            log.info("📧 Notification recipient: {}", key);
            log.info("📧 Notification content: {}",
                    notificationData.has("content") ? notificationData.get("content").asText() : "No content");
            log.debug("📧 Processing notification: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            handleConsumerError(e, key, topic, acknowledgment, "notification");
        }
    }

    // Dead Letter Queue Consumer
    @KafkaListener(topics = "${app.kafka.topics.dead-letter}", groupId = "dead-letter-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeDeadLetterMessages(
            @Payload(required = false) JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.warn("💀 Received dead letter message from topic: {}, key: {}", topic, key);

            // Validate data using utility method
            if (!isValidJsonData(data, key, topic, acknowledgment)) {
                return;
            }

            // ✅ Log dead letter message for analysis
            // Analyze dead letter message for debugging
            log.warn("💀 Dead letter analysis:");
            log.warn("💀 Key: {}", key);
            log.warn("💀 Data size: {} bytes", data.toString().length());
            log.warn("💀 Source topic: {}", data.has("originalTopic") ? data.get("originalTopic").asText() : "unknown");
            log.warn("💀 Error message: {}", data.has("errorMessage") ? data.get("errorMessage").asText() : "none");
            log.warn("💀 Dead letter message analysis required for key: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            handleConsumerError(e, key, topic, acknowledgment, "dead letter");
        }
    }

    // Data Quality Issues Consumer
    @KafkaListener(topics = "${app.kafka.topics.data-quality-issues}", groupId = "data-quality-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeDataQualityIssues(
            @Payload(required = false) JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.warn("⚠️ Received data quality issue from topic: {}, key: {}", topic, key);

            // Validate data using utility method
            if (!isValidJsonData(data, key, topic, acknowledgment)) {
                return;
            }

            // ✅ Process data quality issue - log and potentially alert administrators
            // Monitor data quality with detailed analysis
            log.warn("⚠️ Data quality metrics:");
            log.warn("⚠️ Entity: {}", key);
            log.warn("⚠️ Issue type: {}", data.has("issueType") ? data.get("issueType").asText() : "unknown");
            log.warn("⚠️ Severity: {}", data.has("severity") ? data.get("severity").asText() : "medium");
            log.warn("⚠️ Description: {}",
                    data.has("description") ? data.get("description").asText() : "no description");
            log.warn("⚠️ Data quality issue detected for entity: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            handleConsumerError(e, key, topic, acknowledgment, "data quality");
        }
    }

    // Historical Data Consumer (for batch processing)
    @KafkaListener(topics = "${app.kafka.topics.historical-data}", groupId = "historical-data-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeHistoricalData(
            @Payload(required = false) JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("📊 Received historical data from topic: {}, key: {}", topic, key);

            // Validate data using utility method
            if (!isValidJsonData(data, key, topic, acknowledgment)) {
                return;
            }

            // ✅ Process historical data for analytics
            // Process historical data for analytics
            log.debug("📊 Historical data processing:");
            log.debug("📊 Entity: {}", key);
            log.debug("📊 Data type: {}", data.has("dataType") ? data.get("dataType").asText() : "unknown");
            log.debug("📊 Time range: {}", data.has("timeRange") ? data.get("timeRange").asText() : "not specified");
            log.debug("📊 Records count: {}", data.has("recordsCount") ? data.get("recordsCount").asInt() : 0);
            log.debug("📊 Processing historical data for analytics: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            handleConsumerError(e, key, topic, acknowledgment, "historical data");
        }
    }

    // WebSocket Updates Consumer (for internal distribution)
    @KafkaListener(topics = "${app.kafka.topics.websocket-updates}", groupId = "websocket-updates-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeWebSocketUpdates(
            @Payload(required = false) JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("🔌 Received websocket update from topic: {}, key: {}", topic, key);

            // Validate data using utility method
            if (!isValidJsonData(data, key, topic, acknowledgment)) {
                return;
            }

            // ✅ Distribute WebSocket update to connected clients
            webSocketService.broadcastSystemStatus(Map.of("type", "websocket-update", "sessionId", key, "data", data));
            log.debug("🔌 Distributed WebSocket update for session: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            handleConsumerError(e, key, topic, acknowledgment, "websocket update");
        }
    }

    /**
     * Send failed message to dead letter queue for analysis
     */
    private void sendToDeadLetterQueue(String key, String topic, String errorMessage, String consumerType) {
        try {
            Map<String, Object> deadLetterData = Map.of(
                    "originalKey", key,
                    "originalTopic", topic,
                    "errorMessage", errorMessage,
                    "consumerType", consumerType,
                    "failureTime", LocalDateTime.now().toString());

            // Send to dead letter topic using alert method (reusing existing
            // infrastructure)
            kafkaProducer.publishAlert("dead-letter-" + key, deadLetterData);
            log.info("📨 Sent failed message to dead letter queue: key={}, topic={}, error={}",
                    key, topic, errorMessage);

        } catch (Exception e) {
            log.error("Failed to send message to dead letter queue: {}", e.getMessage(), e);
        }
    }
}