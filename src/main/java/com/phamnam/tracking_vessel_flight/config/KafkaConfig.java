package com.phamnam.tracking_vessel_flight.config;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.ShipTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.service.kafka.DeadLetterQueueService;
import com.phamnam.tracking_vessel_flight.service.kafka.KafkaMonitoringService;
import com.phamnam.tracking_vessel_flight.service.kafka.DatabaseConstraintErrorHandler;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.phamnam.tracking_vessel_flight.util.LocalDateTimeArrayDeserializer;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableKafka
// @EnableKafkaStreams // Commented out - no stream topologies defined
public class KafkaConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topic-config.partitions:12}")
    private int defaultPartitions;

    @Value("${app.kafka.topic-config.replication-factor:1}")
    private short defaultReplicationFactor;

    @Value("${app.kafka.topic-config.retention-ms:604800000}")
    private String retentionMs;

    @Value("${app.kafka.topic-config.segment-ms:86400000}")
    private String segmentMs;

    @Value("${app.kafka.topic-config.cleanup-policy:delete}")
    private String cleanupPolicy;

    @Value("${app.kafka.topic-config.compression-type:snappy}")
    private String compressionType;

    // Topic names
    @Value("${app.kafka.topics.raw-aircraft-data}")
    private String rawAircraftDataTopic;

    @Value("${app.kafka.topics.raw-vessel-data}")
    private String rawVesselDataTopic;

    // Raw Data Topics by Source - New Implementation
    @Value("${app.kafka.topics.raw-flightradar24-data}")
    private String rawFlightRadar24DataTopic;

    @Value("${app.kafka.topics.raw-adsbexchange-data}")
    private String rawAdsbExchangeDataTopic;

    @Value("${app.kafka.topics.raw-marinetraffic-data}")
    private String rawMarineTrafficDataTopic;

    @Value("${app.kafka.topics.raw-vesselfinder-data}")
    private String rawVesselFinderDataTopic;

    @Value("${app.kafka.topics.raw-chinaports-data}")
    private String rawChinaportsDataTopic;

    @Value("${app.kafka.topics.raw-marinetrafficv2-data}")
    private String rawMarineTrafficV2DataTopic;

    @Value("${app.kafka.topics.processed-aircraft-data}")
    private String processedAircraftDataTopic;

    @Value("${app.kafka.topics.processed-vessel-data}")
    private String processedVesselDataTopic;

    @Value("${app.kafka.topics.aggregated-tracking-data}")
    private String aggregatedTrackingDataTopic;

    @Value("${app.kafka.topics.alerts}")
    private String alertsTopic;

    @Value("${app.kafka.topics.dead-letter}")
    private String deadLetterTopic;

    @Value("${app.kafka.topics.data-quality-issues}")
    private String dataQualityIssuesTopic;

    @Value("${app.kafka.topics.realtime-positions}")
    private String realtimePositionsTopic;

    @Value("${app.kafka.topics.historical-data}")
    private String historicalDataTopic;

    @Value("${app.kafka.topics.notifications}")
    private String notificationsTopic;

    @Value("${app.kafka.topics.websocket-updates}")
    private String websocketUpdatesTopic;

    // Legacy topic configurations for backward compatibility
    @Value("${app.kafka.aircraft-topic}")
    private String legacyAircraftTopic;

    @Value("${app.kafka.ship-topic}")
    private String legacyShipTopic;

    // Producer Configuration
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Autowired
    @Lazy
    private DeadLetterQueueService deadLetterQueueService;

    @Autowired
    @Lazy
    private KafkaMonitoringService kafkaMonitoringService;

    @Autowired
    private DatabaseConstraintErrorHandler databaseConstraintErrorHandler;

    // Add common error handler
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler((consumerRecord, exception) -> {
            // Log the error and the problematic record with more details
            Object value = consumerRecord.value();

            // Special handling for ListenerExecutionFailedException with null values -
            // Check FIRST
            if ("ListenerExecutionFailedException".equals(exception.getClass().getSimpleName()) && value == null) {
                log.info(
                        "🔄 Tombstone record detected for topic: {}, key: {}. Skipping error logging and DLQ processing.",
                        consumerRecord.topic(), consumerRecord.key());
                return; // Skip all further processing for ListenerExecutionFailedException with null
                        // values
            }

            String valueStr = value != null ? value.toString() : "null";

            // Enhanced error logging
            log.error("🚨 Kafka Error Handler - Failed to process record:");
            log.error(" Topic: {}", consumerRecord.topic());
            log.error(" Partition: {}", consumerRecord.partition());
            log.error(" Offset: {}", consumerRecord.offset());
            log.error(" Key: {}", consumerRecord.key());
            log.error(" Value: {}",
                    valueStr.length() > 500 ? valueStr.substring(0, 500) + "... [truncated]" : valueStr);
            log.error(" Error: {}", exception.getMessage());
            log.error(" Exception Type: {}", exception.getClass().getSimpleName());

            // Handle null values specifically - this indicates tombstone records or
            // serialization issues
            if (value == null) {
                log.warn(
                        "⚠️ Null value detected in Kafka message from topic: {}, partition: {}, offset: {}. This may be a tombstone record or misconfigured producer.",
                        consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
                // Don't send null values to DLQ, just acknowledge and skip
                return; // Skip further processing for null values
            }

            // Check for specific null-related exceptions or
            // ListenerExecutionFailedException
            if (exception.getMessage() != null &&
                    (exception.getMessage().toLowerCase().contains("null") ||
                            exception.getMessage().toLowerCase().contains("deserialization failed") ||
                            exception.getClass().getSimpleName().contains("ListenerExecutionFailedException"))) {
                log.warn("⚠️ Likely null/deserialization/listener issue for topic: {}, key: {}, exception: {}",
                        consumerRecord.topic(), consumerRecord.key(), exception.getMessage());
                // Skip DLQ for these cases and just acknowledge
                return;
            }

            // Determine error type for categorization
            String errorType = determineErrorType(exception);

            // Record error for monitoring and pattern detection
            try {
                kafkaMonitoringService.recordKafkaError(consumerRecord.topic(), errorType, exception);
            } catch (Exception monitoringException) {
                log.warn("Failed to record error in monitoring service: {}", monitoringException.getMessage());
            }

            // Check if this is a database constraint violation
            boolean isConstraintViolation = databaseConstraintErrorHandler.isConstraintViolation(exception);

            if (isConstraintViolation) {
                // Handle constraint violation (entity already exists) - use DEBUG level to
                // reduce noise
                databaseConstraintErrorHandler.handleConstraintViolation(exception,
                        String.valueOf(consumerRecord.key()), consumerRecord.topic());
                log.debug("🔄 Constraint violation handled for topic: {}, key: {} - entity already exists (IMO: {})",
                        consumerRecord.topic(), consumerRecord.key(), extractImoFromException(exception));
                return; // Skip DLQ and further processing for constraint violations
            } else {
                // Send to dead letter queue only for non-constraint violations
                try {
                    deadLetterQueueService.sendToDeadLetterQueue(consumerRecord, exception, errorType);
                    log.info("Message sent to dead letter queue successfully for topic: {}, partition: {}, offset: {}",
                            consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
                } catch (Exception dlqException) {
                    log.error(
                            "CRITICAL: Failed to send message to dead letter queue! Original error: {}, DLQ error: {}",
                            exception.getMessage(), dlqException.getMessage(), dlqException);
                }
            }

        }, new FixedBackOff(1000L, 2)); // Retry 2 times with 1 second delay
    }

    /**
     * Determine error type for better categorization in dead letter queue
     */
    private String determineErrorType(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName();
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";

        // Check for database constraint violations first
        if (databaseConstraintErrorHandler.isConstraintViolation(exception)) {
            return "DATABASE_CONSTRAINT_VIOLATION";
        } else if (exceptionName.contains("DataIntegrity") || message.contains("constraint")) {
            return "DATABASE_INTEGRITY_ERROR";
        } else if (exceptionName.contains("Deserialization") || message.contains("deserialization")) {
            return "DESERIALIZATION_ERROR";
        } else if (exceptionName.contains("Json") || message.contains("json")) {
            return "JSON_PARSE_ERROR";
        } else if (exceptionName.contains("ClassCast") || message.contains("class cast")) {
            return "TYPE_MISMATCH_ERROR";
        } else if (exceptionName.contains("Timeout") || message.contains("timeout")) {
            return "TIMEOUT_ERROR";
        } else if (exceptionName.contains("Connection") || message.contains("connection")) {
            return "CONNECTION_ERROR";
        } else {
            return "UNKNOWN_ERROR";
        }
    }

    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Configure basic consumer properties
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Create JsonDeserializer with our custom ObjectMapper
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(kafkaObjectMapper());
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        // Wrap with error handling
        ErrorHandlingDeserializer<Object> errorHandlingDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandlingDeserializer);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // Create consumer factory with custom ObjectMapper
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Create custom deserializer with custom ObjectMapper
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(kafkaObjectMapper());
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(4); // Number of consumer threads
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // Flight tracking specific container factory
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, FlightTrackingRequestDTO>> flightKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FlightTrackingRequestDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // Create consumer factory for FlightTrackingRequestDTO
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Create custom deserializer with custom ObjectMapper
        JsonDeserializer<FlightTrackingRequestDTO> jsonDeserializer = new JsonDeserializer<>(kafkaObjectMapper());
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, FlightTrackingRequestDTO> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // Batch flight tracking container factory
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, List<FlightTrackingRequestDTO>>> batchFlightKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, List<FlightTrackingRequestDTO>> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // Create consumer factory for batch processing
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Use ErrorHandlingDeserializer
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // Configure delegate deserializers
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put("spring.json.use.type.headers", false);
        props.put("spring.json.add.type.headers", false);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // Batch size
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        DefaultKafkaConsumerFactory<String, List<FlightTrackingRequestDTO>> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props);
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1); // Single thread for batch processing
        factory.getContainerProperties().setPollTimeout(5000);
        factory.setBatchListener(true); // Enable batch processing
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // Ship tracking specific container factory
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ShipTrackingRequest>> shipKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ShipTrackingRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // Create consumer factory for ShipTrackingRequest with custom ObjectMapper
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Create JsonDeserializer with our custom ObjectMapper for ShipTrackingRequest
        JsonDeserializer<ShipTrackingRequest> jsonDeserializer = new JsonDeserializer<>(kafkaObjectMapper());
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        // Wrap with error handling
        ErrorHandlingDeserializer<ShipTrackingRequest> errorHandlingDeserializer = new ErrorHandlingDeserializer<>(
                jsonDeserializer);

        DefaultKafkaConsumerFactory<String, ShipTrackingRequest> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), errorHandlingDeserializer);
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // Raw Aircraft Data container factory
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, AircraftTrackingRequest>> rawAircraftKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AircraftTrackingRequest> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // Create consumer factory for AircraftTrackingRequest
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Create custom deserializer with custom ObjectMapper
        JsonDeserializer<AircraftTrackingRequest> jsonDeserializer = new JsonDeserializer<>(kafkaObjectMapper());
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, AircraftTrackingRequest> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // Raw Vessel Data container factory
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ShipTrackingRequestDTO>> rawVesselKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ShipTrackingRequestDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // Create consumer factory for ShipTrackingRequestDTO (for raw vessel data)
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Create custom deserializer with custom ObjectMapper
        JsonDeserializer<ShipTrackingRequestDTO> jsonDeserializer = new JsonDeserializer<>(kafkaObjectMapper());
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, ShipTrackingRequestDTO> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // Processed Aircraft Data container factory (FlightTracking model)
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, FlightTracking>> processedAircraftKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FlightTracking> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // Create consumer factory for FlightTracking
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Create custom deserializer with custom ObjectMapper
        JsonDeserializer<FlightTracking> jsonDeserializer = new JsonDeserializer<>(kafkaObjectMapper());
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, FlightTracking> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(2);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // Batch ship tracking container factory
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, List<ShipTrackingRequest>>> batchShipKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, List<ShipTrackingRequest>> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // Create consumer factory for batch ship tracking
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // Batch size
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 1000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Create custom deserializer with custom ObjectMapper
        JsonDeserializer<List<ShipTrackingRequest>> jsonDeserializer = new JsonDeserializer<>(kafkaObjectMapper());
        jsonDeserializer.setUseTypeHeaders(false);
        jsonDeserializer.addTrustedPackages("*");

        DefaultKafkaConsumerFactory<String, List<ShipTrackingRequest>> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new ErrorHandlingDeserializer<>(jsonDeserializer));
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1); // Single thread for batch processing
        factory.getContainerProperties().setPollTimeout(5000);
        factory.setBatchListener(true); // Enable batch processing
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // Admin Client for Topic Management
    @Bean
    public AdminClient kafkaAdminClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return AdminClient.create(configs);
    }

    // Topic Definitions
    @Bean
    public NewTopic rawAircraftDataTopic() {
        return createTopic(rawAircraftDataTopic, "Raw aircraft tracking data from external APIs");
    }

    @Bean
    public NewTopic rawVesselDataTopic() {
        return createTopic(rawVesselDataTopic, "Raw vessel tracking data from external APIs");
    }

    // Raw Data Topics by Source - New Implementation
    @Bean
    public NewTopic rawFlightRadar24DataTopic() {
        return createTopic(rawFlightRadar24DataTopic, "Raw aircraft data from FlightRadar24 API");
    }

    @Bean
    public NewTopic rawAdsbExchangeDataTopic() {
        return createTopic(rawAdsbExchangeDataTopic, "Raw aircraft data from ADS-B Exchange API");
    }

    @Bean
    public NewTopic rawMarineTrafficDataTopic() {
        return createTopic(rawMarineTrafficDataTopic, "Raw vessel data from MarineTraffic API");
    }

    @Bean
    public NewTopic rawVesselFinderDataTopic() {
        return createTopic(rawVesselFinderDataTopic, "Raw vessel data from VesselFinder API");
    }

    @Bean
    public NewTopic rawChinaportsDataTopic() {
        return createTopic(rawChinaportsDataTopic, "Raw vessel data from Chinaports API");
    }

    @Bean
    public NewTopic rawMarineTrafficV2DataTopic() {
        return createTopic(rawMarineTrafficV2DataTopic, "Raw vessel data from MarineTraffic V2 API");
    }

    @Bean
    public NewTopic processedAircraftDataTopic() {
        return createTopic(processedAircraftDataTopic, "Processed and validated aircraft tracking data");
    }

    @Bean
    public NewTopic processedVesselDataTopic() {
        return createTopic(processedVesselDataTopic, "Processed and validated vessel tracking data");
    }

    @Bean
    public NewTopic aggregatedTrackingDataTopic() {
        return createTopic(aggregatedTrackingDataTopic, "Aggregated tracking data for analytics");
    }

    @Bean
    public NewTopic alertsTopic() {
        return createTopic(alertsTopic, "Alert events and notifications");
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return createTopic(deadLetterTopic, "Failed messages for debugging");
    }

    @Bean
    public NewTopic dataQualityIssuesTopic() {
        return createTopic(dataQualityIssuesTopic, "Data quality issues and anomalies");
    }

    @Bean
    public NewTopic realtimePositionsTopic() {
        return createTopic(realtimePositionsTopic, "Current positions for real-time display", 24);
    }

    @Bean
    public NewTopic historicalDataTopic() {
        return createHighRetentionTopic(historicalDataTopic, "Historical tracking data for analysis");
    }

    @Bean
    public NewTopic notificationsTopic() {
        return createTopic(notificationsTopic, "User notifications and alerts");
    }

    @Bean
    public NewTopic websocketUpdatesTopic() {
        return createTopic(websocketUpdatesTopic, "Real-time updates for WebSocket clients", 24);
    }

    // Helper methods for topic creation
    private NewTopic createTopic(String name, String description) {
        return createTopic(name, description, defaultPartitions);
    }

    private NewTopic createTopic(String name, String description, int partitions) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(defaultReplicationFactor)
                .config(TopicConfig.RETENTION_MS_CONFIG, retentionMs)
                .config(TopicConfig.SEGMENT_MS_CONFIG, segmentMs)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy)
                .config(TopicConfig.COMPRESSION_TYPE_CONFIG, compressionType)
                .config(TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG, "false")
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }

    private NewTopic createHighRetentionTopic(String name, String description) {
        return TopicBuilder.name(name)
                .partitions(defaultPartitions)
                .replicas(defaultReplicationFactor)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 days
                .config(TopicConfig.SEGMENT_MS_CONFIG, segmentMs)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy)
                .config(TopicConfig.COMPRESSION_TYPE_CONFIG, compressionType)
                .config(TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG, "false")
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }

    // Topic name beans for easy injection
    @Bean("rawAircraftDataTopicName")
    public String rawAircraftDataTopicName() {
        return rawAircraftDataTopic;
    }

    @Bean("rawVesselDataTopicName")
    public String rawVesselDataTopicName() {
        return rawVesselDataTopic;
    }

    // Raw Data Topic Names by Source - New Implementation
    @Bean("rawFlightRadar24DataTopicName")
    public String rawFlightRadar24DataTopicName() {
        return rawFlightRadar24DataTopic;
    }

    @Bean("rawAdsbExchangeDataTopicName")
    public String rawAdsbExchangeDataTopicName() {
        return rawAdsbExchangeDataTopic;
    }

    @Bean("rawMarineTrafficDataTopicName")
    public String rawMarineTrafficDataTopicName() {
        return rawMarineTrafficDataTopic;
    }

    @Bean("rawVesselFinderDataTopicName")
    public String rawVesselFinderDataTopicName() {
        return rawVesselFinderDataTopic;
    }

    @Bean("rawChinaportsDataTopicName")
    public String rawChinaportsDataTopicName() {
        return rawChinaportsDataTopic;
    }

    @Bean("rawMarineTrafficV2DataTopicName")
    public String rawMarineTrafficV2DataTopicName() {
        return rawMarineTrafficV2DataTopic;
    }

    @Bean("processedAircraftDataTopicName")
    public String processedAircraftDataTopicName() {
        return processedAircraftDataTopic;
    }

    @Bean("processedVesselDataTopicName")
    public String processedVesselDataTopicName() {
        return processedVesselDataTopic;
    }

    @Bean("aggregatedTrackingDataTopicName")
    public String aggregatedTrackingDataTopicName() {
        return aggregatedTrackingDataTopic;
    }

    @Bean("alertsTopicName")
    public String alertsTopicName() {
        return alertsTopic;
    }

    @Bean("deadLetterTopicName")
    public String deadLetterTopicName() {
        return deadLetterTopic;
    }

    @Bean("dataQualityIssuesTopicName")
    public String dataQualityIssuesTopicName() {
        return dataQualityIssuesTopic;
    }

    @Bean("realtimePositionsTopicName")
    public String realtimePositionsTopicName() {
        return realtimePositionsTopic;
    }

    @Bean("historicalDataTopicName")
    public String historicalDataTopicName() {
        return historicalDataTopic;
    }

    @Bean("notificationsTopicName")
    public String notificationsTopicName() {
        return notificationsTopic;
    }

    @Bean("websocketUpdatesTopicName")
    public String websocketUpdatesTopicName() {
        return websocketUpdatesTopic;
    }

    // Legacy topic name beans for backward compatibility
    @Bean("legacyAircraftTopicName")
    public String legacyAircraftTopicName() {
        return legacyAircraftTopic;
    }

    @Bean("legacyShipTopicName")
    public String legacyShipTopicName() {
        return legacyShipTopic;
    }

    /**
     * Global ObjectMapper for Kafka JSON deserialization with LocalDateTime array
     * support
     */
    @Bean
    public ObjectMapper kafkaObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Create custom module for LocalDateTime array deserialization
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeArrayDeserializer());
        mapper.registerModule(module);

        return mapper;
    }

    /**
     * Extract IMO number from database constraint exception message
     */
    private String extractImoFromException(Exception exception) {
        if (exception.getMessage() != null && exception.getMessage().contains("Key (imo)=(")) {
            try {
                String message = exception.getMessage();
                int start = message.indexOf("Key (imo)=(") + "Key (imo)=(".length();
                int end = message.indexOf(")", start);
                if (start > 0 && end > start) {
                    return message.substring(start, end);
                }
            } catch (Exception e) {
                // Ignore extraction errors
            }
        }
        return "unknown";
    }
}