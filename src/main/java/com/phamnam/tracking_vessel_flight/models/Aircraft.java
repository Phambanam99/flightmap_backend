package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "aircraft")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Aircraft extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String hexident; //icao24
    private String register;
    private String type;
    private String manufacture;
    private String constructorNumber;
    private String operator;
    private String operatorCode;
    private String engines;
    private String engineType;
    private Boolean isMilitary;
    private String country;
    private String transponderType;
    private Integer year;
    private String source;
    private Integer itemType;
    @OneToMany(mappedBy = "aircraft", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlightTracking> flightTrackings;
}