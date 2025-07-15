package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import com.phamnam.tracking_vessel_flight.repository.ShipTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for warming up Redis cache with recent vessel data
 * from the database on application startup and providing manual refresh capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService implements ApplicationRunner {

    private final ShipTrackingRepository shipTrackingRepository;
    private final TrackingCacheService trackingCacheService;

    /**
     * Runs after application startup to warm up Redis cache
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("🔥 Starting cache warmup process...");
        warmupVesselCache().thenRun(() -> 
            log.info("✅ Cache warmup completed successfully")
        ).exceptionally(throwable -> {
            log.error("❌ Cache warmup failed: {}", throwable.getMessage(), throwable);
            return null;
        });
    }

    /**
     * Asynchronously warm up vessel cache with recent data
     */
    @Async
    public CompletableFuture<Void> warmupVesselCache() {
        try {
            // Get recent vessel data from last 2 hours to populate cache
            LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
            
            // Get all recent tracking data and limit results
            List<ShipTracking> recentTrackings = shipTrackingRepository
                .findAll(PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "timestamp")))
                .getContent()
                .stream()
                .filter(tracking -> tracking.getTimestamp().isAfter(twoHoursAgo))
                .toList();

            log.info("📦 Found {} recent vessel tracking records to cache", recentTrackings.size());

            int cachedCount = 0;
            for (ShipTracking tracking : recentTrackings) {
                try {
                    // Convert to ShipTrackingRequest for caching
                    ShipTrackingRequest request = convertToRequest(tracking);
                    
                    // Cache the vessel data
                    trackingCacheService.cacheShipTracking(request);
                    cachedCount++;
                    
                } catch (Exception e) {
                    log.warn("⚠️ Failed to cache vessel data for MMSI {}: {}", 
                            tracking.getMmsi(), e.getMessage());
                }
            }

            log.info("✅ Successfully cached {} vessel records in Redis", cachedCount);
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ Error during vessel cache warmup: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Convert ShipTracking entity to ShipTrackingRequest DTO
     */
    private ShipTrackingRequest convertToRequest(ShipTracking tracking) {
        return ShipTrackingRequest.builder()
                .mmsi(tracking.getMmsi())
                .latitude(tracking.getLatitude())
                .longitude(tracking.getLongitude())
                .speed(tracking.getSpeed())
                .course(tracking.getCourse())
                .heading(tracking.getHeading())
                .navStatus(tracking.getNavigationStatus())
                .timestamp(tracking.getTimestamp())
                .build();
    }
}
