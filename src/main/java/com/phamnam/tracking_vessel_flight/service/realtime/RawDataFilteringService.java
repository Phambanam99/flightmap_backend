package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import lombok.Data;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RawDataFilteringService {

    @Value("${raw.data.sampling.enabled:true}")
    private boolean samplingEnabled;

    @Value("${raw.data.sampling.rate:0.05}")
    private double samplingRate;

    @Value("${raw.data.smart-filtering.enabled:true}")
    private boolean smartFilteringEnabled;

    /**
     * Determine if aircraft raw data should be stored
     */
    public boolean shouldStoreAircraftRawData(AircraftTrackingRequest aircraft) {
        if (aircraft == null) {
            return false;
        }

        // Always store if basic validation fails (for debugging)
        if (!isValidBasicAircraft(aircraft)) {
            log.debug("Storing invalid aircraft data for analysis: {}", aircraft.getHexident());
            return true;
        }

        // Apply sampling if enabled
        if (samplingEnabled && Math.random() > samplingRate) {
            return false;
        }

        return true;
    }

    /**
     * Determine if vessel raw data should be stored
     */
    public boolean shouldStoreVesselRawData(VesselTrackingRequest vessel) {
        if (vessel == null) {
            return false;
        }

        // Always store if basic validation fails (for debugging)
        if (!isValidBasicVessel(vessel)) {
            log.debug("Storing invalid vessel data for analysis: {}", vessel.getMmsi());
            return true;
        }

        // Apply sampling if enabled
        if (samplingEnabled && Math.random() > samplingRate) {
            return false;
        }

        return true;
    }

    private boolean isValidBasicAircraft(AircraftTrackingRequest aircraft) {
        return aircraft.getHexident() != null &&
                aircraft.getLatitude() != null &&
                aircraft.getLongitude() != null &&
                aircraft.getLatitude() >= -90 && aircraft.getLatitude() <= 90 &&
                aircraft.getLongitude() >= -180 && aircraft.getLongitude() <= 180;
    }

    private boolean isValidBasicVessel(VesselTrackingRequest vessel) {
        return vessel.getMmsi() != null &&
                vessel.getLatitude() != null &&
                vessel.getLongitude() != null &&
                vessel.getLatitude() >= -90 && vessel.getLatitude() <= 90 &&
                vessel.getLongitude() >= -180 && vessel.getLongitude() <= 180;
    }

    /**
     * Get filtering statistics
     */
    public FilteringStats getFilteringStats() {
        return FilteringStats.builder()
                .samplingEnabled(samplingEnabled)
                .samplingRate(samplingRate)
                .smartFilteringEnabled(smartFilteringEnabled)
                .build();
    }

    /**
     * Clear filtering cache (placeholder for future implementation)
     */
    public void clearCache() {
        log.info("Filtering cache cleared (no cache implemented yet)");
    }

    @Builder
    @Data
    public static class FilteringStats {
        private boolean samplingEnabled;
        private double samplingRate;
        private boolean smartFilteringEnabled;
    }
}