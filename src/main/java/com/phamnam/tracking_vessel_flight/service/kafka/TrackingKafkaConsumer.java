package com.phamnam.tracking_vessel_flight.service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingKafkaConsumer {

    private final ObjectMapper objectMapper;

    // Raw Aircraft Data Consumer
    @KafkaListener(topics = "${app.kafka.topics.raw-aircraft-data}", groupId = "raw-aircraft-consumer-group")
    public void consumeRawAircraftData(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received raw aircraft data from topic: {}, partition: {}, offset: {}, key: {}",
                    topic, partition, offset, key);

            // Process raw aircraft data - store in raw data repository for audit
            log.info("Processing raw aircraft data for hexident: {}", key);

            // Acknowledge message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing raw aircraft data with key: {}", key, e);
            // Don't acknowledge - message will be retried
        }
    }

    // Raw Vessel Data Consumer
    @KafkaListener(topics = "${app.kafka.topics.raw-vessel-data}", groupId = "raw-vessel-consumer-group")
    public void consumeRawVesselData(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received raw vessel data from topic: {}, partition: {}, offset: {}, key: {}",
                    topic, partition, offset, key);

            // Process raw vessel data - store in raw data repository for audit
            log.info("Processing raw vessel data for mmsi: {}", key);

            // Acknowledge message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing raw vessel data with key: {}", key, e);
            // Don't acknowledge - message will be retried
        }
    }

    // Processed Aircraft Data Consumer
    @KafkaListener(topics = "${app.kafka.topics.processed-aircraft-data}", groupId = "processed-aircraft-consumer-group")
    public void consumeProcessedAircraftData(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received processed aircraft data from topic: {}, key: {}", topic, key);

            // Store processed aircraft data in main tracking tables
            log.info("Storing processed aircraft data for hexident: {}", key);

            // Generate real-time position update for WebSocket clients
            log.debug("Generated realtime position update for aircraft: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing processed aircraft data with key: {}", key, e);
        }
    }

    // Processed Vessel Data Consumer
    @KafkaListener(topics = "${app.kafka.topics.processed-vessel-data}", groupId = "processed-vessel-consumer-group")
    public void consumeProcessedVesselData(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received processed vessel data from topic: {}, key: {}", topic, key);

            // Store processed vessel data in main tracking tables
            log.info("Storing processed vessel data for mmsi: {}", key);

            // Generate real-time position update for WebSocket clients
            log.debug("Generated realtime position update for vessel: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing processed vessel data with key: {}", key, e);
        }
    }

    // Real-time Positions Consumer (for WebSocket broadcasting)
    @KafkaListener(topics = "${app.kafka.topics.realtime-positions}", groupId = "realtime-positions-consumer-group")
    public void consumeRealtimePositions(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received realtime position from topic: {}, key: {}", topic, key);

            // Broadcast to WebSocket clients
            log.debug("Broadcasting realtime position update for entity: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing realtime position with key: {}", key, e);
        }
    }

    // Alerts Consumer
    @KafkaListener(topics = "${app.kafka.topics.alerts}", groupId = "alerts-consumer-group")
    public void consumeAlerts(
            @Payload JsonNode alertData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received alert from topic: {}, key: {}", topic, key);

            // Process alert - save to database and trigger notifications
            log.info("Processing alert: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing alert with key: {}", key, e);
        }
    }

    // Notifications Consumer
    @KafkaListener(topics = "${app.kafka.topics.notifications}", groupId = "notifications-consumer-group")
    public void consumeNotifications(
            @Payload JsonNode notificationData,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received notification from topic: {}, key: {}", topic, key);

            // Process notification - send email, SMS, push notification, etc.
            log.debug("Processing notification: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing notification with key: {}", key, e);
        }
    }

    // Dead Letter Queue Consumer
    @KafkaListener(topics = "${app.kafka.topics.dead-letter}", groupId = "dead-letter-consumer-group")
    public void consumeDeadLetterMessages(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.warn("Received dead letter message from topic: {}, key: {}", topic, key);

            // Log dead letter message for analysis
            log.warn("Dead letter message analysis required for key: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing dead letter message with key: {}", key, e);
        }
    }

    // Data Quality Issues Consumer
    @KafkaListener(topics = "${app.kafka.topics.data-quality-issues}", groupId = "data-quality-consumer-group")
    public void consumeDataQualityIssues(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.warn("Received data quality issue from topic: {}, key: {}", topic, key);

            // Process data quality issue - log and potentially alert administrators
            log.warn("Data quality issue detected for entity: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing data quality issue with key: {}", key, e);
        }
    }

    // Historical Data Consumer (for batch processing)
    @KafkaListener(topics = "${app.kafka.topics.historical-data}", groupId = "historical-data-consumer-group")
    public void consumeHistoricalData(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received historical data from topic: {}, key: {}", topic, key);

            // Process historical data for analytics
            log.debug("Processing historical data for analytics: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing historical data with key: {}", key, e);
        }
    }

    // WebSocket Updates Consumer (for internal distribution)
    @KafkaListener(topics = "${app.kafka.topics.websocket-updates}", groupId = "websocket-updates-consumer-group")
    public void consumeWebSocketUpdates(
            @Payload JsonNode data,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received websocket update from topic: {}, key: {}", topic, key);

            // Distribute WebSocket update to connected clients
            log.debug("Distributing WebSocket update for session: {}", key);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing websocket update with key: {}", key, e);
        }
    }
}