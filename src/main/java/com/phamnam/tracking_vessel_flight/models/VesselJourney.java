package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vessel_journey")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VesselJourney extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long journeyId;

    @ManyToOne
    @JoinColumn(name = "vessel_id")
    private Aircraft aircraft;

    private String fromPort;
    private String toPort;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String status;
}
