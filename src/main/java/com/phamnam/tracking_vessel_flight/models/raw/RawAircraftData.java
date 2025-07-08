package com.phamnam.tracking_vessel_flight.models.raw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Raw aircraft data from external APIs with metadata
 * This model preserves original data structure while adding tracking metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawAircraftData {

    // Metadata fields
    private String source; // "flightradar24", "adsbexchange", etc.
    private String apiEndpoint; // Original API endpoint
    private LocalDateTime fetchTime; // When data was fetched
    private Long responseTimeMs; // API response time
    private String requestId; // Unique request identifier
    private Double dataQuality; // Data quality score (0.0-1.0)

    // Original aircraft data fields - using Object to preserve original types
    private String hexident;
    private String callsign;
    private Double latitude;
    private Double longitude;
    private Integer altitude;
    private Integer groundSpeed;
    private Integer track; // Heading/direction
    private Integer verticalRate;
    private String squawk;
    private String aircraftType;
    private String registration;
    private Boolean onGround;
    private Boolean emergency;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDateTime timestamp;

    // Additional fields that might come from different APIs
    private Object rawData; // Original JSON/XML data as received
    private String manufacturer;
    private String model;
    private Integer year;
    private String operator;
    private String operatorCode;
    private String country;
    private Boolean isMilitary;
    private String engineType;
    private Integer engines;
    private String transponderType;

    // Processing status
    private Boolean processed; // Whether this data has been processed
    private LocalDateTime processedAt; // When it was processed
    private String processingErrors; // Any errors during processing

    // Validation fields
    private Boolean isValid;
    private String validationErrors;

    /**
     * Create RawAircraftData from source with metadata
     */
    public static RawAircraftData fromSource(String source, String apiEndpoint, Object originalData,
            Long responseTime) {
        return RawAircraftData.builder()
                .source(source)
                .apiEndpoint(apiEndpoint)
                .fetchTime(LocalDateTime.now())
                .responseTimeMs(responseTime)
                .requestId(generateRequestId())
                .rawData(originalData)
                .processed(false)
                .isValid(true)
                .build();
    }

    /**
     * Generate unique request ID for tracking
     */
    private static String generateRequestId() {
        return System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * Mark as processed
     */
    public void markAsProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark as invalid with errors
     */
    public void markAsInvalid(String errors) {
        this.isValid = false;
        this.validationErrors = errors;
    }

    /**
     * Get unique identifier for this aircraft data
     */
    public String getUniqueId() {
        return source + ":" + (hexident != null ? hexident : callsign) + ":" + fetchTime.toString();
    }
}