package com.phamnam.tracking_vessel_flight.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "voyage", indexes = {
        @Index(name = "idx_voyage_departure_time", columnList = "departure_time"),
        @Index(name = "idx_voyage_status", columnList = "status"),
        @Index(name = "idx_voyage_ship_id", columnList = "ship_id"),
        @Index(name = "idx_voyage_number", columnList = "voyage_number")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Voyage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voyage_number")
    private String voyageNumber; // Unique voyage identifier

    @Column(name = "voyage_name")
    private String voyageName; // Descriptive name for the voyage

    // Schedule information
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

    // Port information
    @Column(name = "departure_port")
    private String departurePort; // Port code or name

    @Column(name = "arrival_port")
    private String arrivalPort; // Port code or name

    @Column(name = "departure_terminal")
    private String departureTerminal;

    @Column(name = "arrival_terminal")
    private String arrivalTerminal;

    @Column(name = "departure_berth")
    private String departureBerth;

    @Column(name = "arrival_berth")
    private String arrivalBerth;

    @Column(name = "intermediate_ports")
    private String intermediatePorts; // JSON array of intermediate ports

    // Voyage status
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VoyageStatus status = VoyageStatus.PLANNED;

    @Column(name = "voyage_phase")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VoyagePhase voyagePhase = VoyagePhase.UNKNOWN;

    // Cargo information
    @Column(name = "cargo_type")
    private String cargoType; // Type of cargo

    @Column(name = "cargo_description")
    @Lob
    private String cargoDescription;

    @Column(name = "cargo_quantity")
    private Double cargoQuantity; // In tons

    @Column(name = "cargo_value")
    private Double cargoValue; // Monetary value

    @Column(name = "dangerous_cargo")
    @Builder.Default
    private Boolean dangerousCargo = false;

    @Column(name = "container_count")
    private Integer containerCount;

    @Column(name = "teu_count")
    private Integer teuCount; // Twenty-foot Equivalent Units

    // Route and navigation
    @Column(name = "planned_route")
    @Lob
    private String plannedRoute; // JSON array of waypoints

    @Column(name = "actual_route")
    @Lob
    private String actualRoute; // JSON array of actual waypoints

    @Column(name = "planned_distance")
    private Double plannedDistance; // Nautical miles

    @Column(name = "actual_distance")
    private Double actualDistance; // Nautical miles

    @Column(name = "planned_duration")
    private Integer plannedDuration; // Hours

    @Column(name = "actual_duration")
    private Integer actualDuration; // Hours

    // Realtime information
    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "current_speed")
    private Double currentSpeed; // Knots

    @Column(name = "current_heading")
    private Double currentHeading; // Degrees

    @Column(name = "current_draught")
    private Double currentDraught; // Meters

    @Column(name = "navigation_status")
    private String navigationStatus;

    @Column(name = "destination_eta")
    private LocalDateTime destinationEta;

    // Delay and performance
    @Column(name = "departure_delay")
    private Integer departureDelay; // Hours

    @Column(name = "arrival_delay")
    private Integer arrivalDelay; // Hours

    @Column(name = "delay_reason")
    private String delayReason;

    @Column(name = "average_speed")
    private Double averageSpeed; // Knots

    @Column(name = "fuel_consumption")
    private Double fuelConsumption; // Tons

    // Environmental and safety
    @Column(name = "weather_impact")
    private String weatherImpact;

    @Column(name = "sea_conditions")
    private String seaConditions;

    @Column(name = "emergency_status")
    @Builder.Default
    private Boolean emergencyStatus = false;

    @Column(name = "security_level")
    private String securityLevel;

    @Column(name = "pollution_incidents")
    private String pollutionIncidents; // JSON array

    @Column(name = "inspection_status")
    private String inspectionStatus;

    // Commercial information
    @Column(name = "charter_type")
    private String charterType; // Time charter, voyage charter, etc.

    @Column(name = "customer")
    private String customer;

    @Column(name = "booking_reference")
    private String bookingReference;

    @Column(name = "bill_of_lading")
    private String billOfLading;

    @Column(name = "voyage_cost")
    private Double voyageCost;

    @Column(name = "revenue")
    private Double revenue;

    // Crew and passengers
    @Column(name = "crew_count")
    private Integer crewCount;

    @Column(name = "passenger_count")
    private Integer passengerCount;

    @Column(name = "captain_name")
    private String captainName;

    @Column(name = "pilot_required")
    @Builder.Default
    private Boolean pilotRequired = false;

    // Data quality and tracking
    @Column(name = "tracking_confidence")
    @Builder.Default
    private Double trackingConfidence = 1.0;

    @Column(name = "data_sources")
    private String dataSources; // JSON array of data sources

    @Column(name = "last_position_update")
    private LocalDateTime lastPositionUpdate;

    // Special voyage types
    @Column(name = "is_maiden_voyage")
    @Builder.Default
    private Boolean isMaidenVoyage = false;

    @Column(name = "is_ballast_voyage")
    @Builder.Default
    private Boolean isBallastVoyage = false;

    @Column(name = "is_emergency_voyage")
    @Builder.Default
    private Boolean isEmergencyVoyage = false;

    @Column(name = "is_research_voyage")
    @Builder.Default
    private Boolean isResearchVoyage = false;

    @Column(name = "is_military_voyage")
    @Builder.Default
    private Boolean isMilitaryVoyage = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ship_id")
    private Ship ship;

    @OneToMany(mappedBy = "voyage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ShipTracking> trackings;

    public enum VoyageStatus {
        PLANNED, // Voyage is planned
        PREPARING, // Preparing for departure
        DEPARTED, // Departed from port
        UNDERWAY, // Underway at sea
        APPROACHING, // Approaching destination
        ARRIVED, // Arrived at destination
        COMPLETED, // Voyage completed
        CANCELLED, // Voyage cancelled
        DELAYED, // Voyage delayed
        EMERGENCY, // Emergency situation
        DIVERTED // Diverted to alternate port
    }

    public enum VoyagePhase {
        UNKNOWN,
        LOADING, // Loading cargo/passengers
        DEPARTURE, // Departure procedures
        PILOTAGE_OUT, // Outbound pilotage
        OPEN_SEA, // In open sea
        COASTAL, // Coastal navigation
        TRAFFIC_SEPARATION, // In traffic separation scheme
        PILOTAGE_IN, // Inbound pilotage
        APPROACH, // Approaching port
        ANCHORING, // At anchor
        DOCKING, // Docking procedures
        UNLOADING, // Unloading cargo/passengers
        MAINTENANCE, // Under maintenance
        BUNKERS // Taking fuel/supplies
    }

    // Helper methods
    public boolean isActive() {
        return status == VoyageStatus.DEPARTED ||
                status == VoyageStatus.UNDERWAY ||
                status == VoyageStatus.APPROACHING;
    }

    public boolean isCompleted() {
        return status == VoyageStatus.ARRIVED ||
                status == VoyageStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == VoyageStatus.CANCELLED;
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

    public Double getCompletionPercentage() {
        if (plannedDistance == null || plannedDistance == 0)
            return 0.0;
        if (actualDistance == null)
            return 0.0;
        return Math.min(100.0, (actualDistance / plannedDistance) * 100);
    }

    public boolean hasDangerousCargo() {
        return dangerousCargo != null && dangerousCargo;
    }
}
