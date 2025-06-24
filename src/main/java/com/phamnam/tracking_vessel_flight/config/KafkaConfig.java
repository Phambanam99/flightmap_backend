package com.phamnam.tracking_vessel_flight.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.ShipTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableKafka
// @EnableKafkaStreams // Commented out - no stream topologies defined
public class KafkaConfig {

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

    // Add common error handler
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler((consumerRecord, exception) -> {
            // Log the error and the problematic record with more details
            Object value = consumerRecord.value();
            String valueStr = value != null ? value.toString() : "null";
//
//            System.err.println("🚨 Kafka Error Handler - Failed to process record:");
//            System.err.println("  Topic: " + consumerRecord.topic());
//            System.err.println("  Partition: " + consumerRecord.partition());
//            System.err.println("  Offset: " + consumerRecord.offset());
//            System.err.println("  Key: " + consumerRecord.key());
//            System.err.println("  Value: " + valueStr);
//            System.err.println("  Error: " + exception.getMessage());
//            System.err.println("  Exception Type: " + exception.getClass().getSimpleName());

            // TODO: Send to dead letter queue for further analysis
            // This should be implemented for production systems

        }, new FixedBackOff(1000L, 2)); // Retry 2 times with 1 second delay
    }

    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
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
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
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

        // Use ErrorHandlingDeserializer
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // Configure delegate deserializers
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO");
        props.put("spring.json.use.type.headers", false);
        props.put("spring.json.add.type.headers", false);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        DefaultKafkaConsumerFactory<String, FlightTrackingRequestDTO> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props);
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

    // Ship tracking DTO container factory
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ShipTrackingRequestDTO>> shipKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ShipTrackingRequestDTO> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // Create consumer factory for ShipTrackingRequestDTO
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
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.phamnam.tracking_vessel_flight.dto.ShipTrackingRequestDTO");
        props.put("spring.json.use.type.headers", false);
        props.put("spring.json.add.type.headers", false);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        DefaultKafkaConsumerFactory<String, ShipTrackingRequestDTO> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props);
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

        // Use ErrorHandlingDeserializer
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // Configure delegate deserializers
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest");
        props.put("spring.json.use.type.headers", false);
        props.put("spring.json.add.type.headers", false);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        DefaultKafkaConsumerFactory<String, AircraftTrackingRequest> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props);
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

        // Use ErrorHandlingDeserializer
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // Configure delegate deserializers
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.phamnam.tracking_vessel_flight.dto.ShipTrackingRequestDTO");
        props.put("spring.json.use.type.headers", false);
        props.put("spring.json.add.type.headers", false);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        DefaultKafkaConsumerFactory<String, ShipTrackingRequestDTO> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props);
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

        // Use ErrorHandlingDeserializer
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // Configure delegate deserializers
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        // JsonDeserializer configuration
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.phamnam.tracking_vessel_flight.models.FlightTracking");
        props.put("spring.json.use.type.headers", false);
        props.put("spring.json.add.type.headers", false);

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        DefaultKafkaConsumerFactory<String, FlightTracking> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props);
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

        DefaultKafkaConsumerFactory<String, List<ShipTrackingRequest>> consumerFactory = new DefaultKafkaConsumerFactory<>(
                props);
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
}