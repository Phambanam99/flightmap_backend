package com.phamnam.tracking_vessel_flight.service.realtime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.phamnam.tracking_vessel_flight.service.kafka.DeadLetterQueueService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ScheduledCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledCleanupService.class);

    // Cleanup thresholds
    private static final int INACTIVE_VEHICLE_THRESHOLD_MINUTES = 30;
    private static final int STALE_DATA_THRESHOLD_HOURS = 6;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DeadLetterQueueService deadLetterQueueService;

    // Note: IntelligentStorageService will be integrated in future iterations

    /**
     * Cleanup inactive vehicles every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupInactiveVehicles() {
        logger.info("Starting cleanup of inactive vehicles...");

        try {
            AtomicInteger cleanedFlights = new AtomicInteger(0);
            AtomicInteger cleanedShips = new AtomicInteger(0);

            // Cleanup inactive flights
            cleanupInactiveFlights(cleanedFlights);

            // Cleanup inactive ships
            cleanupInactiveShips(cleanedShips);

            logger.info("Cleanup completed - Flights: {}, Ships: {}",
                    cleanedFlights.get(), cleanedShips.get());

        } catch (Exception e) {
            logger.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate system statistics every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void generateSystemStatistics() {
        try {
            logger.info("=== SYSTEM STATISTICS ===");

            // Redis statistics
            Set<String> flightKeys = redisTemplate.keys("flight:*:current");
            Set<String> shipKeys = redisTemplate.keys("ship:*:current");

            int activeFlights = flightKeys != null ? flightKeys.size() : 0;
            int activeShips = shipKeys != null ? shipKeys.size() : 0;

            logger.info("Active vehicles in Redis - Flights: {}, Ships: {}", activeFlights, activeShips);

            // Memory usage
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            logger.info("Memory usage - Used: {}MB, Free: {}MB, Total: {}MB",
                    usedMemory / 1024 / 1024,
                    freeMemory / 1024 / 1024,
                    totalMemory / 1024 / 1024);

            // Dead Letter Queue statistics
            try {
                java.util.Map<String, Object> dlqMetrics = deadLetterQueueService.getMetrics();
                logger.info("Dead Letter Queue - Total Errors: {}, DLQ Messages: {}",
                        dlqMetrics.get("totalErrorCount"),
                        dlqMetrics.get("deadLetterMessageCount"));
            } catch (Exception e) {
                logger.warn("Failed to get dead letter queue metrics: {}", e.getMessage());
            }

            logger.info("=== END STATISTICS ===");

        } catch (Exception e) {
            logger.error("Error generating statistics: {}", e.getMessage());
        }
    }

    /**
     * Health check every minute
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void performHealthCheck() {
        try {
            // Test Redis connectivity
            redisTemplate.opsForValue().set("health:check", System.currentTimeMillis());

            // Count active vehicles
            Set<String> flightKeys = redisTemplate.keys("flight:*:current");
            Set<String> shipKeys = redisTemplate.keys("ship:*:current");

            int totalVehicles = (flightKeys != null ? flightKeys.size() : 0) +
                    (shipKeys != null ? shipKeys.size() : 0);

            if (totalVehicles > 0) {
                logger.debug("Health check OK - {} active vehicles", totalVehicles);
            }

        } catch (Exception e) {
            logger.warn("Health check failed: {}", e.getMessage());
        }
    }

    /**
     * Cleanup old health check entries daily
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    public void dailyMaintenance() {
        logger.info("Starting daily maintenance...");

        try {
            // Cleanup health check entries
            redisTemplate.delete("health:check");

            // Cleanup any orphaned Redis keys
            cleanupOrphanedKeys();

            logger.info("Daily maintenance completed");

        } catch (Exception e) {
            logger.error("Error during daily maintenance: {}", e.getMessage());
        }
    }

    private void cleanupInactiveFlights(AtomicInteger counter) {
        try {
            Set<String> flightCurrentKeys = redisTemplate.keys("flight:*:current");
            if (flightCurrentKeys == null)
                return;

            LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVE_VEHICLE_THRESHOLD_MINUTES);

            for (String currentKey : flightCurrentKeys) {
                String baseKey = currentKey.replace(":current", "");
                String lastSeenKey = baseKey + ":last_seen";

                LocalDateTime lastSeen = (LocalDateTime) redisTemplate.opsForValue().get(lastSeenKey);

                if (lastSeen != null && lastSeen.isBefore(threshold)) {
                    // Vehicle is inactive, remove all its keys
                    redisTemplate.delete(currentKey);
                    redisTemplate.delete(baseKey + ":previous");
                    redisTemplate.delete(lastSeenKey);
                    redisTemplate.delete(baseKey + ":last_db_save");

                    counter.incrementAndGet();

                    String hexIdent = baseKey.replace("flight:", "");
                    logger.debug("Cleaned up inactive flight: {}", hexIdent);
                }
            }

        } catch (Exception e) {
            logger.error("Error cleaning up flights: {}", e.getMessage());
        }
    }

    private void cleanupInactiveShips(AtomicInteger counter) {
        try {
            Set<String> shipCurrentKeys = redisTemplate.keys("ship:*:current");
            if (shipCurrentKeys == null)
                return;

            LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVE_VEHICLE_THRESHOLD_MINUTES);

            for (String currentKey : shipCurrentKeys) {
                String baseKey = currentKey.replace(":current", "");
                String lastSeenKey = baseKey + ":last_seen";

                LocalDateTime lastSeen = (LocalDateTime) redisTemplate.opsForValue().get(lastSeenKey);

                if (lastSeen != null && lastSeen.isBefore(threshold)) {
                    // Vehicle is inactive, remove all its keys
                    redisTemplate.delete(currentKey);
                    redisTemplate.delete(baseKey + ":previous");
                    redisTemplate.delete(lastSeenKey);
                    redisTemplate.delete(baseKey + ":last_db_save");

                    counter.incrementAndGet();

                    String mmsi = baseKey.replace("ship:", "");
                    logger.debug("Cleaned up inactive ship: {}", mmsi);
                }
            }

        } catch (Exception e) {
            logger.error("Error cleaning up ships: {}", e.getMessage());
        }
    }

    private void cleanupOrphanedKeys() {
        try {
            // Find and cleanup any orphaned keys (e.g., previous/last_seen without current)
            Set<String> allKeys = redisTemplate.keys("flight:*");
            if (allKeys != null) {
                cleanupOrphanedKeysForType(allKeys, "flight:");
            }

            allKeys = redisTemplate.keys("ship:*");
            if (allKeys != null) {
                cleanupOrphanedKeysForType(allKeys, "ship:");
            }

        } catch (Exception e) {
            logger.error("Error cleaning up orphaned keys: {}", e.getMessage());
        }
    }

    private void cleanupOrphanedKeysForType(Set<String> allKeys, String prefix) {
        for (String key : allKeys) {
            if (key.endsWith(":previous") || key.endsWith(":last_seen") || key.endsWith(":last_db_save")) {
                String baseKey = key.substring(0, key.lastIndexOf(":"));
                String currentKey = baseKey + ":current";

                // If no current key exists, cleanup the orphaned key
                if (!redisTemplate.hasKey(currentKey)) {
                    redisTemplate.delete(key);
                    logger.debug("Cleaned up orphaned key: {}", key);
                }
            }
        }
    }

    /**
     * Manual cleanup trigger for admin use
     */
    public void triggerManualCleanup() {
        logger.info("Manual cleanup triggered");
        cleanupInactiveVehicles();
    }

    /**
     * Get cleanup statistics
     */
    public String getCleanupStatistics() {
        try {
            Set<String> flightKeys = redisTemplate.keys("flight:*:current");
            Set<String> shipKeys = redisTemplate.keys("ship:*:current");

            int activeFlights = flightKeys != null ? flightKeys.size() : 0;
            int activeShips = shipKeys != null ? shipKeys.size() : 0;

            // Count stale entries (for information)
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVE_VEHICLE_THRESHOLD_MINUTES);
            int staleFlights = 0;
            int staleShips = 0;

            if (flightKeys != null) {
                for (String key : flightKeys) {
                    String baseKey = key.replace(":current", "");
                    LocalDateTime lastSeen = (LocalDateTime) redisTemplate.opsForValue().get(baseKey + ":last_seen");
                    if (lastSeen != null && lastSeen.isBefore(threshold)) {
                        staleFlights++;
                    }
                }
            }

            if (shipKeys != null) {
                for (String key : shipKeys) {
                    String baseKey = key.replace(":current", "");
                    LocalDateTime lastSeen = (LocalDateTime) redisTemplate.opsForValue().get(baseKey + ":last_seen");
                    if (lastSeen != null && lastSeen.isBefore(threshold)) {
                        staleShips++;
                    }
                }
            }

            return String.format("Cleanup Stats - Active: %d flights, %d ships | Stale: %d flights, %d ships",
                    activeFlights, activeShips, staleFlights, staleShips);

        } catch (Exception e) {
            logger.error("Error getting cleanup statistics: {}", e.getMessage());
            return "Cleanup statistics unavailable";
        }
    }
}