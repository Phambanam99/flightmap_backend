package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.Entity;
import lombok.Data;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_vessel")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserFlight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userVesselId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "flight_id")
    private Aircraft aircraft;
}