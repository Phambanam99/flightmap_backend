package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_aircraft_data", indexes = {
        @Index(name = "idx_raw_aircraft_source_time", columnList = "data_source, received_at"),
        @Index(name = "idx_raw_aircraft_hexident", columnList = "hexident"),
        @Index(name = "idx_raw_aircraft_received_at", columnList = "received_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawAircraftData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Source tracking
    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource; // flightradar24, adsbexchange, etc.

    @Column(name = "source_priority")
    private Integer sourcePriority;

    @Column(name = "api_response_time_ms")
    private Long apiResponseTime;

    // Aircraft identification
    @Column(name = "hexident", length = 20)
    private String hexident;

    @Column(name = "callsign", length = 20)
    private String callsign;

    @Column(name = "registration", length = 20)
    private String registration;

    @Column(name = "aircraft_type", length = 20)
    private String aircraftType;

    // Position data
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "altitude")
    private Integer altitude;

    @Column(name = "ground_speed")
    private Integer groundSpeed;

    @Column(name = "track")
    private Integer track;

    @Column(name = "vertical_rate")
    private Integer verticalRate;

    // Status
    @Column(name = "squawk", length = 4)
    private String squawk;

    @Column(name = "on_ground")
    private Boolean onGround;

    @Column(name = "emergency")
    private Boolean emergency;

    // Quality and metadata
    @Column(name = "data_quality")
    private Double dataQuality;

    @Column(name = "original_timestamp")
    private LocalDateTime originalTimestamp; // Timestamp from source

    @CreatedDate
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt; // When we received from API

    @Column(name = "processed_at")
    private LocalDateTime processedAt; // When it was processed/fused

    @Column(name = "fusion_result_id")
    private Long fusionResultId; // Link to final fused record

    // Raw data preservation
    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson; // Original JSON response for debugging

    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint; // Which API endpoint was called

    // Data validation flags
    @Column(name = "is_valid")
    @Builder.Default
    private Boolean isValid = true;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @Column(name = "is_duplicate")
    @Builder.Default
    private Boolean isDuplicate = false;

    @Column(name = "duplicate_of_id")
    private Long duplicateOfId;

    // Retention policy
    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 30; // Keep raw data for 30 days by default
}