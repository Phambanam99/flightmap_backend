package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.models.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.RawVesselData;
import com.phamnam.tracking_vessel_flight.repository.RawAircraftDataRepository;
import com.phamnam.tracking_vessel_flight.repository.RawVesselDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawDataRetentionService {

    @Value("${raw.data.retention.days:7}")
    private int retentionDays;

    @Value("${raw.data.retention.enabled:true}")
    private boolean retentionEnabled;

    @Value("${raw.data.retention.batch-size:1000}")
    private int batchSize;

    @Value("${raw.data.retention.emergency-retention-days:30}")
    private int emergencyRetentionDays;

    @Value("${raw.data.retention.high-quality-retention-days:14}")
    private int highQualityRetentionDays;

    private final RawAircraftDataRepository rawAircraftDataRepository;
    private final RawVesselDataRepository rawVesselDataRepository;

    // Statistics
    private final AtomicLong totalDeletedRecords = new AtomicLong(0);
    private final AtomicLong totalCleanupRuns = new AtomicLong(0);
    private final AtomicLong lastCleanupDuration = new AtomicLong(0);
    private LocalDateTime lastCleanupTime = null;

    /**
     * Scheduled cleanup task - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Async("scheduledTaskExecutor")
    public void performScheduledCleanup() {
        if (!retentionEnabled) {
            log.debug("Raw data retention disabled, skipping cleanup");
            return;
        }

        log.info("🧹 Starting scheduled raw data cleanup...");
        long startTime = System.currentTimeMillis();

        long deletedRecords = performCleanup();

        long duration = System.currentTimeMillis() - startTime;
        lastCleanupDuration.set(duration);
        lastCleanupTime = LocalDateTime.now();
        totalCleanupRuns.incrementAndGet();

        log.info("✅ Scheduled cleanup completed: {} records deleted in {}ms",
                deletedRecords, duration);
    }

    /**
     * Manual cleanup trigger
     */
    @Transactional
    public long performCleanup() {
        if (!retentionEnabled) {
            log.warn("Raw data retention is disabled");
            return 0;
        }

        log.info("🗑️ Performing raw data cleanup with retention period: {} days", retentionDays);

        long totalDeleted = 0;

        // Clean up aircraft data
        totalDeleted += cleanupAircraftData();

        // Clean up vessel data
        totalDeleted += cleanupVesselData();

        totalDeletedRecords.addAndGet(totalDeleted);

        log.info("🧹 Cleanup completed: {} total records deleted", totalDeleted);
        return totalDeleted;
    }

    /**
     * Clean up old aircraft data
     */
    private long cleanupAircraftData() {
        log.info("🛩️ Cleaning up aircraft raw data...");

        LocalDateTime standardCutoff = LocalDateTime.now().minusDays(retentionDays);
        LocalDateTime emergencyCutoff = LocalDateTime.now().minusDays(emergencyRetentionDays);
        LocalDateTime highQualityCutoff = LocalDateTime.now().minusDays(highQualityRetentionDays);

        long deletedCount = 0;

        try {
            // Delete standard data (non-emergency, standard quality)
            long standardDeleted = deleteAircraftDataInBatches(
                    standardCutoff, false, 0.8, "standard");
            deletedCount += standardDeleted;

            // Delete emergency data (older than emergency retention period)
            long emergencyDeleted = deleteEmergencyAircraftData(emergencyCutoff);
            deletedCount += emergencyDeleted;

            // Delete high-quality data (older than high-quality retention period)
            long highQualityDeleted = deleteHighQualityAircraftData(highQualityCutoff);
            deletedCount += highQualityDeleted;

            log.info("✈️ Aircraft data cleanup: {} standard, {} emergency, {} high-quality records deleted",
                    standardDeleted, emergencyDeleted, highQualityDeleted);

        } catch (Exception e) {
            log.error("Error during aircraft data cleanup: {}", e.getMessage(), e);
        }

        return deletedCount;
    }

    /**
     * Clean up old vessel data
     */
    private long cleanupVesselData() {
        log.info("🚢 Cleaning up vessel raw data...");

        LocalDateTime standardCutoff = LocalDateTime.now().minusDays(retentionDays);
        LocalDateTime emergencyCutoff = LocalDateTime.now().minusDays(emergencyRetentionDays);
        LocalDateTime highQualityCutoff = LocalDateTime.now().minusDays(highQualityRetentionDays);

        long deletedCount = 0;

        try {
            // Delete standard data
            long standardDeleted = deleteVesselDataInBatches(
                    standardCutoff, false, 0.8, "standard");
            deletedCount += standardDeleted;

            // Delete emergency data (older than emergency retention period)
            long emergencyDeleted = deleteEmergencyVesselData(emergencyCutoff);
            deletedCount += emergencyDeleted;

            // Delete high-quality data
            long highQualityDeleted = deleteHighQualityVesselData(highQualityCutoff);
            deletedCount += highQualityDeleted;

            log.info("🛳️ Vessel data cleanup: {} standard, {} emergency, {} high-quality records deleted",
                    standardDeleted, emergencyDeleted, highQualityDeleted);

        } catch (Exception e) {
            log.error("Error during vessel data cleanup: {}", e.getMessage(), e);
        }

        return deletedCount;
    }

    /**
     * Delete aircraft data in batches
     */
    @Transactional
    private long deleteAircraftDataInBatches(LocalDateTime cutoff, boolean isEmergency,
            double maxQuality, String category) {
        long totalDeleted = 0;
        int deletedInBatch;

        // Use existing repository methods for deletion
        List<RawAircraftData> dataToDelete;

        if (isEmergency) {
            // Find emergency aircraft data older than cutoff
            dataToDelete = rawAircraftDataRepository.findByEmergencyTrueAndReceivedAtBetween(
                    LocalDateTime.MIN, cutoff);
        } else {
            // Find standard aircraft data older than cutoff
            dataToDelete = rawAircraftDataRepository.findByReceivedAtBefore(cutoff);
        }

        if (!dataToDelete.isEmpty()) {
            // Delete in batches to avoid memory issues
            for (int i = 0; i < dataToDelete.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, dataToDelete.size());
                List<RawAircraftData> batch = dataToDelete.subList(i, endIndex);
                rawAircraftDataRepository.deleteAll(batch);
                totalDeleted += batch.size();
                log.debug("Deleted {} {} aircraft records in batch", batch.size(), category);
            }
        }

        return totalDeleted;
    }

    /**
     * Delete vessel data in batches
     */
    @Transactional
    private long deleteVesselDataInBatches(LocalDateTime cutoff, boolean isEmergency,
            double maxQuality, String category) {
        long totalDeleted = 0;
        int deletedInBatch;

        // Use existing repository methods for deletion
        List<RawVesselData> dataToDelete;

        if (isEmergency) {
            // Find emergency vessel data older than cutoff (using dangerous cargo as proxy
            // for emergency)
            dataToDelete = rawVesselDataRepository.findByDangerousCargoTrueAndReceivedAtBetween(
                    LocalDateTime.MIN, cutoff);
        } else {
            // Find standard vessel data older than cutoff
            dataToDelete = rawVesselDataRepository.findByReceivedAtBefore(cutoff);
        }

        if (!dataToDelete.isEmpty()) {
            // Delete in batches to avoid memory issues
            for (int i = 0; i < dataToDelete.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, dataToDelete.size());
                List<RawVesselData> batch = dataToDelete.subList(i, endIndex);
                rawVesselDataRepository.deleteAll(batch);
                totalDeleted += batch.size();
                log.debug("Deleted {} {} vessel records in batch", batch.size(), category);
            }
        }

        return totalDeleted;
    }

    /**
     * Delete emergency aircraft data
     */
    private long deleteEmergencyAircraftData(LocalDateTime cutoff) {
        return deleteAircraftDataInBatches(cutoff, true, 1.0, "emergency");
    }

    /**
     * Delete high-quality aircraft data
     */
    private long deleteHighQualityAircraftData(LocalDateTime cutoff) {
        // Implementation for high-quality data detection and deletion
        try {
            // Find aircraft data with high data quality (>= 0.8) older than cutoff
            List<RawAircraftData> highQualityData = rawAircraftDataRepository
                    .findByReceivedAtBefore(cutoff)
                    .stream()
                    .filter(data -> data.getDataQuality() != null && data.getDataQuality() >= 0.8)
                    .collect(Collectors.toList());

            if (!highQualityData.isEmpty()) {
                rawAircraftDataRepository.deleteAll(highQualityData);
                log.info("🗑️ Deleted {} high-quality aircraft records older than {}",
                        highQualityData.size(), cutoff);
                return highQualityData.size();
            }

            return 0;
        } catch (Exception e) {
            log.error("Error deleting high-quality aircraft data: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Delete emergency vessel data
     */
    private long deleteEmergencyVesselData(LocalDateTime cutoff) {
        return deleteVesselDataInBatches(cutoff, true, 1.0, "emergency");
    }

    /**
     * Delete high-quality vessel data
     */
    private long deleteHighQualityVesselData(LocalDateTime cutoff) {
        // Implementation for high-quality vessel data detection and deletion
        try {
            // Find vessel data with high data quality (>= 0.8) older than cutoff
            List<RawVesselData> highQualityData = rawVesselDataRepository
                    .findByReceivedAtBefore(cutoff)
                    .stream()
                    .filter(data -> data.getDataQuality() != null && data.getDataQuality() >= 0.8)
                    .collect(Collectors.toList());

            if (!highQualityData.isEmpty()) {
                rawVesselDataRepository.deleteAll(highQualityData);
                log.info("🗑️ Deleted {} high-quality vessel records older than {}",
                        highQualityData.size(), cutoff);
                return highQualityData.size();
            }

            return 0;
        } catch (Exception e) {
            log.error("Error deleting high-quality vessel data: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get count of records that would be deleted in next cleanup
     */
    public Map<String, Object> getCleanupPreview() {
        LocalDateTime standardCutoff = LocalDateTime.now().minusDays(retentionDays);
        LocalDateTime emergencyCutoff = LocalDateTime.now().minusDays(emergencyRetentionDays);
        LocalDateTime highQualityCutoff = LocalDateTime.now().minusDays(highQualityRetentionDays);

        // Get counts for aircraft data (full implementation)
        long aircraftStandard = rawAircraftDataRepository.findByReceivedAtBefore(standardCutoff).size();
        long aircraftEmergency = rawAircraftDataRepository.findByEmergencyTrueAndReceivedAtBetween(
                LocalDateTime.MIN, emergencyCutoff).size();
        long aircraftHighQuality = rawAircraftDataRepository.findByReceivedAtBefore(highQualityCutoff)
                .stream()
                .mapToLong(data -> (data.getDataQuality() != null && data.getDataQuality() >= 0.8) ? 1 : 0)
                .sum();

        // Get counts for vessel data (full implementation)
        long vesselStandard = rawVesselDataRepository.findByReceivedAtBefore(standardCutoff).size();
        long vesselEmergency = rawVesselDataRepository.findByDangerousCargoTrueAndReceivedAtBetween(
                LocalDateTime.MIN, emergencyCutoff).size();
        long vesselHighQuality = rawVesselDataRepository.findByReceivedAtBefore(highQualityCutoff)
                .stream()
                .mapToLong(data -> (data.getDataQuality() != null && data.getDataQuality() >= 0.8) ? 1 : 0)
                .sum();

        return Map.of(
                "retentionEnabled", retentionEnabled,
                "retentionDays", retentionDays,
                "emergencyRetentionDays", emergencyRetentionDays,
                "highQualityRetentionDays", highQualityRetentionDays,
                "aircraftToDelete", Map.of(
                        "standard", aircraftStandard,
                        "emergency", aircraftEmergency,
                        "highQuality", aircraftHighQuality,
                        "total", aircraftStandard + aircraftEmergency + aircraftHighQuality),
                "vesselToDelete", Map.of(
                        "standard", vesselStandard,
                        "emergency", vesselEmergency,
                        "highQuality", vesselHighQuality,
                        "total", vesselStandard + vesselEmergency + vesselHighQuality),
                "totalToDelete", aircraftStandard + aircraftEmergency + aircraftHighQuality +
                        vesselStandard + vesselEmergency + vesselHighQuality);
    }

    /**
     * Get retention statistics
     */
    public Map<String, Object> getRetentionStats() {
        return Map.of(
                "retentionEnabled", retentionEnabled,
                "retentionDays", retentionDays,
                "emergencyRetentionDays", emergencyRetentionDays,
                "highQualityRetentionDays", highQualityRetentionDays,
                "batchSize", batchSize,
                "totalDeletedRecords", totalDeletedRecords.get(),
                "totalCleanupRuns", totalCleanupRuns.get(),
                "lastCleanupTime", lastCleanupTime,
                "lastCleanupDurationMs", lastCleanupDuration.get());
    }

    /**
     * Reset retention statistics
     */
    public void resetStats() {
        totalDeletedRecords.set(0);
        totalCleanupRuns.set(0);
        lastCleanupDuration.set(0);
        lastCleanupTime = null;
        log.info("Retention statistics reset");
    }

    /**
     * Force cleanup of specific data source
     */
    @Transactional
    public long forceCleanupBySource(String dataSource, int days) {
        if (!retentionEnabled) {
            log.warn("Raw data retention is disabled");
            return 0;
        }

        log.info("🧹 Force cleanup for source: {} older than {} days", dataSource, days);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

        // Find and delete by data source (simplified implementation)
        List<RawAircraftData> aircraftToDelete = rawAircraftDataRepository
                .findByDataSourceAndReceivedAtBetween(dataSource, LocalDateTime.MIN, cutoff);
        List<RawVesselData> vesselToDelete = rawVesselDataRepository
                .findByDataSourceAndReceivedAtBetween(dataSource, LocalDateTime.MIN, cutoff);

        rawAircraftDataRepository.deleteAll(aircraftToDelete);
        rawVesselDataRepository.deleteAll(vesselToDelete);

        long aircraftDeleted = aircraftToDelete.size();
        long vesselDeleted = vesselToDelete.size();

        long totalDeleted = aircraftDeleted + vesselDeleted;
        totalDeletedRecords.addAndGet(totalDeleted);

        log.info("✅ Force cleanup completed: {} aircraft, {} vessel records deleted from {}",
                aircraftDeleted, vesselDeleted, dataSource);

        return totalDeleted;
    }

    /**
     * Get storage size information
     */
    public Map<String, Object> getStorageInfo() {
        // Get table sizes
        long aircraftCount = rawAircraftDataRepository.count();
        long vesselCount = rawVesselDataRepository.count();

        // Estimate storage sizes (approximate)
        long estimatedAircraftSize = aircraftCount * 2048; // ~2KB per record
        long estimatedVesselSize = vesselCount * 2048; // ~2KB per record

        return Map.of(
                "aircraftRecordCount", aircraftCount,
                "vesselRecordCount", vesselCount,
                "totalRecordCount", aircraftCount + vesselCount,
                "estimatedAircraftSizeBytes", estimatedAircraftSize,
                "estimatedVesselSizeBytes", estimatedVesselSize,
                "estimatedTotalSizeBytes", estimatedAircraftSize + estimatedVesselSize,
                "estimatedTotalSizeMB", (estimatedAircraftSize + estimatedVesselSize) / (1024 * 1024));
    }
}