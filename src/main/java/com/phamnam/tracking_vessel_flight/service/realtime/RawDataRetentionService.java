package com.phamnam.tracking_vessel_flight.service.realtime;

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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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

        do {
            // TODO: Implement repository methods for batch deletion
            // if (isEmergency) {
            // deletedInBatch = rawAircraftDataRepository.deleteEmergencyDataBatch(cutoff,
            // batchSize);
            // } else {
            // deletedInBatch = rawAircraftDataRepository.deleteStandardDataBatch(
            // cutoff, maxQuality, batchSize);
            // }
            deletedInBatch = 0; // Temporary placeholder

            totalDeleted += deletedInBatch;

            if (deletedInBatch > 0) {
                log.debug("Deleted {} {} aircraft records in batch", deletedInBatch, category);
            }

        } while (deletedInBatch > 0);

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

        do {
            // TODO: Implement repository methods for batch deletion
            // if (isEmergency) {
            // deletedInBatch = rawVesselDataRepository.deleteEmergencyDataBatch(cutoff,
            // batchSize);
            // } else {
            // deletedInBatch = rawVesselDataRepository.deleteStandardDataBatch(
            // cutoff, maxQuality, batchSize);
            // }
            deletedInBatch = 0; // Temporary placeholder

            totalDeleted += deletedInBatch;

            if (deletedInBatch > 0) {
                log.debug("Deleted {} {} vessel records in batch", deletedInBatch, category);
            }

        } while (deletedInBatch > 0);

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
        // TODO: Implement repository method
        // return rawAircraftDataRepository.deleteHighQualityDataBatch(cutoff, 0.8,
        // batchSize);
        return 0; // Temporary placeholder
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
        // TODO: Implement repository method
        // return rawVesselDataRepository.deleteHighQualityDataBatch(cutoff, 0.8,
        // batchSize);
        return 0; // Temporary placeholder
    }

    /**
     * Get count of records that would be deleted in next cleanup
     */
    public Map<String, Object> getCleanupPreview() {
        LocalDateTime standardCutoff = LocalDateTime.now().minusDays(retentionDays);
        LocalDateTime emergencyCutoff = LocalDateTime.now().minusDays(emergencyRetentionDays);
        LocalDateTime highQualityCutoff = LocalDateTime.now().minusDays(highQualityRetentionDays);

        // Get counts for aircraft data
        long aircraftStandard = rawAircraftDataRepository.countStandardDataForDeletion(standardCutoff, 0.8);
        long aircraftEmergency = rawAircraftDataRepository.countEmergencyDataForDeletion(emergencyCutoff);
        long aircraftHighQuality = rawAircraftDataRepository.countHighQualityDataForDeletion(highQualityCutoff, 0.8);

        // Get counts for vessel data
        long vesselStandard = rawVesselDataRepository.countStandardDataForDeletion(standardCutoff, 0.8);
        long vesselEmergency = rawVesselDataRepository.countEmergencyDataForDeletion(emergencyCutoff);
        long vesselHighQuality = rawVesselDataRepository.countHighQualityDataForDeletion(highQualityCutoff, 0.8);

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

        long aircraftDeleted = rawAircraftDataRepository.deleteByDataSourceAndReceivedAtBefore(dataSource, cutoff);
        long vesselDeleted = rawVesselDataRepository.deleteByDataSourceAndReceivedAtBefore(dataSource, cutoff);

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