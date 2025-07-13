package com.phamnam.tracking_vessel_flight.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.models.raw.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.raw.RawVesselData;
import com.phamnam.tracking_vessel_flight.service.kafka.TrackingKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Kafka Test Utilities
 * 
 * Provides utilities for testing Kafka functionality including:
 * - Publishing test messages to raw data topics
 * - Consuming and verifying messages from topics
 * - Testing topic configuration and connectivity
 * - Performance testing utilities
 */
@TestComponent
@RequiredArgsConstructor
@Slf4j
public class KafkaTestUtils {

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired(required = false)
    private TrackingKafkaProducer trackingKafkaProducer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Topic names from configuration
    @Value("${app.kafka.topics.raw-flightradar24-data:raw-flightradar24-data}")
    private String rawFlightRadar24Topic;

    @Value("${app.kafka.topics.raw-adsbexchange-data:raw-adsbexchange-data}")
    private String rawAdsbExchangeTopic;

    @Value("${app.kafka.topics.raw-marinetraffic-data:raw-marinetraffic-data}")
    private String rawMarineTrafficTopic;

    @Value("${app.kafka.topics.raw-vesselfinder-data:raw-vesselfinder-data}")
    private String rawVesselFinderTopic;

    @Value("${app.kafka.topics.raw-chinaports-data:raw-chinaports-data}")
    private String rawChinaportsTopic;

    @Value("${app.kafka.topics.raw-marinetrafficv2-data:raw-marinetrafficv2-data}")
    private String rawMarineTrafficV2Topic;

    // ============================================================================
    // RAW DATA PUBLISHING UTILITIES
    // ============================================================================

    /**
     * Publish test aircraft data to raw FlightRadar24 topic
     */
    public CompletableFuture<SendResult<String, Object>> publishTestFlightRadar24Data(String key,
            RawAircraftData data) {
        log.info("📡 Publishing test data to FlightRadar24 topic: key={}", key);
        return sendMessage(rawFlightRadar24Topic, key, data);
    }

    /**
     * Publish test aircraft data to raw ADS-B Exchange topic
     */
    public CompletableFuture<SendResult<String, Object>> publishTestAdsbExchangeData(String key, RawAircraftData data) {
        log.info("📡 Publishing test data to ADS-B Exchange topic: key={}", key);
        return sendMessage(rawAdsbExchangeTopic, key, data);
    }

    /**
     * Publish test vessel data to raw MarineTraffic topic
     */
    public CompletableFuture<SendResult<String, Object>> publishTestMarineTrafficData(String key, RawVesselData data) {
        log.info("🚢 Publishing test data to MarineTraffic topic: key={}", key);
        return sendMessage(rawMarineTrafficTopic, key, data);
    }

    /**
     * Publish test vessel data to raw VesselFinder topic
     */
    public CompletableFuture<SendResult<String, Object>> publishTestVesselFinderData(String key, RawVesselData data) {
        log.info("🚢 Publishing test data to VesselFinder topic: key={}", key);
        return sendMessage(rawVesselFinderTopic, key, data);
    }

    /**
     * Publish test vessel data to raw Chinaports topic
     */
    public CompletableFuture<SendResult<String, Object>> publishTestChinaportsData(String key, RawVesselData data) {
        log.info("🚢 Publishing test data to Chinaports topic: key={}", key);
        return sendMessage(rawChinaportsTopic, key, data);
    }

    /**
     * Publish test vessel data to raw MarineTraffic V2 topic
     */
    public CompletableFuture<SendResult<String, Object>> publishTestMarineTrafficV2Data(String key,
            RawVesselData data) {
        log.info("🚢 Publishing test data to MarineTraffic V2 topic: key={}", key);
        return sendMessage(rawMarineTrafficV2Topic, key, data);
    }

