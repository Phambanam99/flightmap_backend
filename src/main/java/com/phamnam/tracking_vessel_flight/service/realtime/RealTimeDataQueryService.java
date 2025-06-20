/*
 * RealTimeDataQueryService - Temporarily disabled pending repository method
 * alignment
 * This service will be re-enabled once repository methods are properly defined
 */

// TODO: Re-enable after repository methods are implemented
/*
 * package com.phamnam.tracking_vessel_flight.service.realtime;
 * 
 * import com.phamnam.tracking_vessel_flight.models.FlightTracking;
 * import com.phamnam.tracking_vessel_flight.models.ShipTracking;
 * import
 * com.phamnam.tracking_vessel_flight.repository.FlightTrackingRepository;
 * import com.phamnam.tracking_vessel_flight.repository.ShipTrackingRepository;
 * import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
 * import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
 * import org.springframework.beans.factory.annotation.Autowired;
 * import org.springframework.data.redis.core.RedisTemplate;
 * import org.springframework.stereotype.Service;
 * import org.slf4j.Logger;
 * import org.slf4j.LoggerFactory;
 * 
 * import java.time.LocalDateTime;
 * import java.util.*;
 * import java.util.stream.Collectors;
 * 
 * @Service
 * public class RealTimeDataQueryService {
 * // Implementation will be added after repository alignment
 * }
 */

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
     * Get current flight data from Redis (real-time)
     */
    public FlightTrackingRequest getCurrentFlightData(String hexident) {
        try {
            String flightKey = "flight:" + hexident + ":current";
            FlightTrackingRequest currentData = (FlightTrackingRequest) redisTemplate.opsForValue().get(flightKey);

            if (currentData != null) {
                logger.debug("Found current flight data in Redis for {}", hexident);
                return currentData;
            }

            logger.debug("No current flight data found in Redis for {}", hexident);
            return null;

        } catch (Exception e) {
            logger.error("Error getting current flight data for {}: {}", hexident, e.getMessage());
            return null;
        }
    }

    /**
     * Get current ship data from Redis (real-time)
     */
    public ShipTrackingRequest getCurrentShipData(String mmsi) {
        try {
            String shipKey = "ship:" + mmsi + ":current";
            ShipTrackingRequest currentData = (ShipTrackingRequest) redisTemplate.opsForValue().get(shipKey);

            if (currentData != null) {
                logger.debug("Found current ship data in Redis for {}", mmsi);
                return currentData;
            }

            logger.debug("No current ship data found in Redis for {}", mmsi);
            return null;

        } catch (Exception e) {
            logger.error("Error getting current ship data for {}: {}", mmsi, e.getMessage());
            return null;
        }
    }

    /**
     * Get historical flight data from database
     */
    public List<FlightTracking> getFlightHistory(String hexident, LocalDateTime fromTime, LocalDateTime toTime) {
        try {
            return flightTrackingRepository.findByHexIdentAndLastSeenBetweenOrderByLastSeenAsc(
                    hexident, fromTime, toTime);

        } catch (Exception e) {
            logger.error("Error getting flight history for {}: {}", hexident, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get historical ship data from database
     */
    public List<ShipTracking> getShipHistory(String mmsi, LocalDateTime fromTime, LocalDateTime toTime) {
        try {
            return shipTrackingRepository.findByMmsiAndTimestampBetweenOrderByTimestampAsc(
                    mmsi, fromTime, toTime);

        } catch (Exception e) {
            logger.error("Error getting ship history for {}: {}", mmsi, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get flights in area (simplified - Redis only for now)
     */
    public List<FlightTrackingRequest> getFlightsInArea(Double minLat, Double maxLat, Double minLon, Double maxLon) {
        try {
            Set<String> flightKeys = redisTemplate.keys("flight:*:current");
            List<FlightTrackingRequest> flightsInArea = new ArrayList<>();

            if (flightKeys != null) {
                for (String key : flightKeys) {
                    FlightTrackingRequest flight = (FlightTrackingRequest) redisTemplate.opsForValue().get(key);
                    if (flight != null
                            && isInArea(flight.getLatitude(), flight.getLongitude(), minLat, maxLat, minLon, maxLon)) {
                        flightsInArea.add(flight);
                    }
                }
            }

            logger.debug("Found {} flights in area", flightsInArea.size());
            return flightsInArea;

        } catch (Exception e) {
            logger.error("Error getting flights in area: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get ships in area (simplified - Redis only for now)
     */
    public List<ShipTrackingRequest> getShipsInArea(Double minLat, Double maxLat, Double minLon, Double maxLon) {
        try {
            Set<String> shipKeys = redisTemplate.keys("ship:*:current");
            List<ShipTrackingRequest> shipsInArea = new ArrayList<>();

            if (shipKeys != null) {
                for (String key : shipKeys) {
                    ShipTrackingRequest ship = (ShipTrackingRequest) redisTemplate.opsForValue().get(key);
                    if (ship != null
                            && isInArea(ship.getLatitude(), ship.getLongitude(), minLat, maxLat, minLon, maxLon)) {
                        shipsInArea.add(ship);
                    }
                }
            }

            logger.debug("Found {} ships in area", shipsInArea.size());
            return shipsInArea;

        } catch (Exception e) {
            logger.error("Error getting ships in area: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get recent flight data (last 24 hours)
     */
    public Map<String, Object> getRecentFlightData(String hexident) {
        try {
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            List<FlightTracking> recentHistory = getFlightHistory(hexident, oneDayAgo, LocalDateTime.now());

            Map<String, Object> result = new HashMap<>();
            result.put("hexident", hexident);
            result.put("totalPoints", recentHistory.size());
            result.put("timeRange", "24 hours");
            result.put("trackingPoints", convertFlightTrackingToPoints(recentHistory));

            return result;

        } catch (Exception e) {
            logger.error("Error getting recent flight data for {}: {}", hexident, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Get recent ship data (last 24 hours)
     */
    public Map<String, Object> getRecentShipData(String mmsi) {
        try {
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            List<ShipTracking> recentHistory = getShipHistory(mmsi, oneDayAgo, LocalDateTime.now());

            Map<String, Object> result = new HashMap<>();
            result.put("mmsi", mmsi);
            result.put("totalPoints", recentHistory.size());
            result.put("timeRange", "24 hours");
            result.put("trackingPoints", convertShipTrackingToPoints(recentHistory));

            return result;

        } catch (Exception e) {
            logger.error("Error getting recent ship data for {}: {}", mmsi, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Get system statistics
     */
    public Map<String, Object> getSystemStatistics() {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

            long recentFlightRecords = flightTrackingRepository.countByLastSeenAfter(oneHourAgo);
            long recentShipRecords = shipTrackingRepository.countByTimestampAfter(oneHourAgo);

            Set<String> activeFlights = redisTemplate.keys("flight:*:current");
            Set<String> activeShips = redisTemplate.keys("ship:*:current");

            Map<String, Object> stats = new HashMap<>();
            stats.put("activeFlightsInRedis", activeFlights != null ? activeFlights.size() : 0);
            stats.put("activeShipsInRedis", activeShips != null ? activeShips.size() : 0);
            stats.put("recentFlightRecords", recentFlightRecords);
            stats.put("recentShipRecords", recentShipRecords);
            stats.put("timestamp", LocalDateTime.now());

            return stats;

        } catch (Exception e) {
            logger.error("Error getting system statistics: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // Helper methods

    private boolean isInArea(Double lat, Double lon, Double minLat, Double maxLat, Double minLon, Double maxLon) {
        if (lat == null || lon == null || minLat == null || maxLat == null || minLon == null || maxLon == null) {
            return false;
        }
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    private List<Map<String, Object>> convertFlightTrackingToPoints(List<FlightTracking> trackingList) {
        return trackingList.stream().map(tracking -> {
            Map<String, Object> point = new HashMap<>();
            point.put("latitude", tracking.getLatitude());
            point.put("longitude", tracking.getLongitude());
            point.put("altitude", tracking.getAltitude());
            point.put("speed", tracking.getSpeed());
            point.put("timestamp", tracking.getLastSeen());
            return point;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> convertShipTrackingToPoints(List<ShipTracking> trackingList) {
        return trackingList.stream().map(tracking -> {
            Map<String, Object> point = new HashMap<>();
            point.put("latitude", tracking.getLatitude());
            point.put("longitude", tracking.getLongitude());
            point.put("speed", tracking.getSpeed());
            point.put("course", tracking.getCourse());
            point.put("heading", tracking.getHeading());
            point.put("timestamp", tracking.getTimestamp());
            return point;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> convertFlightRequestToPoints(List<FlightTrackingRequest> requestList) {
        return requestList.stream().map(current -> {
            Map<String, Object> point = new HashMap<>();
            point.put("latitude", current.getLatitude());
            point.put("longitude", current.getLongitude());
            point.put("altitude", current.getAltitude());
            point.put("speed", current.getGroundSpeed());
            point.put("timestamp", current.getLastSeen());
            return point;
        }).collect(Collectors.toList());
    }
}