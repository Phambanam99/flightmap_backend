package com.phamnam.tracking_vessel_flight.service;

import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String FLIGHT_KEY_PREFIX = "flight:";
    private static final String VESSEL_KEY_PREFIX = "vessel:";
    private static final int CACHE_TTL_HOURS = 24;

    // Hot storage cho flight tracking
    public void cacheFlightTracking(FlightTrackingRequest tracking) {
        String key = FLIGHT_KEY_PREFIX + tracking.getFlightId();
        log.info("Caching flight tracking data with key: {}", key);

        // Lưu dữ liệu vào Redis với TTL
        redisTemplate.opsForValue().set(key, tracking, CACHE_TTL_HOURS, TimeUnit.HOURS);

        // Cập nhật danh sách các flights đang hoạt động
        redisTemplate.opsForSet().add("flights:active", tracking.getFlightId().toString());
        redisTemplate.expire("flights:active", CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    // Lấy dữ liệu flight từ Redis
    public FlightTrackingRequest getFlightTracking(Long flightId) {
        String key = FLIGHT_KEY_PREFIX + flightId;
        return (FlightTrackingRequest) redisTemplate.opsForValue().get(key);
    }

    // Lấy danh sách các flights đang hoạt động
    @SuppressWarnings("unchecked")
    public Set<Object> getActiveFlights() {
        return redisTemplate.opsForSet().members("flights:active");
    }

    // Xóa dữ liệu flight khi không còn hoạt động
    public void removeFlightFromCache(Long flightId) {
        String key = FLIGHT_KEY_PREFIX + flightId;
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove("flights:active", flightId.toString());
    }
    // Trong một scheduled task hoặc service nào đó
@Scheduled(fixedRate = 300000) // 5 phút
public void cleanupInactiveFlights() {
    Set<Object> activeFlights = getActiveFlights();
    LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
    
    for (Object flightIdObj : activeFlights) {
        Long flightId = Long.parseLong(flightIdObj.toString());
        FlightTrackingRequest lastPosition = getFlightTracking(flightId);
        
        if (lastPosition != null && 
            lastPosition.getUpdateTime().isBefore(cutoffTime)) {
            log.info("Flight {} inactive for 30+ minutes, removing from cache", flightId);
            removeFlightFromCache(flightId);
        }
    }
}

    // Tương tự cho vessel tracking nếu cần
}