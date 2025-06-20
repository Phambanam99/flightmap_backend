package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ship")
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Ship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String mmsi; // Maritime Mobile Service Identity - unique identifier

    @Column(unique = true)
    private String imo; // IMO number - permanent identifier

    private String name; // Ship name
    private String callsign; // Radio callsign
    private String shipType; // Cargo, Tanker, Passenger, etc.
    private String flag; // Flag state

    // Physical dimensions
    private Double length; // Length overall in meters
    private Double width; // Beam in meters
    private Double draught; // Maximum draught in meters
    private Double grossTonnage; // Gross tonnage
    private Double deadweight; // Deadweight tonnage

    private Integer buildYear; // Year built

    // Enhanced fields for realtime tracking
    @Column(name = "ship_category")
    private String shipCategory; // Commercial, Military, Fishing, Pleasure, etc.

    @Column(name = "cargo_capacity")
    private Double cargoCapacity; // Cargo capacity in tons

    @Column(name = "passenger_capacity")
    private Integer passengerCapacity; // Number of passengers

    @Column(name = "crew_size")
    private Integer crewSize; // Number of crew members

    @Column(name = "max_speed")
    private Double maxSpeed; // Maximum speed in knots

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Data source information
    @Column(name = "data_source")
    private String dataSource; // MarineTraffic, VesselFinder, AIS, etc.

    @Column(name = "tracking_confidence")
    @Builder.Default
    private Double trackingConfidence = 1.0; // 0.0 to 1.0

    // Navigation status
    @Column(name = "navigation_status")
    private String navigationStatus; // Under way, At anchor, Moored, etc.

    // Special vessel categories
    @Column(name = "is_dangerous_cargo")
    @Builder.Default
    private Boolean isDangerousCargo = false;

    @Column(name = "is_high_priority")
    @Builder.Default
    private Boolean isHighPriority = false;

    @Column(name = "is_government")
    @Builder.Default
    private Boolean isGovernment = false;

    // Port information
    @Column(name = "home_port")
    private String homePort;

    @Column(name = "destination_port")
    private String destinationPort;

    @Column(name = "eta")
    private LocalDateTime eta; // Estimated Time of Arrival

    @OneToMany(mappedBy = "ship", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Voyage> voyages;

    @OneToMany(mappedBy = "ship", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShipMonitoring> monitoringData;
}
