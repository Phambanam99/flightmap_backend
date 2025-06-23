package com.phamnam.tracking_vessel_flight.service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.ShipTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
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

    @Value("${raw.data.storage.enabled:false}")
    private boolean rawStorageEnabled;

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
            FlightTracking savedTracking = flightTrackingService.processNewTrackingData(trackingRequest, null);
            log.info("✅ Processed aircraft data through service for hexident: {}", key);

            // ✅ Send real-time update to WebSocket clients
            webSocketService.broadcastAircraftUpdate(savedTracking);
            log.debug("📡 Broadcasted aircraft position update for: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing processed aircraft data with key: {}", key, e);
        }
    }

    // Processed Vessel Data Consumer
    @KafkaListener(topics = "${app.kafka.topics.processed-vessel-data}", groupId = "processed-vessel-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeProcessedVesselData(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received processed vessel data from topic: {}, key: {}", topic, key);

            // Convert JsonNode to ShipTrackingRequestDTO for service processing
            ShipTrackingRequestDTO trackingRequest = objectMapper.treeToValue(data, ShipTrackingRequestDTO.class);

            // ✅ Process through service to create Ship and Voyage entities
            // Note: This method should be implemented in ShipTrackingService
            log.info("✅ Processing vessel data for mmsi: {} through service", key);

            // For now, create a basic ShipTracking entity and broadcast via WebSocket
            // TODO: Implement full vessel processing through ShipTrackingService
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
            log.error("❌ Error processing processed vessel data with key: {}", key, e);
        }
    }

    // Real-time Positions Consumer (for WebSocket broadcasting)
    @KafkaListener(topics = "${app.kafka.topics.realtime-positions}", groupId = "realtime-positions-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeRealtimePositions(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received realtime position from topic: {}, key: {}", topic, key);

            // ✅ Broadcast to all WebSocket clients
            webSocketService.broadcastSystemStatus(Map.of("type", "position-update", "entityId", key, "data", data));
            log.debug("📡 Broadcasted realtime position for entity: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing realtime position with key: {}", key, e);
        }
    }

    // Alerts Consumer
    @KafkaListener(topics = "${app.kafka.topics.alerts}", groupId = "alerts-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeAlerts(
            @Payload JsonNode alertData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.info("🚨 Received alert from topic: {}, key: {}", topic, key);

            // ✅ Process alert - save to database and trigger notifications
            // TODO: Implement alert processing service
            log.info("🚨 Processing alert: {}", key);

            // ✅ Broadcast alert to WebSocket clients
            // TODO: Convert JsonNode to AlertEvent entity for proper alert broadcasting
            webSocketService.broadcastSystemStatus(Map.of("type", "alert", "alertId", key, "data", alertData));

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing alert with key: {}", key, e);
        }
    }

    // Notifications Consumer
    @KafkaListener(topics = "${app.kafka.topics.notifications}", groupId = "notifications-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeNotifications(
            @Payload JsonNode notificationData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("📧 Received notification from topic: {}, key: {}", topic, key);

            // ✅ Process notification - send email, SMS, push notification, etc.
            // TODO: Implement notification service
            log.debug("📧 Processing notification: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing notification with key: {}", key, e);
        }
    }

    // Dead Letter Queue Consumer
    @KafkaListener(topics = "${app.kafka.topics.dead-letter}", groupId = "dead-letter-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeDeadLetterMessages(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.warn("💀 Received dead letter message from topic: {}, key: {}", topic, key);

            // ✅ Log dead letter message for analysis
            // TODO: Implement dead letter analysis service
            log.warn("💀 Dead letter message analysis required for key: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing dead letter message with key: {}", key, e);
        }
    }

    // Data Quality Issues Consumer
    @KafkaListener(topics = "${app.kafka.topics.data-quality-issues}", groupId = "data-quality-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeDataQualityIssues(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.warn("⚠️ Received data quality issue from topic: {}, key: {}", topic, key);

            // ✅ Process data quality issue - log and potentially alert administrators
            // TODO: Implement data quality monitoring service
            log.warn("⚠️ Data quality issue detected for entity: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing data quality issue with key: {}", key, e);
        }
    }

    // Historical Data Consumer (for batch processing)
    @KafkaListener(topics = "${app.kafka.topics.historical-data}", groupId = "historical-data-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeHistoricalData(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("📊 Received historical data from topic: {}, key: {}", topic, key);

            // ✅ Process historical data for analytics
            // TODO: Implement analytics service
            log.debug("📊 Processing historical data for analytics: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing historical data with key: {}", key, e);
        }
    }

    // WebSocket Updates Consumer (for internal distribution)
    @KafkaListener(topics = "${app.kafka.topics.websocket-updates}", groupId = "websocket-updates-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeWebSocketUpdates(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("🔌 Received websocket update from topic: {}, key: {}", topic, key);

            // ✅ Distribute WebSocket update to connected clients
            webSocketService.broadcastSystemStatus(Map.of("type", "websocket-update", "sessionId", key, "data", data));
            log.debug("🔌 Distributed WebSocket update for session: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("❌ Error processing websocket update with key: {}", key, e);
        }
    }
}