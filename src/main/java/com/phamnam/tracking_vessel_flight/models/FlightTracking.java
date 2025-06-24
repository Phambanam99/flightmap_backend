package com.phamnam.tracking_vessel_flight.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.phamnam.tracking_vessel_flight.config.serializer.PointSerializer;
import jakarta.persistence.Entity;
import lombok.*;
import org.locationtech.jts.geom.Point;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "flight_tracking", indexes = {
        @Index(name = "idx_flight_tracking_timestamp", columnList = "timestamp"),
        @Index(name = "idx_flight_tracking_flight_id", columnList = "flight_id"),
        @Index(name = "idx_flight_tracking_hexident", columnList = "hexident"),
        @Index(name = "idx_flight_tracking_callsign", columnList = "callsign"),
        @Index(name = "idx_flight_tracking_squawk", columnList = "squawk")
})
@Data
@ToString(exclude = { "flight" })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "trackingId")
public class FlightTracking extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracking_id")
    private Long trackingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    @JsonBackReference
    private Flight flight;

    // Core tracking data
    @Column(nullable = false)
    private LocalDateTime timestamp; // Primary time column for TimescaleDB

    @Column(name = "hexident", nullable = false)
    private String hexident; // ICAO24 for easier querying

    @Column(name = "last_seen")
    private LocalDateTime lastSeen; // For IntelligentStorageService compatibility

    private String callsign;

    // Position data
    @Column(columnDefinition = "geometry(Point, 4326)")
    @JsonSerialize(using = PointSerializer.class)
    private Point location;

    @Column(name = "latitude")
    private Double latitude; // Denormalized for easier querying

    @Column(name = "longitude")
    private Double longitude; // Denormalized for easier querying

    // Altitude information
    @Column(name = "altitude")
    private Float altitude; // Current altitude in feet

    @Column(name = "altitude_type")
    private String altitudeType; // Barometric, GNSS, etc.

    @Column(name = "target_altitude")
    private Float targetAlt; // Target altitude

    @Column(name = "geometric_altitude")
    private Float geometricAltitude; // GPS altitude

    // Speed information
    @Column(name = "ground_speed")
    private Float speed; // Ground speed in knots

    @Column(name = "speed_type")
    private String speedType; // Ground, Air, etc.

    @Column(name = "indicated_airspeed")
    private Float indicatedAirspeed; // IAS in knots

    @Column(name = "true_airspeed")
    private Float trueAirspeed; // TAS in knots

    @Column(name = "vertical_speed")
    private Float verticalSpeed; // Rate of climb/descent in ft/min

    // Direction information
    @Column(name = "track")
    private Float track; // Ground track in degrees

    @Column(name = "heading")
    private Float heading; // Magnetic heading in degrees

    @Column(name = "bearing")
    private Float bearing; // Bearing from reference point

    @Column(name = "distance")
    private Float distance; // Distance from reference point

    // Transponder information
    @Column(name = "squawk")
    private Integer squawk; // Transponder code

    @Column(name = "emergency")
    @Builder.Default
    private Boolean emergency = false; // Emergency status

    @Column(name = "spi")
    @Builder.Default
    private Boolean spi = false; // Special Position Identification

    // Data quality and source
    @Column(name = "unix_time")
    private Long unixTime; // Original timestamp

    @Column(name = "update_time")
    private LocalDateTime updateTime; // When this record was created

    @Column(name = "data_source")
    private String dataSource; // ADS-B, Mode S, etc.

    @Column(name = "receiver_id")
    private String receiverId; // Which receiver captured this data

    @Column(name = "signal_level")
    private Float signalLevel; // Signal strength

    @Column(name = "messages_count")
    private Integer messagesCount; // Number of messages received

    // Flight phase information
    @Column(name = "flight_phase")
    private String flightPhase; // Takeoff, Climb, Cruise, Descent, Landing

    @Column(name = "on_ground")
    @Builder.Default
    private Boolean onGround = false;

    @Column(name = "gear_down")
    @Builder.Default
    private Boolean gearDown = false;

    @Column(name = "flaps_down")
    @Builder.Default
    private Boolean flapsDown = false;

    // Airport information
    @Column(name = "origin_airport")
    private String originAirport;

    @Column(name = "destination_airport")
    private String destinationAirport;

    // Landing information
    @Column(name = "landing_unix_time")
    private Long landingUnixTimes;

    @Column(name = "landing_time")
    private LocalDateTime landingTimes;

    // Weather impact
    @Column(name = "wind_speed")
    private Float windSpeed;

    @Column(name = "wind_direction")
    private Float windDirection;

    @Column(name = "temperature")
    private Float temperature;

    // Navigation performance
    @Column(name = "navigation_accuracy")
    private String navigationAccuracy; // RNP value

    @Column(name = "surveillance_status")
    private String surveillanceStatus; // No condition, permanent alert, etc.

    @JsonProperty("flightId")
    public Long getFlightId() {
        return flight != null ? flight.getId() : null;
    }

    @Transient
    @JsonProperty("aircraftId")
    public Long getAircraftId() {
        return flight != null && flight.getAircraft() != null ? flight.getAircraft().getId() : null;
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