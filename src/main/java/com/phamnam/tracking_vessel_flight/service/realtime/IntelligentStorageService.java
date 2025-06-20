/*
 * IntelligentStorageService - Temporarily disabled pending DTO alignment
 * 
 * This service will be re-enabled once DTOs are properly structured to support:
 * - Flight tracking with hexIdent, squawk, groundSpeed fields
 * - Ship tracking with mmsi field
 * - Compatible field types (Double vs Float consistency)
 */

// TODO: Re-enable after DTO restructuring
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
 * import java.time.temporal.ChronoUnit;
 * import java.util.Set;
 * import java.util.concurrent.TimeUnit;
 * 
 * @Service
 * public class IntelligentStorageService {
 * 
 * private static final Logger logger =
 * LoggerFactory.getLogger(IntelligentStorageService.class);
 * 
 * // Thresholds for intelligent storage decisions
 * private static final double POSITION_THRESHOLD_METERS = 100.0;
 * private static final double ALTITUDE_THRESHOLD_FEET = 500.0;
 * private static final double SPEED_THRESHOLD_KNOTS = 10.0;
 * private static final double COURSE_THRESHOLD_DEGREES = 30.0;
 * private static final long FORCE_SAVE_INTERVAL_SECONDS = 60;
 * 
 * // Emergency squawk codes that always trigger save
 * private static final Set<String> EMERGENCY_SQUAWK_CODES = Set.of("7500",
 * "7600", "7700");
 * 
 * @Autowired
 * private RedisTemplate<String, Object> redisTemplate;
 * 
 * @Autowired
 * private FlightTrackingRepository flightTrackingRepository;
 * 
 * @Autowired
 * private ShipTrackingRepository shipTrackingRepository;
 * 
 * // Implementation methods here...
 * }
 */