package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import com.phamnam.tracking_vessel_flight.repository.FlightTrackingRepository;
import com.phamnam.tracking_vessel_flight.repository.ShipTrackingRepository;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RealTimeDataQueryService {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeDataQueryService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FlightTrackingRepository flightTrackingRepository;

    @Autowired
    private ShipTrackingRepository shipTrackingRepository;

    /**
     * Get latest flight tracking data (from Redis)
     */
    public FlightTrackingRequest getLatestFlightData(String hexIdent) {
        try {
            String key = "flight:" + hexIdent + ":current";
            return (FlightTrackingRequest) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("Error getting latest flight data for {}: {}", hexIdent, e.getMessage());
            return null;
        }
    }

    /**
     * Get latest ship tracking data (from Redis)
     */
    public ShipTrackingRequest getLatestShipData(String mmsi) {
        try {
            String key = "ship:" + mmsi + ":current";
            return (ShipTrackingRequest) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("Error getting latest ship data for {}: {}", mmsi, e.getMessage());
            return null;
        }
    }

    /**
     * Get all active flights from Redis
     */
    public List<FlightTrackingRequest> getAllActiveFlights() {
        try {
            Set<String> keys = redisTemplate.keys("flight:*:current");
            if (keys == null || keys.isEmpty()) {
                return new ArrayList<>();
            }

            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            return values.stream()
                    .filter(Objects::nonNull)
                    .map(FlightTrackingRequest.class::cast)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting all active flights: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get all active ships from Redis
     */
    public List<ShipTrackingRequest> getAllActiveShips() {
        try {
            Set<String> keys = redisTemplate.keys("ship:*:current");
            if (keys == null || keys.isEmpty()) {
                return new ArrayList<>();
            }

            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            return values.stream()
                    .filter(Objects::nonNull)
                    .map(ShipTrackingRequest.class::cast)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting all active ships: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get flights in geographic area from Redis
     */
    public List<FlightTrackingRequest> getFlightsInArea(double minLat, double maxLat, double minLon, double maxLon) {
        List<FlightTrackingRequest> allFlights = getAllActiveFlights();

        return allFlights.stream()
                .filter(flight -> isInArea(flight.getLatitude(), flight.getLongitude(), minLat, maxLat, minLon, maxLon))
                .collect(Collectors.toList());
    }

    /**
     * Get ships in geographic area from Redis
     */
    public List<ShipTrackingRequest> getShipsInArea(double minLat, double maxLat, double minLon, double maxLon) {
        List<ShipTrackingRequest> allShips = getAllActiveShips();

        return allShips.stream()
                .filter(ship -> isInArea(ship.getLatitude(), ship.getLongitude(), minLat, maxLat, minLon, maxLon))
                .collect(Collectors.toList());
    }

    /**
     * Get historical flight tracking data from database
     */
    public List<FlightTracking> getFlightHistory(String hexIdent, LocalDateTime fromTime, LocalDateTime toTime) {
        try {
            return flightTrackingRepository.findByHexIdentAndLastSeenBetweenOrderByLastSeenAsc(
                    hexIdent, fromTime, toTime);
        } catch (Exception e) {
            logger.error("Error getting flight history for {}: {}", hexIdent, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get historical ship tracking data from database
     */
    public List<ShipTracking> getShipHistory(String mmsi, LocalDateTime fromTime, LocalDateTime toTime) {
        try {
            return shipTrackingRepository.findByMmsiAndTimestampBetweenOrderByTimestampAsc(
                    mmsi, fromTime, toTime);
        } catch (Exception e) {
            logger.error("Error getting ship history for {}: {}", mmsi, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get recent flight tracking data (combines Redis + Database)
     */
    public Map<String, Object> getRecentFlightData(String hexIdent, int hours) {
        Map<String, Object> result = new HashMap<>();

        // Get latest from Redis
        FlightTrackingRequest latest = getLatestFlightData(hexIdent);
        result.put("latest", latest);

        // Get recent history from database
        LocalDateTime fromTime = LocalDateTime.now().minusHours(hours);
        List<FlightTracking> history = getFlightHistory(hexIdent, fromTime, LocalDateTime.now());
        result.put("history", history);

        // Get activity status
        LocalDateTime lastSeen = (LocalDateTime) redisTemplate.opsForValue()
                .get("flight:" + hexIdent + ":last_seen");
        result.put("lastSeen", lastSeen);
        result.put("isActive", latest != null);

        return result;
    }

    /**
     * Get recent ship tracking data (combines Redis + Database)
     */
    public Map<String, Object> getRecentShipData(String mmsi, int hours) {
        Map<String, Object> result = new HashMap<>();

        // Get latest from Redis
        ShipTrackingRequest latest = getLatestShipData(mmsi);
        result.put("latest", latest);

        // Get recent history from database
        LocalDateTime fromTime = LocalDateTime.now().minusHours(hours);
        List<ShipTracking> history = getShipHistory(mmsi, fromTime, LocalDateTime.now());
        result.put("history", history);

        // Get activity status
        LocalDateTime lastSeen = (LocalDateTime) redisTemplate.opsForValue()
                .get("ship:" + mmsi + ":last_seen");
        result.put("lastSeen", lastSeen);
        result.put("isActive", latest != null);

        return result;
    }

    /**
     * Check if vehicle is currently active (last seen within threshold)
     */
    public boolean isVehicleActive(String vehicleType, String identifier, int thresholdMinutes) {
        try {
            String key = vehicleType + ":" + identifier + ":last_seen";
            LocalDateTime lastSeen = (LocalDateTime) redisTemplate.opsForValue().get(key);

            if (lastSeen == null)
                return false;

            LocalDateTime threshold = LocalDateTime.now().minusMinutes(thresholdMinutes);
            return lastSeen.isAfter(threshold);

        } catch (Exception e) {
            logger.error("Error checking vehicle activity for {}:{}: {}", vehicleType, identifier, e.getMessage());
            return false;
        }
    }

    /**
     * Get system statistics
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Redis statistics
            Set<String> flightKeys = redisTemplate.keys("flight:*:current");
            Set<String> shipKeys = redisTemplate.keys("ship:*:current");

            stats.put("activeFlights", flightKeys != null ? flightKeys.size() : 0);
            stats.put("activeShips", shipKeys != null ? shipKeys.size() : 0);

            // Database statistics (recent data)
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long recentFlightRecords = flightTrackingRepository.countByLastSeenAfter(oneHourAgo);
            long recentShipRecords = shipTrackingRepository.countByTimestampAfter(oneHourAgo);

            stats.put("recentFlightRecords", recentFlightRecords);
            stats.put("recentShipRecords", recentShipRecords);
            stats.put("generatedAt", LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Error getting system statistics: {}", e.getMessage());
            stats.put("error", "Statistics unavailable: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Get tracking trail for a flight (combines real-time + historical)
     */
    public List<Map<String, Object>> getFlightTrail(String hexIdent, int hours) {
        List<Map<String, Object>> trail = new ArrayList<>();

        try {
            // Get historical points from database
            LocalDateTime fromTime = LocalDateTime.now().minusHours(hours);
            List<FlightTracking> history = getFlightHistory(hexIdent, fromTime, LocalDateTime.now());

            for (FlightTracking tracking : history) {
                Map<String, Object> point = new HashMap<>();
                point.put("latitude", tracking.getLatitude());
                point.put("longitude", tracking.getLongitude());
                point.put("altitude", tracking.getAltitude());
                point.put("timestamp", tracking.getLastSeen());
                point.put("source", "database");
                trail.add(point);
            }

            // Add current position from Redis if available
            FlightTrackingRequest current = getLatestFlightData(hexIdent);
            if (current != null) {
                Map<String, Object> point = new HashMap<>();
                point.put("latitude", current.getLatitude());
                point.put("longitude", current.getLongitude());
                point.put("altitude", current.getAltitude());
                point.put("timestamp", current.getLastSeen());
                point.put("source", "redis");
                trail.add(point);
            }

        } catch (Exception e) {
            logger.error("Error getting flight trail for {}: {}", hexIdent, e.getMessage());
        }

        return trail;
    }

    private boolean isInArea(Double lat, Double lon, double minLat, double maxLat, double minLon, double maxLon) {
        return lat != null && lon != null &&
                lat >= minLat && lat <= maxLat &&
                lon >= minLon && lon <= maxLon;
    }
}