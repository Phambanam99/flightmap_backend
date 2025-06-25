package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class RawDataFilteringService {

    @Value("${raw.data.sampling.enabled:false}")
    private boolean samplingEnabled;

    @Value("${raw.data.sampling.rate:0.05}")
    private double samplingRate;

    @Value("${raw.data.smart-filtering.enabled:false}")
    private boolean smartFilteringEnabled;

    @Value("${raw.data.smart-filtering.min-distance-change:500}")
    private double minDistanceChangeMeters;

    @Value("${raw.data.smart-filtering.min-altitude-change:500}")
    private double minAltitudeChangeFeet;

    // Cache for previous data to enable smart filtering
    private final Map<String, AircraftTrackingRequest> previousAircraftData = new ConcurrentHashMap<>();
    private final Map<String, VesselTrackingRequest> previousVesselData = new ConcurrentHashMap<>();

    /**
     * Check if aircraft raw data should be stored based on sampling and filtering
     * rules
     */
    public boolean shouldStoreAircraftRawData(AircraftTrackingRequest current) {
        try {
            // Step 1: Check sampling
            if (!passesSampling()) {
                log.trace("Aircraft {} filtered out by sampling", current.getHexident());
                return false;
            }

            // Step 2: Check smart filtering
            if (!passesSmartFiltering(current)) {
                log.trace("Aircraft {} filtered out by smart filtering", current.getHexident());
                return false;
            }

            // Update cache for next comparison
            previousAircraftData.put(current.getHexident(), current);

            log.debug("Aircraft {} passed all filters - will store raw data", current.getHexident());
            return true;

        } catch (Exception e) {
            log.error("Error in aircraft filtering for {}: {}", current.getHexident(), e.getMessage());
            return true; // Default to store on error
        }
    }

    /**
     * Check if vessel raw data should be stored based on sampling and filtering
     * rules
     */
    public boolean shouldStoreVesselRawData(VesselTrackingRequest current) {
        try {
            // Step 1: Check sampling
            if (!passesSampling()) {
                log.trace("Vessel {} filtered out by sampling", current.getMmsi());
                return false;
            }

            // Step 2: Check smart filtering
            if (!passesVesselSmartFiltering(current)) {
                log.trace("Vessel {} filtered out by smart filtering", current.getMmsi());
                return false;
            }

            // Update cache for next comparison
            previousVesselData.put(current.getMmsi(), current);

            log.debug("Vessel {} passed all filters - will store raw data", current.getMmsi());
            return true;

        } catch (Exception e) {
            log.error("Error in vessel filtering for {}: {}", current.getMmsi(), e.getMessage());
            return true; // Default to store on error
        }
    }

    /**
     * Check if data passes sampling rules
     */
    private boolean passesSampling() {
        if (!samplingEnabled) {
            return true; // No sampling, store all
        }

        boolean passes = ThreadLocalRandom.current().nextDouble() < samplingRate;
        if (passes) {
            log.trace("Data passed sampling (rate: {})", samplingRate);
        }
        return passes;
    }

    /**
     * Check if aircraft data passes smart filtering rules
     */
    private boolean passesSmartFiltering(AircraftTrackingRequest current) {
        if (!smartFilteringEnabled) {
            return true; // No smart filtering, store all
        }

        AircraftTrackingRequest previous = previousAircraftData.get(current.getHexident());
        if (previous == null) {
            log.trace("No previous data for aircraft {} - storing", current.getHexident());
            return true; // Always store first occurrence
        }

        // Check for emergency conditions (always store)
        if (isEmergencyAircraft(current)) {
            log.info("Emergency aircraft {} detected - forcing storage", current.getHexident());
            return true;
        }

        // Check significant position change
        if (hasSignificantPositionChange(
                previous.getLatitude(), previous.getLongitude(),
                current.getLatitude(), current.getLongitude())) {
            log.trace("Significant position change for aircraft {}", current.getHexident());
            return true;
        }

        // Check significant altitude change
        if (hasSignificantAltitudeChange(
                previous.getAltitude() != null ? previous.getAltitude().doubleValue() : null,
                current.getAltitude() != null ? current.getAltitude().doubleValue() : null)) {
            log.trace("Significant altitude change for aircraft {}", current.getHexident());
            return true;
        }

        // Check significant speed change
        if (hasSignificantSpeedChange(
                previous.getGroundSpeed() != null ? previous.getGroundSpeed().doubleValue() : null,
                current.getGroundSpeed() != null ? current.getGroundSpeed().doubleValue() : null)) {
            log.trace("Significant speed change for aircraft {}", current.getHexident());
            return true;
        }

        // Check data quality change
        if (hasSignificantDataQualityChange(previous.getDataQuality(), current.getDataQuality())) {
            log.trace("Significant data quality change for aircraft {}", current.getHexident());
            return true;
        }

        return false; // No significant changes
    }

    /**
     * Check if vessel data passes smart filtering rules
     */
    private boolean passesVesselSmartFiltering(VesselTrackingRequest current) {
        if (!smartFilteringEnabled) {
            return true; // No smart filtering, store all
        }

        VesselTrackingRequest previous = previousVesselData.get(current.getMmsi());
        if (previous == null) {
            log.trace("No previous data for vessel {} - storing", current.getMmsi());
            return true; // Always store first occurrence
        }

        // Check for emergency/security conditions (always store)
        if (isEmergencyVessel(current)) {
            log.info("Emergency vessel {} detected - forcing storage", current.getMmsi());
            return true;
        }

        // Check significant position change
        if (hasSignificantPositionChange(
                previous.getLatitude(), previous.getLongitude(),
                current.getLatitude(), current.getLongitude())) {
            log.trace("Significant position change for vessel {}", current.getMmsi());
            return true;
        }

        // Check significant speed change
        if (hasSignificantSpeedChange(previous.getSpeed(), current.getSpeed())) {
            log.trace("Significant speed change for vessel {}", current.getMmsi());
            return true;
        }

        // Check significant course change
        if (hasSignificantCourseChange(
                previous.getCourse() != null ? previous.getCourse().doubleValue() : null,
                current.getCourse() != null ? current.getCourse().doubleValue() : null)) {
            log.trace("Significant course change for vessel {}", current.getMmsi());
            return true;
        }

        // Check navigation status change
        if (hasStatusChange(previous.getNavigationStatus(), current.getNavigationStatus())) {
            log.trace("Navigation status change for vessel {}", current.getMmsi());
            return true;
        }

        return false; // No significant changes
    }

    /**
     * Check if aircraft is in emergency state
     */
    private boolean isEmergencyAircraft(AircraftTrackingRequest aircraft) {
        String squawk = aircraft.getSquawk();
        return squawk != null && (squawk.equals("7500") || squawk.equals("7600") || squawk.equals("7700"));
    }

    /**
     * Check if vessel is in emergency/security state
     */
    private boolean isEmergencyVessel(VesselTrackingRequest vessel) {
        String status = vessel.getNavigationStatus();
        return status != null && (status.toLowerCase().contains("distress") ||
                status.toLowerCase().contains("emergency") ||
                status.toLowerCase().contains("security") ||
                status.toLowerCase().contains("piracy")) ||
                (vessel.getSecurityAlert() != null && vessel.getSecurityAlert());
    }

    /**
     * Check if position change is significant
     */
    private boolean hasSignificantPositionChange(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return true; // Missing coordinates, consider significant
        }

        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance > minDistanceChangeMeters;
    }

    /**
     * Check if altitude change is significant
     */
    private boolean hasSignificantAltitudeChange(Double alt1, Double alt2) {
        if (alt1 == null || alt2 == null) {
            return true; // Missing altitude, consider significant
        }

        double altitudeChange = Math.abs(alt1 - alt2);
        return altitudeChange > minAltitudeChangeFeet;
    }

    /**
     * Check if speed change is significant
     */
    private boolean hasSignificantSpeedChange(Double speed1, Double speed2) {
        if (speed1 == null || speed2 == null) {
            return true; // Missing speed, consider significant
        }

        double speedChange = Math.abs(speed1 - speed2);
        return speedChange > 10.0; // 10 knots threshold
    }

    /**
     * Check if course change is significant
     */
    private boolean hasSignificantCourseChange(Double course1, Double course2) {
        if (course1 == null || course2 == null) {
            return true; // Missing course, consider significant
        }

        double diff = Math.abs(course1 - course2);
        double courseChange = Math.min(diff, 360 - diff); // Handle wrap-around
        return courseChange > 30.0; // 30 degrees threshold
    }

    /**
     * Check if data quality change is significant
     */
    private boolean hasSignificantDataQualityChange(Double quality1, Double quality2) {
        if (quality1 == null || quality2 == null) {
            return true; // Missing quality, consider significant
        }

        double qualityChange = Math.abs(quality1 - quality2);
        return qualityChange > 0.1; // 10% threshold
    }

    /**
     * Check if status has changed
     */
    private boolean hasStatusChange(String status1, String status2) {
        if (status1 == null && status2 == null)
            return false;
        if (status1 == null || status2 == null)
            return true;
        return !status1.equals(status2);
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // Earth's radius in meters

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * Get filtering statistics
     */
    public Map<String, Object> getFilteringStats() {
        return Map.of(
                "samplingEnabled", samplingEnabled,
                "samplingRate", samplingRate,
                "smartFilteringEnabled", smartFilteringEnabled,
                "minDistanceChangeMeters", minDistanceChangeMeters,
                "minAltitudeChangeFeet", minAltitudeChangeFeet,
                "cachedAircraftCount", previousAircraftData.size(),
                "cachedVesselCount", previousVesselData.size());
    }

    /**
     * Clear cached data (useful for testing or memory management)
     */
    public void clearCache() {
        previousAircraftData.clear();
        previousVesselData.clear();
        log.info("Raw data filtering cache cleared");
    }
}