    /**
     * Generic message sending utility
     */
    private CompletableFuture<SendResult<String, Object>> sendMessage(String topic, String key, Object data) {
        if (kafkaTemplate == null) {
            log.warn("⚠️ KafkaTemplate not available - creating mock result");
            return CompletableFuture.completedFuture(null);
        }

        try {
            return kafkaTemplate.send(topic, key, data);
        } catch (Exception e) {
            log.error("❌ Failed to send message to topic {}: {}", topic, e.getMessage(), e);
            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    // ============================================================================
    // BULK TESTING UTILITIES
    // ============================================================================

    /**
     * Publish multiple test aircraft messages to all aircraft topics
     */
    public Map<String, List<CompletableFuture<SendResult<String, Object>>>> publishBulkAircraftTestData(
            List<RawAircraftData> flightRadar24Data, List<RawAircraftData> adsbData) {

        Map<String, List<CompletableFuture<SendResult<String, Object>>>> results = new HashMap<>();

        // Publish FlightRadar24 data
        List<CompletableFuture<SendResult<String, Object>>> fr24Results = new ArrayList<>();
        for (int i = 0; i < flightRadar24Data.size(); i++) {
            RawAircraftData data = flightRadar24Data.get(i);
            String key = data.getHexident() != null ? data.getHexident() : "aircraft_" + i;
            fr24Results.add(publishTestFlightRadar24Data(key, data));
        }
        results.put("flightradar24", fr24Results);

        // Publish ADS-B Exchange data
        List<CompletableFuture<SendResult<String, Object>>> adsbResults = new ArrayList<>();
        for (int i = 0; i < adsbData.size(); i++) {
            RawAircraftData data = adsbData.get(i);
            String key = data.getHexident() != null ? data.getHexident() : "aircraft_" + i;
            adsbResults.add(publishTestAdsbExchangeData(key, data));
        }
        results.put("adsbexchange", adsbResults);

        log.info("📡 Published {} FlightRadar24 and {} ADS-B Exchange test messages",
                flightRadar24Data.size(), adsbData.size());

        return results;
    }

    /**
     * Publish multiple test vessel messages to all vessel topics
     */
    public Map<String, List<CompletableFuture<SendResult<String, Object>>>> publishBulkVesselTestData(
            Map<String, List<RawVesselData>> dataBySource) {

        Map<String, List<CompletableFuture<SendResult<String, Object>>>> results = new HashMap<>();

        dataBySource.forEach((source, vesselDataList) -> {
            List<CompletableFuture<SendResult<String, Object>>> sourceResults = new ArrayList<>();

            for (int i = 0; i < vesselDataList.size(); i++) {
                RawVesselData data = vesselDataList.get(i);
                String key = data.getMmsi() != null ? data.getMmsi() : "vessel_" + i;

                switch (source) {
                    case "marinetraffic":
                        sourceResults.add(publishTestMarineTrafficData(key, data));
                        break;
                    case "vesselfinder":
                        sourceResults.add(publishTestVesselFinderData(key, data));
                        break;
                    case "chinaports":
                        sourceResults.add(publishTestChinaportsData(key, data));
                        break;
                    case "marinetrafficv2":
                        sourceResults.add(publishTestMarineTrafficV2Data(key, data));
                        break;
                    default:
                        log.warn("⚠️ Unknown vessel source: {}", source);
                }
            }

            results.put(source, sourceResults);
            log.info("🚢 Published {} {} test messages", vesselDataList.size(), source);
        });

        return results;
    }

    // ============================================================================
    // MESSAGE VERIFICATION UTILITIES
    // ============================================================================

    /**
     * Wait for all futures to complete and verify results
     */
    public TestPublishResult waitForPublishResults(
            Map<String, List<CompletableFuture<SendResult<String, Object>>>> futuresBySource,
            long timeoutSeconds) {
        TestPublishResult result = new TestPublishResult();

        futuresBySource.forEach((source, futures) -> {
            int successCount = 0;
            int failureCount = 0;

            for (CompletableFuture<SendResult<String, Object>> future : futures) {
                try {
                    SendResult<String, Object> sendResult = future.get(timeoutSeconds, TimeUnit.SECONDS);
                    if (sendResult != null) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    failureCount++;
                    log.error("❌ Failed to send message from source {}: {}", source, e.getMessage());
                }
            }

            result.addSourceResult(source, successCount, failureCount);
        });

        return result;
    }

    // ============================================================================
    // TEST DATA GENERATION AND PUBLISHING
    // ============================================================================

    /**
     * Generate and publish complete test scenario
     */
    public TestScenarioResult executeTestScenario(String scenarioName, int aircraftCount, int vesselCountPerSource) {
        log.info("🧪 Executing test scenario: {} with {} aircraft and {} vessels per source",
                scenarioName, aircraftCount, vesselCountPerSource);

        TestScenarioResult scenarioResult = new TestScenarioResult(scenarioName);

        try {
            // Generate test data
            Map<String, List<RawAircraftData>> aircraftData = SampleDataGenerator
                    .generateRawAircraftDataBySource(aircraftCount);
            Map<String, List<RawVesselData>> vesselData = SampleDataGenerator
                    .generateRawVesselDataBySource(vesselCountPerSource);

            // Publish aircraft data
            List<RawAircraftData> fr24Data = aircraftData.get("flightradar24");
            List<RawAircraftData> adsbData = aircraftData.get("adsbexchange");
            Map<String, List<CompletableFuture<SendResult<String, Object>>>> aircraftResults = publishBulkAircraftTestData(
                    fr24Data, adsbData);

            // Publish vessel data
            Map<String, List<CompletableFuture<SendResult<String, Object>>>> vesselResults = publishBulkVesselTestData(
                    vesselData);

            // Wait for results
            TestPublishResult aircraftPublishResult = waitForPublishResults(aircraftResults, 30);
            TestPublishResult vesselPublishResult = waitForPublishResults(vesselResults, 30);

            scenarioResult.setAircraftResults(aircraftPublishResult);
            scenarioResult.setVesselResults(vesselPublishResult);
            scenarioResult.setSuccess(true);

            log.info("✅ Test scenario {} completed successfully", scenarioName);

        } catch (Exception e) {
            log.error("❌ Test scenario {} failed: {}", scenarioName, e.getMessage(), e);
            scenarioResult.setSuccess(false);
            scenarioResult.setErrorMessage(e.getMessage());
        }

        return scenarioResult;
    }

    // ============================================================================
    // TOPIC VALIDATION UTILITIES
    // ============================================================================

    /**
     * Verify all raw data topics are configured correctly
     */
    public TopicValidationResult validateRawDataTopics() {
        TopicValidationResult result = new TopicValidationResult();

        String[] aircraftTopics = { rawFlightRadar24Topic, rawAdsbExchangeTopic };
        String[] vesselTopics = { rawMarineTrafficTopic, rawVesselFinderTopic, rawChinaportsTopic,
                rawMarineTrafficV2Topic };

        // Validate aircraft topics
        for (String topic : aircraftTopics) {
            result.addTopicValidation(topic, validateTopicConnectivity(topic));
        }

        // Validate vessel topics
        for (String topic : vesselTopics) {
            result.addTopicValidation(topic, validateTopicConnectivity(topic));
        }

        return result;
    }

    /**
     * Test connectivity to a specific topic
     */
    private boolean validateTopicConnectivity(String topic) {
        try {
            if (kafkaTemplate == null) {
                log.warn("⚠️ KafkaTemplate not available for topic validation: {}", topic);
                return false;
            }

            // Try to send a test message
            String testKey = "test_" + System.currentTimeMillis();
            Map<String, Object> testData = Map.of(
                    "test", true,
                    "timestamp", LocalDateTime.now().toString(),
                    "topic", topic);

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, testKey, testData);
            SendResult<String, Object> result = future.get(5, TimeUnit.SECONDS);

            log.info("✅ Topic {} connectivity validated successfully", topic);
            return true;

        } catch (Exception e) {
            log.error("❌ Topic {} connectivity validation failed: {}", topic, e.getMessage());
            return false;
        }
    }

    // ============================================================================
    // PERFORMANCE TESTING UTILITIES
    // ============================================================================

    /**
     * Performance test for high-volume message publishing
     */
    public PerformanceTestResult performanceTest(int messagesPerTopic, int concurrentThreads) {
        log.info("🚀 Starting performance test: {} messages per topic, {} threads", messagesPerTopic,
                concurrentThreads);

        PerformanceTestResult result = new PerformanceTestResult();
        long startTime = System.currentTimeMillis();

        try {
            // Generate large amounts of test data
            List<RawAircraftData> aircraftData = new ArrayList<>();
            List<RawVesselData> vesselData = new ArrayList<>();

            for (int i = 0; i < messagesPerTopic; i++) {
                aircraftData.add(SampleDataGenerator.generateRawAircraftData("flightradar24", "/api/test"));
                vesselData.add(SampleDataGenerator.generateRawVesselData("marinetraffic", "/api/test"));
            }

            // Publish in parallel
            List<CompletableFuture<Void>> publishTasks = new ArrayList<>();

            for (int i = 0; i < concurrentThreads; i++) {
                final int threadIndex = i;
                CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                    int startIdx = threadIndex * (messagesPerTopic / concurrentThreads);
                    int endIdx = Math.min((threadIndex + 1) * (messagesPerTopic / concurrentThreads), messagesPerTopic);

                    for (int j = startIdx; j < endIdx; j++) {
                        try {
                            publishTestFlightRadar24Data("perf_aircraft_" + j, aircraftData.get(j));
                            publishTestMarineTrafficData("perf_vessel_" + j, vesselData.get(j));
                        } catch (Exception e) {
                            log.error("❌ Error in performance test thread {}: {}", threadIndex, e.getMessage());
                        }
                    }
                });
                publishTasks.add(task);
            }

            // Wait for all tasks to complete
            CompletableFuture.allOf(publishTasks.toArray(new CompletableFuture[0])).get(300, TimeUnit.SECONDS);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            result.setTotalMessages(messagesPerTopic * 2); // aircraft + vessel
            result.setDurationMs(duration);
            result.setMessagesPerSecond((double) (messagesPerTopic * 2) / (duration / 1000.0));
            result.setSuccess(true);

            log.info("🏁 Performance test completed: {} messages in {}ms ({} msg/sec)",
                    result.getTotalMessages(), duration, String.format("%.2f", result.getMessagesPerSecond()));

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("❌ Performance test failed: {}", e.getMessage(), e);
        }

