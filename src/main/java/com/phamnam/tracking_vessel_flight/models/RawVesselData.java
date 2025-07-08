package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_vessel_data", indexes = {
        @Index(name = "idx_raw_vessel_source_time", columnList = "data_source, received_at"),
        @Index(name = "idx_raw_vessel_mmsi", columnList = "mmsi"),
        @Index(name = "idx_raw_vessel_received_at", columnList = "received_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawVesselData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Source tracking
    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource; // marinetraffic, vesselfinder, chinaports, marinetrafficv2

    @Column(name = "source_priority")
    private Integer sourcePriority;

    @Column(name = "api_response_time_ms")
    private Long apiResponseTime;

    // Vessel identification
    @Column(name = "mmsi", length = 15)
    private String mmsi;

    @Column(name = "imo", length = 15)
    private String imo;

    @Column(name = "callsign", length = 20)
    private String callsign;

    @Column(name = "vessel_name", length = 100)
    private String vesselName;

    @Column(name = "vessel_type", length = 50)
    private String vesselType;

    // Position data
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "course")
    private Integer course;

    @Column(name = "heading")
    private Integer heading;

    // Status and navigation
    @Column(name = "navigation_status", length = 50)
    private String navigationStatus;

    @Column(name = "destination", length = 100)
    private String destination;

    @Column(name = "eta", length = 50)
    private String eta;

    // Physical characteristics
    @Column(name = "length")
    private Integer length;

    @Column(name = "width")
    private Integer width;

    @Column(name = "draught")
    private Double draught;

    @Column(name = "flag", length = 10)
    private String flag;

    // Additional vessel information
    @Column(name = "cargo_type", length = 50)
    private String cargoType;

    @Column(name = "gross_tonnage")
    private Integer grossTonnage;

    @Column(name = "deadweight")
    private Integer deadweight;

    @Column(name = "build_year", length = 4)
    private String buildYear;

    @Column(name = "last_port", length = 100)
    private String lastPort;

    @Column(name = "next_port", length = 100)
    private String nextPort;

    @Column(name = "route", length = 200)
    private String route;

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

    // Special flags for vessel data
    @Column(name = "dangerous_cargo")
    private Boolean dangerousCargo;

    @Column(name = "security_alert")
    private Boolean securityAlert;

    // Retention policy
    @Column(name = "retention_days")
    @Builder.Default
    private Integer retentionDays = 30; // Keep raw data for 30 days by default
}