package com.phamnam.tracking_vessel_flight.models.raw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Raw vessel data from external APIs with metadata
 * This model preserves original data structure while adding tracking metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawVesselData {

    // Metadata fields
    private String source; // "marinetraffic", "vesselfinder", "chinaports", etc.
    private String apiEndpoint; // Original API endpoint
    private LocalDateTime fetchTime; // When data was fetched
    private Long responseTimeMs; // API response time
    private String requestId; // Unique request identifier
    private Double dataQuality; // Data quality score (0.0-1.0)

    // Original vessel data fields - using appropriate types
    private String mmsi; // Maritime Mobile Service Identity
    private String imo; // International Maritime Organization number
    private String vesselName;
    private String callsign;
    private Double latitude;
    private Double longitude;
    private Double speed; // Speed over ground (knots)
    private Double course; // Course over ground (degrees)
    private Double heading; // True heading (degrees)
    private String navigationStatus; // AIS navigation status
    private String vesselType; // Type of vessel
    private LocalDateTime timestamp;

    // Vessel specifications
    private Integer length; // Length in meters
    private Integer width; // Width in meters
    private Double draught; // Draught in meters
    private Integer grossTonnage;
    private Integer deadweight;
    private String flag; // Flag state
    private String vesselClass;
    private Integer yearBuilt;
    private String homePort;

    // Voyage information
    private String destination;
    private String eta; // Estimated Time of Arrival
    private String origin;
    private String voyageId;
    private String cargoType;

    // Additional fields that might come from different APIs
    private Object rawData; // Original JSON/XML data as received
    private String operator;
    private String operatorCode;
    private String charterer;
    private String manager;
    private String owner;
    private String builder;
    private String engineType;
    private Integer enginePower;

    // Processing status
    private Boolean processed; // Whether this data has been processed
    private LocalDateTime processedAt; // When it was processed
    private String processingErrors; // Any errors during processing

    // Validation fields
    private Boolean isValid;
    private String validationErrors;

    // Geographic validation
    private Boolean inBounds; // Whether position is within configured bounds

    /**
     * Create RawVesselData from source with metadata
     */
    public static RawVesselData fromSource(String source, String apiEndpoint, Object originalData, Long responseTime) {
        return RawVesselData.builder()
                .source(source)
                .apiEndpoint(apiEndpoint)
                .fetchTime(LocalDateTime.now())
                .responseTimeMs(responseTime)
                .requestId(generateRequestId())
                .rawData(originalData)
                .processed(false)
                .isValid(true)
                .inBounds(true)
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
     * Mark as out of bounds
     */
    public void markAsOutOfBounds() {
        this.inBounds = false;
    }

    /**
     * Get unique identifier for this vessel data
     */
    public String getUniqueId() {
        return source + ":" + (mmsi != null ? mmsi : imo) + ":" + fetchTime.toString();
    }

    /**
     * Check if vessel data has valid position
     */
    public boolean hasValidPosition() {
        return latitude != null && longitude != null &&
                latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }

    /**
     * Check if vessel has valid MMSI
     */
    public boolean hasValidMmsi() {
        return mmsi != null && mmsi.matches("\\d{9}");
    }
}