        return result;
    }

    // ============================================================================
    // RESULT CLASSES
    // ============================================================================

    public static class TestPublishResult {
        private final Map<String, Integer> successCounts = new HashMap<>();
        private final Map<String, Integer> failureCounts = new HashMap<>();

        public void addSourceResult(String source, int successCount, int failureCount) {
            successCounts.put(source, successCount);
            failureCounts.put(source, failureCount);
        }

        public int getTotalSuccess() {
            return successCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        public int getTotalFailures() {
            return failureCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        public boolean isSuccess() {
            return getTotalFailures() == 0;
        }

        // Getters
        public Map<String, Integer> getSuccessCounts() {
            return successCounts;
        }

        public Map<String, Integer> getFailureCounts() {
            return failureCounts;
        }
    }

    public static class TestScenarioResult {
        private final String scenarioName;
        private TestPublishResult aircraftResults;
        private TestPublishResult vesselResults;
        private boolean success;
        private String errorMessage;

        public TestScenarioResult(String scenarioName) {
            this.scenarioName = scenarioName;
        }

        // Getters and setters
        public String getScenarioName() {
            return scenarioName;
        }

        public TestPublishResult getAircraftResults() {
            return aircraftResults;
        }

        public void setAircraftResults(TestPublishResult aircraftResults) {
            this.aircraftResults = aircraftResults;
        }

        public TestPublishResult getVesselResults() {
            return vesselResults;
        }

        public void setVesselResults(TestPublishResult vesselResults) {
            this.vesselResults = vesselResults;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    public static class TopicValidationResult {
        private final Map<String, Boolean> topicResults = new HashMap<>();

        public void addTopicValidation(String topic, boolean isValid) {
            topicResults.put(topic, isValid);
        }

        public boolean areAllTopicsValid() {
            return topicResults.values().stream().allMatch(Boolean::booleanValue);
        }

        public Map<String, Boolean> getTopicResults() {
            return topicResults;
        }
    }

    public static class PerformanceTestResult {
        private int totalMessages;
        private long durationMs;
        private double messagesPerSecond;
        private boolean success;
        private String errorMessage;

        // Getters and setters
        public int getTotalMessages() {
            return totalMessages;
        }

        public void setTotalMessages(int totalMessages) {
            this.totalMessages = totalMessages;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public double getMessagesPerSecond() {
            return messagesPerSecond;
        }

        public void setMessagesPerSecond(double messagesPerSecond) {
            this.messagesPerSecond = messagesPerSecond;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}