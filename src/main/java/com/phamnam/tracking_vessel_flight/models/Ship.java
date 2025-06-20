package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.*;
import lombok.*;

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

    private String mmsi; // Maritime Mobile Service Identity - số định danh
    private String imo; // Số IMO - số nhận dạng tàu (optional)
    private String name;
    private String callsign;
    private String shipType;
    private String flag; // Quốc tịch
    private Double length;
    private Double width;
    private Integer buildYear;

    @OneToMany(mappedBy = "ship", cascade = CascadeType.ALL)
    private List<Voyage> voyages;
}
