package com.phamnam.tracking_vessel_flight.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "aircraft")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Aircraft extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String hexident; // ICAO24 - unique identifier

    private String register; // Aircraft registration
    private String type; // Aircraft type (B737, A320, etc.)
    private String manufacture; // Boeing, Airbus, etc.
    private String constructorNumber; // Serial number
    private String operator; // Airline name
    private String operatorCode; // IATA/ICAO code
    private String engines; // Engine description
    private String engineType; // Jet, Turboprop, etc.

    @Column(name = "is_military")
    @Builder.Default
    private Boolean isMilitary = false;

    private String country; // Country of registration
    private String transponderType; // ADS-B, Mode S, etc.
    private Integer year; // Year of manufacture

    // Enhanced fields for realtime tracking
    @Column(name = "wake_turbulence_category")
    private String wakeTurbulenceCategory; // L, M, H, J

    @Column(name = "max_altitude")
    private Integer maxAltitude; // Service ceiling in feet

    @Column(name = "max_speed")
    private Integer maxSpeed; // Maximum speed in knots

    @Column(name = "emergency_squawk")
    private String emergencySquawk; // Current emergency squawk if any

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Data source information
    private String source; // FlightRadar24, ADS-B Exchange, etc.
    private Integer itemType;

    // Tracking confidence level
    @Column(name = "tracking_confidence")
    @Builder.Default
    private Double trackingConfidence = 1.0; // 0.0 to 1.0

    // Special aircraft categories
    @Column(name = "is_cargo")
    @Builder.Default
    private Boolean isCargo = false;

    @Column(name = "is_passenger")
    @Builder.Default
    private Boolean isPassenger = false;

    @Column(name = "is_government")
    @Builder.Default
    private Boolean isGovernment = false;

    @OneToMany(mappedBy = "aircraft", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Flight> flights;

    @OneToMany(mappedBy = "aircraft", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<AircraftMonitoring> monitoringData;
}