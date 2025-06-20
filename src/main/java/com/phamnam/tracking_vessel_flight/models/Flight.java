package com.phamnam.tracking_vessel_flight.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "flight", indexes = {
        @Index(name = "idx_flight_callsign", columnList = "callsign"),
        @Index(name = "idx_flight_status", columnList = "status"),
        @Index(name = "idx_flight_departure_time", columnList = "departure_time"),
        @Index(name = "idx_flight_aircraft_id", columnList = "aircraft_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Flight extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "callsign", nullable = false)
    private String callsign;

    // Flight schedule information
    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Column(name = "scheduled_departure_time")
    private LocalDateTime scheduledDepartureTime;

    @Column(name = "scheduled_arrival_time")
    private LocalDateTime scheduledArrivalTime;

    @Column(name = "estimated_departure_time")
    private LocalDateTime estimatedDepartureTime;

    @Column(name = "estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;

    // Flight status
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    @Column(name = "flight_phase")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FlightPhase flightPhase = FlightPhase.UNKNOWN;

    // Airport information
    @Column(name = "origin_airport")
    private String originAirport; // ICAO code

    @Column(name = "destination_airport")
    private String destinationAirport; // ICAO code

    @Column(name = "departure_terminal")
    private String departureTerminal;

    @Column(name = "arrival_terminal")
    private String arrivalTerminal;

    @Column(name = "departure_gate")
    private String departureGate;

    @Column(name = "arrival_gate")
    private String arrivalGate;

    @Column(name = "departure_runway")
    private String departureRunway;

    @Column(name = "arrival_runway")
    private String arrivalRunway;

    // Flight details
    @Column(name = "flight_number")
    private String flightNumber; // Airline flight number

    @Column(name = "airline_code")
    private String airlineCode; // IATA/ICAO airline code

    @Column(name = "aircraft_type")
    private String aircraftType; // Aircraft type from aircraft or override

    @Column(name = "registration")
    private String registration; // Aircraft registration

    @Column(name = "route")
    @Lob
    private String route; // Planned route waypoints

    @Column(name = "alternate_airports")
    private String alternateAirports; // JSON array of alternate airports

    // Realtime tracking information
    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "current_altitude")
    private Integer currentAltitude;

    @Column(name = "current_speed")
    private Integer currentSpeed;

    @Column(name = "current_heading")
    private Integer currentHeading;

    @Column(name = "squawk_code")
    private String squawkCode;

    @Column(name = "emergency_status")
    @Builder.Default
    private Boolean emergencyStatus = false;

    @Column(name = "on_ground")
    @Builder.Default
    private Boolean onGround = false;

    // Distance and duration
    @Column(name = "planned_distance")
    private Double plannedDistance; // Nautical miles

    @Column(name = "actual_distance")
    private Double actualDistance; // Nautical miles

    @Column(name = "planned_duration")
    private Integer plannedDuration; // Minutes

    @Column(name = "actual_duration")
    private Integer actualDuration; // Minutes

    // Delay information
    @Column(name = "departure_delay")
    private Integer departureDelay; // Minutes

    @Column(name = "arrival_delay")
    private Integer arrivalDelay; // Minutes

    @Column(name = "delay_reason")
    private String delayReason;

    // Weather and conditions
    @Column(name = "weather_impact")
    private String weatherImpact;

    @Column(name = "turbulence_reports")
    private String turbulenceReports; // JSON array

    // Data quality and source
    @Column(name = "tracking_confidence")
    @Builder.Default
    private Double trackingConfidence = 1.0;

    @Column(name = "data_sources")
    private String dataSources; // JSON array of data sources

    @Column(name = "last_position_update")
    private LocalDateTime lastPositionUpdate;

    // Commercial flight information
    @Column(name = "passenger_count")
    private Integer passengerCount;

    @Column(name = "cargo_weight")
    private Double cargoWeight; // Tons

    @Column(name = "fuel_on_board")
    private Double fuelOnBoard; // Tons

    // Special categories
    @Column(name = "is_cargo_flight")
    @Builder.Default
    private Boolean isCargoFlight = false;

    @Column(name = "is_charter_flight")
    @Builder.Default
    private Boolean isCharterFlight = false;

    @Column(name = "is_medical_flight")
    @Builder.Default
    private Boolean isMedicalFlight = false;

    @Column(name = "is_military_flight")
    @Builder.Default
    private Boolean isMilitaryFlight = false;

    @Column(name = "is_training_flight")
    @Builder.Default
    private Boolean isTrainingFlight = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<FlightTracking> trackings;

    public enum FlightStatus {
        SCHEDULED, // Flight is scheduled
        DELAYED, // Flight is delayed
        DEPARTED, // Flight has departed
        IN_AIR, // Flight is airborne
        APPROACHING, // Flight is approaching destination
        LANDED, // Flight has landed
        ARRIVED, // Flight has arrived at gate
        CANCELLED, // Flight is cancelled
        DIVERTED, // Flight was diverted
        RETURNED, // Flight returned to origin
        UNKNOWN // Status unknown
    }

    public enum FlightPhase {
        UNKNOWN,
        PRE_FLIGHT, // Before departure
        TAXI_OUT, // Taxiing to runway
        TAKEOFF, // Taking off
        CLIMB, // Climbing to cruise altitude
        CRUISE, // At cruise altitude
        DESCENT, // Descending from cruise
        APPROACH, // On approach
        LANDING, // Landing
        TAXI_IN, // Taxiing to gate
        ARRIVED // At gate
    }

    // Helper methods
    public boolean isActive() {
        return status == FlightStatus.DEPARTED ||
                status == FlightStatus.IN_AIR ||
                status == FlightStatus.APPROACHING;
    }

    public boolean isCompleted() {
        return status == FlightStatus.LANDED ||
                status == FlightStatus.ARRIVED;
    }

    public boolean isCancelled() {
        return status == FlightStatus.CANCELLED;
    }

    public boolean isDelayed() {
        return departureDelay != null && departureDelay > 0;
    }

    public boolean isEmergency() {
        return emergencyStatus != null && emergencyStatus;
    }

    public Integer getTotalDelay() {
        if (departureDelay == null && arrivalDelay == null)
            return 0;
        return (departureDelay != null ? departureDelay : 0) +
                (arrivalDelay != null ? arrivalDelay : 0);
    }
}
