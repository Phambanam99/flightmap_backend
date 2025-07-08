package com.phamnam.tracking_vessel_flight.service.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DeadLetterQueueService {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueService.class);

    @Value("${app.kafka.topics.dead-letter}")
    private String deadLetterTopic;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Metrics for monitoring
    private final AtomicLong deadLetterMessageCount = new AtomicLong(0);
    private final AtomicLong totalErrorCount = new AtomicLong(0);

    /**
     * Send failed message to dead letter queue with error context
     */
    public void sendToDeadLetterQueue(ConsumerRecord<?, ?> failedRecord, Exception exception, String errorType) {
        try {
            // Create dead letter message with metadata
            Map<String, Object> deadLetterMessage = createDeadLetterMessage(failedRecord, exception, errorType);

            // Generate a unique key for the dead letter message
            String key = generateDeadLetterKey(failedRecord, errorType);

            // Send to dead letter topic
            kafkaTemplate.send(deadLetterTopic, key, deadLetterMessage)
                    .whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            deadLetterMessageCount.incrementAndGet();
                            logger.info("Successfully sent message to dead letter queue - Key: {}, Topic: {}",
                                    key, deadLetterTopic);
                        } else {
                            logger.error("Failed to send message to dead letter queue - Key: {}, Error: {}",
                                    key, throwable.getMessage(), throwable);
                        }
                    });

            totalErrorCount.incrementAndGet();

        } catch (Exception e) {
            logger.error(
                    "Critical error: Failed to process dead letter message for topic: {}, partition: {}, offset: {}",
                    failedRecord.topic(), failedRecord.partition(), failedRecord.offset(), e);
        }
    }

    /**
     * Create structured dead letter message with all context
     */
    private Map<String, Object> createDeadLetterMessage(ConsumerRecord<?, ?> failedRecord, Exception exception,
            String errorType) {
        Map<String, Object> deadLetterMessage = new HashMap<>();

        // Original message metadata
        deadLetterMessage.put("originalTopic", failedRecord.topic());
        deadLetterMessage.put("originalPartition", failedRecord.partition());
        deadLetterMessage.put("originalOffset", failedRecord.offset());
        deadLetterMessage.put("originalTimestamp", failedRecord.timestamp());
        deadLetterMessage.put("originalKey", failedRecord.key());

        // Try to preserve original value as string if possible
        Object originalValue = failedRecord.value();
        if (originalValue != null) {
            try {
                if (originalValue instanceof String) {
                    deadLetterMessage.put("originalValue", originalValue);
                } else {
                    deadLetterMessage.put("originalValue", objectMapper.writeValueAsString(originalValue));
                }
            } catch (Exception e) {
                deadLetterMessage.put("originalValue", originalValue.toString());
                deadLetterMessage.put("valueSerializationError", e.getMessage());
            }
        } else {
            deadLetterMessage.put("originalValue", null);
        }

        // Error information
        deadLetterMessage.put("errorType", errorType);
        deadLetterMessage.put("errorMessage", exception.getMessage());
        deadLetterMessage.put("errorClass", exception.getClass().getSimpleName());
        deadLetterMessage.put("errorTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Stack trace for debugging (truncated to avoid huge messages)
        String stackTrace = getStackTraceString(exception);
        deadLetterMessage.put("errorStackTrace",
                stackTrace.length() > 2000 ? stackTrace.substring(0, 2000) + "... [truncated]" : stackTrace);

        // Processing context
        deadLetterMessage.put("applicationName", "tracking-aircraft-aircraft");
        deadLetterMessage.put("processingAttempts", 1); // Can be enhanced with retry logic

        return deadLetterMessage;
    }

    /**
     * Generate unique key for dead letter message
     */
    private String generateDeadLetterKey(ConsumerRecord<?, ?> failedRecord, String errorType) {
        return String.format("dlq_%s_%d_%d_%s_%d",
                failedRecord.topic(),
                failedRecord.partition(),
                failedRecord.offset(),
                errorType,
                System.currentTimeMillis());
    }

    /**
     * Convert exception stack trace to string
     */
    private String getStackTraceString(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getSimpleName()).append(": ").append(exception.getMessage()).append("\n");

        StackTraceElement[] elements = exception.getStackTrace();
        int maxElements = Math.min(10, elements.length); // Limit stack trace length

        for (int i = 0; i < maxElements; i++) {
            sb.append("\tat ").append(elements[i].toString()).append("\n");
        }

        if (elements.length > maxElements) {
            sb.append("\t... ").append(elements.length - maxElements).append(" more\n");
        }

        // Include cause if present
        if (exception.getCause() != null) {
            sb.append("Caused by: ").append(exception.getCause().getClass().getSimpleName())
                    .append(": ").append(exception.getCause().getMessage());
        }

        return sb.toString();
    }

    /**
     * Get monitoring metrics
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("deadLetterMessageCount", deadLetterMessageCount.get());
        metrics.put("totalErrorCount", totalErrorCount.get());
        metrics.put("deadLetterTopic", deadLetterTopic);
        metrics.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return metrics;
    }

    /**
     * Reset metrics (for testing/admin purposes)
     */
    public void resetMetrics() {
        deadLetterMessageCount.set(0);
        totalErrorCount.set(0);
        logger.info("Dead letter queue metrics reset");
    }
}