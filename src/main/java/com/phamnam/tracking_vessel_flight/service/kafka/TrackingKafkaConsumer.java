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
    // Remove this line as TrackingDataProcessor doesn't exist
    // private final TrackingDataProcessor trackingDataProcessor;

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

            // Process raw aircraft data
            // TODO: Implement data processing
            // trackingDataProcessor.processRawAircraftData(key, data);

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

            // Process raw vessel data
            // TODO: Implement data processing
            // trackingDataProcessor.processRawVesselData(key, data);

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

            // Store processed aircraft data
            // TODO: Implement data processing
            // trackingDataProcessor.storeProcessedAircraftData(key, data);

            // Generate real-time position update
            // TODO: Implement realtime position generation
            // trackingDataProcessor.generateRealtimePosition(key, data, "AIRCRAFT");

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

            // Store processed vessel data
            // TODO: Implement data processing
            // trackingDataProcessor.storeProcessedVesselData(key, data);

            // Generate real-time position update
            // TODO: Implement realtime position generation
            // trackingDataProcessor.generateRealtimePosition(key, data, "VESSEL");

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
            // TODO: Implement WebSocket broadcasting
            // trackingDataProcessor.broadcastRealtimePosition(key, data);

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

            // Process alert
            // TODO: Implement alert processing
            // trackingDataProcessor.processAlert(key, alertData);

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

            // Process notification
            // TODO: Implement notification processing
            // trackingDataProcessor.processNotification(key, notificationData);

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
            // TODO: Implement dead letter message handling
            // trackingDataProcessor.handleDeadLetterMessage(key, data);

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

            // Process data quality issue
            // TODO: Implement data quality issue processing
            // trackingDataProcessor.processDataQualityIssue(key, data);

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
            // TODO: Implement historical data processing
            // trackingDataProcessor.processHistoricalData(key, data);

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

            // Distribute WebSocket update
            // TODO: Implement WebSocket update distribution
            // trackingDataProcessor.distributeWebSocketUpdate(key, data);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing websocket update with key: {}", key, e);
        }
    }
}