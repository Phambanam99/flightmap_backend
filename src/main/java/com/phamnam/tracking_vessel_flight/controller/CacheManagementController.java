package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import com.phamnam.tracking_vessel_flight.repository.ShipTrackingRepository;
import com.phamnam.tracking_vessel_flight.service.realtime.TrackingCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing cache operations and data loading
 */
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheManagementController {

    private final ShipTrackingRepository shipTrackingRepository;
    private final TrackingCacheService trackingCacheService;

    /**
     * Load recent vessel data from database into Redis cache
     */
    @PostMapping("/vessels/load")
    public ResponseEntity<Map<String, Object>> loadVesselDataToCache(
            @RequestParam(defaultValue = "1000") int limit,
            @RequestParam(defaultValue = "2") int hoursBack) {
        
        try {
            log.info("🔄 Loading vessel data to cache - limit: {}, hours back: {}", limit, hoursBack);
            
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursBack);
            
            // Get recent vessel tracking data from database
            List<ShipTracking> recentVessels = shipTrackingRepository
                .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp")))
                .getContent()
                .stream()
                .filter(vessel -> vessel.getTimestamp() != null && vessel.getTimestamp().isAfter(cutoffTime))
                .toList();

            log.info("📊 Found {} recent vessel records from database", recentVessels.size());

            int cached = 0;
            int failed = 0;

            for (ShipTracking vessel : recentVessels) {
                try {
                    // Convert to cache format
                    ShipTrackingRequest request = ShipTrackingRequest.builder()
                            .mmsi(vessel.getMmsi())
                            .latitude(vessel.getLatitude())
                            .longitude(vessel.getLongitude())
                            .speed(vessel.getSpeed())
                            .course(vessel.getCourse())
                            .heading(vessel.getHeading())
                            .navStatus(vessel.getNavigationStatus())
                            .timestamp(vessel.getTimestamp())
                            .build();

                    // Cache in Redis
                    trackingCacheService.cacheShipTracking(request);
                    cached++;
                    
                } catch (Exception e) {
                    log.warn("⚠️ Failed to cache vessel MMSI {}: {}", vessel.getMmsi(), e.getMessage());
                    failed++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Vessel data loaded to cache successfully");
            result.put("totalFromDb", recentVessels.size());
            result.put("cached", cached);
            result.put("failed", failed);
            result.put("timestamp", LocalDateTime.now());

            log.info("✅ Vessel cache loading completed - cached: {}, failed: {}", cached, failed);
            
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Failed to load vessel data to cache: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to load vessel data: " + e.getMessage());
            error.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            // Get cached vessel count
            var activeShips = trackingCacheService.getActiveShips();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("activeShipsInCache", activeShips.size());
            stats.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ Failed to get cache stats: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get cache stats: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
