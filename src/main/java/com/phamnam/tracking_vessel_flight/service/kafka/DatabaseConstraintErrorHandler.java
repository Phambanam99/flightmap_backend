package com.phamnam.tracking_vessel_flight.service.kafka;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatabaseConstraintErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConstraintErrorHandler.class);

    // Pattern to extract constraint name and key from PostgreSQL error messages
    private static final Pattern CONSTRAINT_PATTERN = Pattern.compile("Key \\((\\w+)\\)=\\(([^)]+)\\) already exists");
    private static final Pattern CONSTRAINT_NAME_PATTERN = Pattern.compile("violates unique constraint \"([^\"]+)\"");

    /**
     * Check if an exception is a database constraint violation
     */
    public boolean isConstraintViolation(Exception exception) {
        if (exception instanceof DataIntegrityViolationException) {
            return true;
        }

        if (exception instanceof ConstraintViolationException) {
            return true;
        }

        // Check nested causes
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof DataIntegrityViolationException ||
                    cause instanceof ConstraintViolationException ||
                    cause instanceof SQLException) {
                String message = cause.getMessage();
                if (message != null && (message.contains("unique constraint") ||
                        message.contains("duplicate key") ||
                        message.contains("already exists"))) {
                    return true;
                }
            }
            cause = cause.getCause();
        }

        return false;
    }

    /**
     * Extract constraint violation details for logging and handling
     */
    public ConstraintViolationDetails extractViolationDetails(Exception exception) {
        String errorMessage = getFullErrorMessage(exception);

        // Extract constraint name
        String constraintName = extractConstraintName(errorMessage);

        // Extract violated key and value
        String keyColumn = null;
        String keyValue = null;

        Matcher matcher = CONSTRAINT_PATTERN.matcher(errorMessage);
        if (matcher.find()) {
            keyColumn = matcher.group(1);
            keyValue = matcher.group(2);
        }

        // Determine entity type based on constraint name or error message
        String entityType = determineEntityType(constraintName, errorMessage);

        return new ConstraintViolationDetails(
                constraintName,
                keyColumn,
                keyValue,
                entityType,
                errorMessage);
    }

    /**
     * Handle constraint violation with appropriate strategy
     */
    public void handleConstraintViolation(Exception exception, String kafkaKey, String topic) {
        try {
            ConstraintViolationDetails details = extractViolationDetails(exception);

            logger.warn("🔄 Database Constraint Violation Detected:");
            logger.warn("🔄 Kafka Topic: {}", topic);
            logger.warn("🔄 Kafka Key: {}", kafkaKey);
            logger.warn("🔄 Entity Type: {}", details.getEntityType());
            logger.warn("🔄 Constraint: {}", details.getConstraintName());
            logger.warn("🔄 Key Column: {}", details.getKeyColumn());
            logger.warn("🔄 Key Value: {}", details.getKeyValue());
            logger.warn("🔄 Strategy: IGNORE_DUPLICATE (entity already exists)");

            // Log suggestion for handling
            if ("aircraft".equals(details.getEntityType()) && "hexident".equals(details.getKeyColumn())) {
                logger.info(
                        "💡 Suggestion: Aircraft with hexident '{}' already exists. Consider implementing upsert logic.",
                        details.getKeyValue());
            } else if ("ship".equals(details.getEntityType()) && "mmsi".equals(details.getKeyColumn())) {
                logger.info("💡 Suggestion: Ship with MMSI '{}' already exists. Consider implementing upsert logic.",
                        details.getKeyValue());
            }

        } catch (Exception e) {
            logger.error("Error handling constraint violation: {}", e.getMessage());
        }
    }

    /**
     * Determine if message should be sent to dead letter queue
     * For constraint violations, we usually don't want to send to DLQ as they
     * represent existing data
     */
    public boolean shouldSendToDeadLetterQueue(Exception exception) {
        if (isConstraintViolation(exception)) {
            // For constraint violations, don't send to DLQ as the entity already exists
            return false;
        }

        // For other exceptions, send to DLQ for investigation
        return true;
    }

    private String getFullErrorMessage(Exception exception) {
        StringBuilder sb = new StringBuilder();

        if (exception.getMessage() != null) {
            sb.append(exception.getMessage());
        }

        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause.getMessage() != null) {
                sb.append(" | ").append(cause.getMessage());
            }
            cause = cause.getCause();
        }

        return sb.toString();
    }

    private String extractConstraintName(String errorMessage) {
        Matcher matcher = CONSTRAINT_NAME_PATTERN.matcher(errorMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown_constraint";
    }

    private String determineEntityType(String constraintName, String errorMessage) {
        String lowerMessage = errorMessage.toLowerCase();
        String lowerConstraint = constraintName.toLowerCase();

        if (lowerConstraint.contains("aircraft") || lowerMessage.contains("aircraft") ||
                lowerConstraint.contains("hexident") || lowerMessage.contains("hexident")) {
            return "aircraft";
        } else if (lowerConstraint.contains("ship") || lowerMessage.contains("ship") ||
                lowerConstraint.contains("mmsi") || lowerMessage.contains("mmsi")) {
            return "ship";
        } else if (lowerConstraint.contains("flight") || lowerMessage.contains("flight")) {
            return "flight";
        } else if (lowerConstraint.contains("voyage") || lowerMessage.contains("voyage")) {
            return "voyage";
        }

        return "unknown";
    }

    /**
     * Data class to hold constraint violation details
     */
    public static class ConstraintViolationDetails {
        private final String constraintName;
        private final String keyColumn;
        private final String keyValue;
        private final String entityType;
        private final String errorMessage;

        public ConstraintViolationDetails(String constraintName, String keyColumn, String keyValue,
                String entityType, String errorMessage) {
            this.constraintName = constraintName;
            this.keyColumn = keyColumn;
            this.keyValue = keyValue;
            this.entityType = entityType;
            this.errorMessage = errorMessage;
        }

        // Getters
        public String getConstraintName() {
            return constraintName;
        }

        public String getKeyColumn() {
            return keyColumn;
        }

        public String getKeyValue() {
            return keyValue;
        }

        public String getEntityType() {
            return entityType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return String.format("ConstraintViolation{type=%s, constraint=%s, key=%s=%s}",
                    entityType, constraintName, keyColumn, keyValue);
        }
    }
}