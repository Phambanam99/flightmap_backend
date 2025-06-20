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
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class IntelligentStorageService {

    private static final Logger logger = LoggerFactory.getLogger(IntelligentStorageService.class);

    // Thresholds for intelligent storage decisions
    private static final double POSITION_THRESHOLD_METERS = 100.0;
    private static final double ALTITUDE_THRESHOLD_FEET = 500.0;
    private static final double SPEED_THRESHOLD_KNOTS = 10.0;
    private static final double COURSE_THRESHOLD_DEGREES = 30.0;
    private static final long FORCE_SAVE_INTERVAL_SECONDS = 60;

    // Emergency squawk codes that always trigger save
    private static final Set<String> EMERGENCY_SQUAWK_CODES = Set.of("7500", "7600", "7700");

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FlightTrackingRepository flightTrackingRepository;

    @Autowired
    private ShipTrackingRepository shipTrackingRepository;

    /**
     * Process flight tracking data with intelligent storage decisions
     */
    public boolean processFlightTracking(FlightTrackingRequest request) {
        String flightKey = "flight:" + request.getHexIdent();

        try {
            // Store current data in Redis (always)
            storeCurrentFlightData(flightKey, request);

            // Check if should save to database
            boolean shouldSave = shouldSaveFlightToDatabase(flightKey, request);

            if (shouldSave) {
                saveFlightToDatabase(request);
                updateLastDbSave(flightKey);
                logger.debug("Flight {} saved to database - triggered by threshold", request.getHexIdent());
            } else {
                logger.debug("Flight {} stored in Redis only - no significant change", request.getHexIdent());
            }

            return shouldSave;

        } catch (Exception e) {
            logger.error("Error processing flight tracking for {}: {}", request.getHexIdent(), e.getMessage());
            return false;
        }
    }

    /**
     * Process ship tracking data with intelligent storage decisions
     */
    public boolean processShipTracking(ShipTrackingRequest request) {
        String shipKey = "ship:" + request.getMmsi();

        try {
            // Store current data in Redis (always)
            storeCurrentShipData(shipKey, request);

            // Check if should save to database
            boolean shouldSave = shouldSaveShipToDatabase(shipKey, request);

            if (shouldSave) {
                saveShipToDatabase(request);
                updateLastDbSave(shipKey);
                logger.debug("Ship {} saved to database - triggered by threshold", request.getMmsi());
            } else {
                logger.debug("Ship {} stored in Redis only - no significant change", request.getMmsi());
            }

            return shouldSave;

        } catch (Exception e) {
            logger.error("Error processing ship tracking for {}: {}", request.getMmsi(), e.getMessage());
            return false;
        }
    }

    private void storeCurrentFlightData(String flightKey, FlightTrackingRequest request) {
        // Move current to previous
        Object current = redisTemplate.opsForValue().get(flightKey + ":current");
        if (current != null) {
            redisTemplate.opsForValue().set(flightKey + ":previous", current, 1, TimeUnit.HOURS);
        }

        // Store new current data
        redisTemplate.opsForValue().set(flightKey + ":current", request, 1, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(flightKey + ":last_seen", LocalDateTime.now(), 1, TimeUnit.HOURS);
    }

    private void storeCurrentShipData(String shipKey, ShipTrackingRequest request) {
        // Move current to previous
        Object current = redisTemplate.opsForValue().get(shipKey + ":current");
        if (current != null) {
            redisTemplate.opsForValue().set(shipKey + ":previous", current, 1, TimeUnit.HOURS);
        }

        // Store new current data
        redisTemplate.opsForValue().set(shipKey + ":current", request, 1, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(shipKey + ":last_seen", LocalDateTime.now(), 1, TimeUnit.HOURS);
    }

    private boolean shouldSaveFlightToDatabase(String flightKey, FlightTrackingRequest current) {
        try {
            // Get previous data
            FlightTrackingRequest previous = (FlightTrackingRequest) redisTemplate.opsForValue()
                    .get(flightKey + ":previous");

            // Always save if no previous data
            if (previous == null) {
                return true;
            }

            // Check emergency squawk codes
            if (current.getSquawk() != null && EMERGENCY_SQUAWK_CODES.contains(current.getSquawk())) {
                logger.warn("Emergency squawk {} detected for flight {}", current.getSquawk(), current.getHexIdent());
                return true;
            }

            // Check force save interval
            LocalDateTime lastDbSave = (LocalDateTime) redisTemplate.opsForValue().get(flightKey + ":last_db_save");
            if (lastDbSave == null
                    || ChronoUnit.SECONDS.between(lastDbSave, LocalDateTime.now()) >= FORCE_SAVE_INTERVAL_SECONDS) {
                return true;
            }

            // Check position change
            if (hasSignificantPositionChange(
                    previous.getLatitude(), previous.getLongitude(),
                    current.getLatitude(), current.getLongitude())) {
                return true;
            }

            // Check altitude change
            if (hasSignificantAltitudeChange(previous.getAltitude(), current.getAltitude())) {
                return true;
            }

            // Check speed change
            if (hasSignificantSpeedChange(previous.getGroundSpeed(), current.getGroundSpeed())) {
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("Error checking save criteria for flight: {}", e.getMessage());
            return true; // Default to save on error
        }
    }

    private boolean shouldSaveShipToDatabase(String shipKey, ShipTrackingRequest current) {
        try {
            // Get previous data
            ShipTrackingRequest previous = (ShipTrackingRequest) redisTemplate.opsForValue()
                    .get(shipKey + ":previous");

            // Always save if no previous data
            if (previous == null) {
                return true;
            }

            // Check force save interval
            LocalDateTime lastDbSave = (LocalDateTime) redisTemplate.opsForValue().get(shipKey + ":last_db_save");
            if (lastDbSave == null
                    || ChronoUnit.SECONDS.between(lastDbSave, LocalDateTime.now()) >= FORCE_SAVE_INTERVAL_SECONDS) {
                return true;
            }

            // Check position change
            if (hasSignificantPositionChange(
                    previous.getLatitude(), previous.getLongitude(),
                    current.getLatitude(), current.getLongitude())) {
                return true;
            }

            // Check speed change
            if (hasSignificantSpeedChange(previous.getSpeed(), current.getSpeed())) {
                return true;
            }

            // Check course change
            if (hasSignificantCourseChange(previous.getCourse(), current.getCourse())) {
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("Error checking save criteria for ship: {}", e.getMessage());
            return true; // Default to save on error
        }
    }

    private boolean hasSignificantPositionChange(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null)
            return true;

        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance > POSITION_THRESHOLD_METERS;
    }

    private boolean hasSignificantAltitudeChange(Double alt1, Double alt2) {
        if (alt1 == null || alt2 == null)
            return true;
        return Math.abs(alt1 - alt2) > ALTITUDE_THRESHOLD_FEET;
    }

    private boolean hasSignificantSpeedChange(Double speed1, Double speed2) {
        if (speed1 == null || speed2 == null)
            return true;
        return Math.abs(speed1 - speed2) > SPEED_THRESHOLD_KNOTS;
    }

    private boolean hasSignificantCourseChange(Double course1, Double course2) {
        if (course1 == null || course2 == null)
            return true;
        double diff = Math.abs(course1 - course2);
        return Math.min(diff, 360 - diff) > COURSE_THRESHOLD_DEGREES;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in meters

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private void saveFlightToDatabase(FlightTrackingRequest request) {
        FlightTracking entity = new FlightTracking();
        entity.setHexident(request.getHexIdent());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setAltitude(request.getAltitude() != null ? request.getAltitude().floatValue() : null);
        entity.setSpeed(request.getGroundSpeed() != null ? request.getGroundSpeed().floatValue() : null);
        entity.setTrack(request.getTrack() != null ? Float.parseFloat(request.getTrack()) : null);
        entity.setVerticalSpeed(request.getVerticalRate() != null ? request.getVerticalRate().floatValue() : null);
        entity.setSquawk(request.getSquawk() != null ? Integer.parseInt(request.getSquawk()) : null);
        entity.setCallsign(request.getCallsign());
        entity.setTimestamp(request.getLastSeen() != null ? request.getLastSeen() : LocalDateTime.now());
        entity.setLastSeen(request.getLastSeen() != null ? request.getLastSeen() : LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        flightTrackingRepository.save(entity);
    }

    private void saveShipToDatabase(ShipTrackingRequest request) {
        ShipTracking entity = new ShipTracking();
        entity.setMmsi(request.getMmsi());
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setSpeed(request.getSpeed());
        entity.setCourse(request.getCourse());
        entity.setHeading(request.getHeading());
        entity.setNavigationStatus(request.getNavStatus());
        entity.setTimestamp(request.getTimestamp());
        entity.setUpdateTime(LocalDateTime.now());

        shipTrackingRepository.save(entity);
    }

    private void updateLastDbSave(String key) {
        redisTemplate.opsForValue().set(key + ":last_db_save", LocalDateTime.now(), 1, TimeUnit.HOURS);
    }

    /**
     * Get statistics for monitoring
     */
    public String getStorageStatistics() {
        try {
            Set<String> flightKeys = redisTemplate.keys("flight:*:current");
            Set<String> shipKeys = redisTemplate.keys("ship:*:current");

            int activeFlights = flightKeys != null ? flightKeys.size() : 0;
            int activeShips = shipKeys != null ? shipKeys.size() : 0;

            return String.format("Active vehicles in Redis - Flights: %d, Ships: %d", activeFlights, activeShips);

        } catch (Exception e) {
            logger.error("Error getting storage statistics: {}", e.getMessage());
            return "Statistics unavailable";
        }
    }
}