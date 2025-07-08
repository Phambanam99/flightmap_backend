package com.phamnam.tracking_vessel_flight.service.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KafkaMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMonitoringService.class);

    @Autowired
    private DeadLetterQueueService deadLetterQueueService;

    // Error tracking maps
    private final Map<String, AtomicLong> errorsByTopic = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastErrorByTopic = new ConcurrentHashMap<>();
    private final List<String> recentErrors = Collections.synchronizedList(new ArrayList<>());

    // System metrics
    private final AtomicLong totalKafkaErrors = new AtomicLong(0);
    private LocalDateTime lastErrorTime;
    private static final int MAX_RECENT_ERRORS = 100;

    /**
     * Record a Kafka error for monitoring
     */
    public void recordKafkaError(String topic, String errorType, Exception exception) {
        try {
            // Update counters
            errorsByTopic.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();
            errorsByType.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
            lastErrorByTopic.put(topic, LocalDateTime.now());
            totalKafkaErrors.incrementAndGet();
            lastErrorTime = LocalDateTime.now();

            // Track recent errors (keep last 100)
            String errorEntry = String.format("[%s] %s - %s: %s",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                    topic,
                    errorType,
                    exception.getMessage());

            synchronized (recentErrors) {
                recentErrors.add(0, errorEntry); // Add to beginning
                if (recentErrors.size() > MAX_RECENT_ERRORS) {
                    recentErrors.remove(recentErrors.size() - 1); // Remove oldest
                }
            }

            // Log pattern detection
            detectErrorPatterns(topic, errorType);

        } catch (Exception e) {
            logger.error("Error recording Kafka error metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Get comprehensive monitoring report
     */
    public Map<String, Object> getMonitoringReport() {
        Map<String, Object> report = new HashMap<>();

        // Overall statistics
        report.put("totalKafkaErrors", totalKafkaErrors.get());
        report.put("lastErrorTime",
                lastErrorTime != null ? lastErrorTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "Never");

        // Errors by topic
        Map<String, Long> topicErrors = new HashMap<>();
        errorsByTopic.forEach((topic, count) -> topicErrors.put(topic, count.get()));
        report.put("errorsByTopic", topicErrors);

        // Errors by type
        Map<String, Long> typeErrors = new HashMap<>();
        errorsByType.forEach((type, count) -> typeErrors.put(type, count.get()));
        report.put("errorsByType", typeErrors);

        // Last error times by topic
        Map<String, String> lastErrors = new HashMap<>();
        lastErrorByTopic
                .forEach((topic, time) -> lastErrors.put(topic, time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        report.put("lastErrorByTopic", lastErrors);

        // Recent errors (last 20)
        List<String> recentErrorsSubset;
        synchronized (recentErrors) {
            recentErrorsSubset = new ArrayList<>(recentErrors.subList(0, Math.min(20, recentErrors.size())));
        }
        report.put("recentErrors", recentErrorsSubset);

        // Dead letter queue metrics
        try {
            report.put("deadLetterQueueMetrics", deadLetterQueueService.getMetrics());
        } catch (Exception e) {
            report.put("deadLetterQueueMetrics", Map.of("error", "Failed to retrieve DLQ metrics"));
        }

        // Health assessment
        report.put("healthAssessment", generateHealthAssessment());

        // Report timestamp
        report.put("reportTimestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return report;
    }

    /**
     * Generate health assessment based on current metrics
     */
    private Map<String, Object> generateHealthAssessment() {
        Map<String, Object> health = new HashMap<>();

        long totalErrors = totalKafkaErrors.get();
        LocalDateTime now = LocalDateTime.now();

        // Overall health status
        String status;
        if (totalErrors == 0) {
            status = "HEALTHY";
        } else if (totalErrors < 100) {
            status = "MINOR_ISSUES";
        } else if (totalErrors < 1000) {
            status = "DEGRADED";
        } else {
            status = "CRITICAL";
        }

        health.put("overallStatus", status);
        health.put("totalErrorCount", totalErrors);

        // Check for recent error spikes
        boolean recentSpike = false;
        if (lastErrorTime != null) {
            long minutesSinceLastError = java.time.Duration.between(lastErrorTime, now).toMinutes();
            if (minutesSinceLastError < 5 && totalErrors > 10) {
                recentSpike = true;
            }
        }
        health.put("recentErrorSpike", recentSpike);

        // Most problematic topics
        String mostProblematicTopic = errorsByTopic.entrySet().stream()
                .max(Map.Entry.comparingByValue((a1, a2) -> Long.compare(a1.get(), a2.get())))
                .map(Map.Entry::getKey)
                .orElse("None");
        health.put("mostProblematicTopic", mostProblematicTopic);

        // Most common error type
        String mostCommonErrorType = errorsByType.entrySet().stream()
                .max(Map.Entry.comparingByValue((a1, a2) -> Long.compare(a1.get(), a2.get())))
                .map(Map.Entry::getKey)
                .orElse("None");
        health.put("mostCommonErrorType", mostCommonErrorType);

        return health;
    }

    /**
     * Detect error patterns and alert if needed
     */
    private void detectErrorPatterns(String topic, String errorType) {
        try {
            long topicErrorCount = errorsByTopic.get(topic).get();
            long typeErrorCount = errorsByType.get(errorType).get();

            // Alert on error patterns
            if (topicErrorCount % 50 == 0 && topicErrorCount > 0) {
                logger.warn("⚠️ ERROR PATTERN DETECTED: Topic '{}' has reached {} errors", topic, topicErrorCount);
            }

            if (typeErrorCount % 25 == 0 && typeErrorCount > 0) {
                logger.warn("⚠️ ERROR PATTERN DETECTED: Error type '{}' has occurred {} times", errorType,
                        typeErrorCount);
            }

            // Check for recent error surge
            long recentTopicErrors = countRecentErrorsForTopic(topic);
            if (recentTopicErrors >= 10) {
                logger.error("🚨 ERROR SURGE DETECTED: Topic '{}' has {} errors in recent history", topic,
                        recentTopicErrors);
            }

        } catch (Exception e) {
            logger.error("Error in pattern detection: {}", e.getMessage());
        }
    }

    /**
     * Count recent errors for a specific topic
     */
    private long countRecentErrorsForTopic(String topic) {
        synchronized (recentErrors) {
            return recentErrors.stream()
                    .filter(error -> error.contains("] " + topic + " -"))
                    .count();
        }
    }

    /**
     * Reset all monitoring metrics
     */
    public void resetMetrics() {
        errorsByTopic.clear();
        errorsByType.clear();
        lastErrorByTopic.clear();
        synchronized (recentErrors) {
            recentErrors.clear();
        }
        totalKafkaErrors.set(0);
        lastErrorTime = null;

        logger.info("Kafka monitoring metrics reset");
    }

    /**
     * Get summary for quick health check
     */
    public Map<String, Object> getHealthSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalErrors", totalKafkaErrors.get());
        summary.put("activeTopics", errorsByTopic.size());
        summary.put("errorTypes", errorsByType.size());
        summary.put("lastError",
                lastErrorTime != null ? lastErrorTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "Never");

        // Quick health status
        long errors = totalKafkaErrors.get();
        String quickStatus = errors == 0 ? "HEALTHY" : errors < 100 ? "MINOR_ISSUES" : "NEEDS_ATTENTION";
        summary.put("status", quickStatus);

        return summary;
    }
}