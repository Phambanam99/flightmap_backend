package com.phamnam.tracking_vessel_flight.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.phamnam.tracking_vessel_flight.config.serializer.PointSerializer;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_points", indexes = {
        @Index(name = "idx_tracking_points_timestamp", columnList = "timestamp"),
        @Index(name = "idx_tracking_points_entity_id", columnList = "entity_type,entity_id"),
        @Index(name = "idx_tracking_points_location", columnList = "latitude,longitude")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrackingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TimescaleDB time column
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    // Entity identification
    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entityType; // AIRCRAFT, VESSEL

    @Column(name = "entity_id", nullable = false)
    private String entityId; // hexident for aircraft, mmsi for vessel

    @Column(name = "entity_name")
    private String entityName; // callsign or ship name

    // Position data
    @Column(columnDefinition = "geometry(Point, 4326)")
    @JsonSerialize(using = PointSerializer.class)
    private Point location;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "altitude")
    private Double altitude; // feet for aircraft, null for vessels

    // Movement data
    @Column(name = "speed")
    private Double speed; // knots

    @Column(name = "heading")
    private Double heading; // degrees

    @Column(name = "course")
    private Double course; // degrees

    @Column(name = "vertical_speed")
    private Double verticalSpeed; // ft/min for aircraft

    // Status information
    @Column(name = "status")
    private String status; // flight phase, navigation status, etc.

    @Column(name = "special_indicators")
    private String specialIndicators; // emergency, spi, etc.

    // Data source and quality
    @Column(name = "data_source")
    private String dataSource;

    @Column(name = "data_quality")
    @Builder.Default
    private Double dataQuality = 1.0; // 0.0 to 1.0

    @Column(name = "signal_strength")
    private Double signalStrength;

    // Aggregation metadata
    @Column(name = "aggregation_count")
    @Builder.Default
    private Integer aggregationCount = 1; // Number of raw points aggregated

    @Column(name = "aggregation_window")
    private Integer aggregationWindow; // Window size in seconds

    @Column(name = "is_interpolated")
    @Builder.Default
    private Boolean isInterpolated = false;

    @Column(name = "is_predicted")
    @Builder.Default
    private Boolean isPredicted = false;

    // Environment data
    @Column(name = "weather_conditions")
    private String weatherConditions;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "wind_direction")
    private Double windDirection;

    @Column(name = "temperature")
    private Double temperature;

    // Geographic context
    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "territorial_waters")
    private String territorialWaters;

    @Column(name = "flight_information_region")
    private String flightInformationRegion;

    // Processed timestamp
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "partition_key")
    private String partitionKey; // For efficient partitioning

    public enum EntityType {
        AIRCRAFT,
        VESSEL
    }

    @PrePersist
    @PreUpdate
    public void updateCoordinates() {
        if (location != null) {
            this.latitude = location.getY();
            this.longitude = location.getX();
        }
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }

        // Set partition key based on timestamp (daily partitions)
        if (timestamp != null) {
            this.partitionKey = timestamp.toLocalDate().toString();
        }
    }

    // Helper methods for easy access
    @JsonProperty("lat")
    public Double getLatitude() {
        return latitude;
    }

    @JsonProperty("lon")
    public Double getLongitude() {
        return longitude;
    }

    @JsonProperty("alt")
    public Double getAltitude() {
        return altitude;
    }

    @JsonProperty("spd")
    public Double getSpeed() {
        return speed;
    }

    @JsonProperty("hdg")
    public Double getHeading() {
        return heading;
    }

    public boolean isAircraft() {
        return EntityType.AIRCRAFT.equals(entityType);
    }

    public boolean isVessel() {
        return EntityType.VESSEL.equals(entityType);
    }
}