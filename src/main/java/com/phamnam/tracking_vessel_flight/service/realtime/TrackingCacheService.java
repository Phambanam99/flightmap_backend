package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String FLIGHT_TRACKING_PREFIX = "flight:tracking:";
    private static final String ACTIVE_FLIGHTS_KEY = "flight:active";
    private static final Duration FLIGHT_INACTIVITY_THRESHOLD = Duration.ofMinutes(5);

    /**
     * Caches flight tracking data in Redis and adds to active flights
     */
    public void cacheFlightTracking(FlightTrackingRequestDTO tracking) {
        String key = FLIGHT_TRACKING_PREFIX + tracking.getId();
        redisTemplate.opsForValue().set(key, tracking);
        redisTemplate.opsForSet().add(ACTIVE_FLIGHTS_KEY, tracking.getId().toString());
        log.debug("Cached flight tracking for flight ID: {} and added to active flights", tracking.getId());
    }

    /**
     * Retrieves flight tracking data from Redis
     */
    public FlightTrackingRequestDTO getFlightTracking(Long flightId) {
        String key = FLIGHT_TRACKING_PREFIX + flightId;
        FlightTrackingRequestDTO tracking = (FlightTrackingRequestDTO) redisTemplate.opsForValue().get(key);
        if (tracking == null) {
            log.debug("No cached tracking found for flight ID: {}", flightId);
        }
        return tracking;
    }

    /**
     * Removes flight tracking data from Redis and active flights set
     */
    public void removeFlightTracking(Long flightId) {
        String key = FLIGHT_TRACKING_PREFIX + flightId;
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(ACTIVE_FLIGHTS_KEY, flightId.toString());
        log.debug("Removed cached tracking for flight ID: {} and removed from active flights", flightId);
    }

    /**
     * Retrieves all active flights from the cache
     * 
     * @return Set of FlightTrackingRequest objects currently in cache
     */
    public Set<Object> getActiveFlights() {
        log.debug("Retrieving all active flights from cache");
        Set<Object> members = redisTemplate.opsForSet().members(ACTIVE_FLIGHTS_KEY);
        if (members == null || members.isEmpty()) {
            return Set.of();
        }

        Set<Object> activeFlights = new HashSet<>();
        for (Object flightIdObj : members) {
            String flightId = flightIdObj.toString();
            Object tracking = redisTemplate.opsForValue().get(FLIGHT_TRACKING_PREFIX + flightId);
            if (tracking != null) {
                activeFlights.add(tracking);
            } else {
                // If tracking data is missing but flight ID is in active set, clean it up
                redisTemplate.opsForSet().remove(ACTIVE_FLIGHTS_KEY, flightId);
            }
        }

        log.debug("Found {} active flights in cache", activeFlights.size());
        return activeFlights;
    }

    /**
     * Scheduled task to clean up inactive flights
     */
    // @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanupInactiveFlights() {
        log.debug("Running cleanup of inactive flights");
        Set<Object> activeFlightIds = redisTemplate.opsForSet().members(ACTIVE_FLIGHTS_KEY);
        if (activeFlightIds == null || activeFlightIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Object flightIdObj : activeFlightIds) {
            String flightId = flightIdObj.toString();
            String key = FLIGHT_TRACKING_PREFIX + flightId;
            FlightTrackingRequest tracking = (FlightTrackingRequest) redisTemplate.opsForValue().get(key);

            if (tracking == null) {
                // Remove from active set if tracking data is missing
                redisTemplate.opsForSet().remove(ACTIVE_FLIGHTS_KEY, flightId);
                continue;
            }

            if (tracking.getUpdateTime() != null) {
                Duration inactiveDuration = Duration.between(tracking.getUpdateTime(), now);
                if (inactiveDuration.compareTo(FLIGHT_INACTIVITY_THRESHOLD) > 0) {
                    // Remove inactive flight from both tracking and active set
                    redisTemplate.delete(key);
                    redisTemplate.opsForSet().remove(ACTIVE_FLIGHTS_KEY, flightId);
                    log.info("Removed inactive flight: {} (inactive for {} minutes)",
                            tracking.getFlightId(), inactiveDuration.toMinutes());
                }
            }
        }
    }
}