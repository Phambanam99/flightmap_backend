package com.phamnam.tracking_vessel_flight.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.phamnam.tracking_vessel_flight.config.serializer.PointSerializer;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "ship_tracking", indexes = {
        @Index(name = "idx_ship_tracking_timestamp", columnList = "timestamp"),
        @Index(name = "idx_ship_tracking_voyage_id", columnList = "voyage_id"),
        @Index(name = "idx_ship_tracking_mmsi", columnList = "mmsi"),
        @Index(name = "idx_ship_tracking_navigation_status", columnList = "navigation_status")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShipTracking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Core tracking data
    @Column(nullable = false)
    private LocalDateTime timestamp; // Primary time column for TimescaleDB

    @Column(name = "mmsi", nullable = false)
    private String mmsi; // MMSI for easier querying

    // Position data
    @Column(columnDefinition = "geometry(Point, 4326)")
    @JsonSerialize(using = PointSerializer.class)
    private Point location;

    @Column(name = "latitude")
    private Double latitude; // Denormalized for easier querying

    @Column(name = "longitude")
    private Double longitude; // Denormalized for easier querying

    // Movement data
    @Column(name = "speed_over_ground")
    private Double speed; // Speed over ground in knots

    @Column(name = "course_over_ground")
    private Double course; // Course over ground in degrees (0-359)

    @Column(name = "heading")
    private Double heading; // True heading in degrees

    @Column(name = "rate_of_turn")
    private Double rateOfTurn; // Rate of turn in degrees per minute

    // Physical status
    @Column(name = "draught")
    private Double draught; // Current draught in meters

    @Column(name = "air_draught")
    private Double airDraught; // Air draught (height above water) in meters

    // Navigation information
    @Column(name = "navigation_status")
    private String navigationStatus; // Under way, At anchor, Moored, etc.

    @Column(name = "maneuver_indicator")
    private String maneuverIndicator; // Not available, No special maneuver, Special maneuver

    @Column(name = "special_maneuver")
    @Builder.Default
    private Boolean specialManeuver = false;

    // AIS data quality
    @Column(name = "position_accuracy")
    @Builder.Default
    private Boolean positionAccuracy = false; // High (true) or Low (false) accuracy

    @Column(name = "timestamp_accuracy")
    @Builder.Default
    private Boolean timestampAccuracy = true; // UTC synchronized or not

    @Column(name = "data_terminal_ready")
    @Builder.Default
    private Boolean dataTerminalReady = true;

    // Data source information
    @Column(name = "data_source")
    private String dataSource; // AIS, Satellite AIS, Terrestrial AIS

    @Column(name = "receiver_id")
    private String receiverId; // Which receiver captured this data

    @Column(name = "signal_level")
    private Float signalLevel; // Signal strength

    @Column(name = "update_time")
    private LocalDateTime updateTime; // When this record was created

    // Cargo and operational information
    @Column(name = "cargo_type")
    private String cargoType; // Type of cargo being carried

    @Column(name = "cargo_status")
    private String cargoStatus; // Loading, Loaded, Unloading, Empty

    @Column(name = "persons_on_board")
    private Integer personsOnBoard; // Total persons on board

    // Port and destination information
    @Column(name = "destination")
    private String destination; // Destination port or area

    @Column(name = "eta")
    private LocalDateTime eta; // Estimated Time of Arrival

    @Column(name = "port_of_call")
    private String portOfCall; // Current or next port

    // Weather and environmental data
    @Column(name = "wind_speed")
    private Double windSpeed; // Wind speed in knots

    @Column(name = "wind_direction")
    private Double windDirection; // Wind direction in degrees

    @Column(name = "wave_height")
    private Double waveHeight; // Significant wave height in meters

    @Column(name = "water_temperature")
    private Double waterTemperature; // Water temperature in Celsius

    @Column(name = "air_temperature")
    private Double airTemperature; // Air temperature in Celsius

    // Safety and security
    @Column(name = "security_alert")
    @Builder.Default
    private Boolean securityAlert = false;

    @Column(name = "piracy_area")
    @Builder.Default
    private Boolean piracyArea = false;

    @Column(name = "dangerous_cargo")
    @Builder.Default
    private Boolean dangerousCargo = false;

    // Zone information
    @Column(name = "exclusive_economic_zone")
    private String exclusiveEconomicZone; // EEZ country code

    @Column(name = "territorial_waters")
    private String territorialWaters; // Territorial waters country code

    @Column(name = "fishing_zone")
    @Builder.Default
    private Boolean fishingZone = false;

    @Column(name = "marine_protected_area")
    @Builder.Default
    private Boolean marineProtectedArea = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voyage_id")
    @JsonBackReference
    private Voyage voyage;

    @JsonProperty("voyageId")
    public Long getVoyageId() {
        return voyage != null ? voyage.getId() : null;
    }

    @JsonProperty("shipId")
    public Long getShipId() {
        return voyage != null && voyage.getShip() != null ? voyage.getShip().getId() : null;
    }

    // Helper method to set lat/lon from Point
    @PrePersist
    @PreUpdate
    public void updateCoordinates() {
        if (location != null) {
            this.latitude = location.getY();
            this.longitude = location.getX();
        }
        if (updateTime == null) {
            updateTime = LocalDateTime.now();
        }
    }
}
