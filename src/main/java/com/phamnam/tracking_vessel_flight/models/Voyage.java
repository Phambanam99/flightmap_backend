package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "voyage")
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

    private String voyageNumber; // Tên mã số hành trình nếu có
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    private String departurePort;
    private String arrivalPort;

    @ManyToOne
    @JoinColumn(name = "ship_id")
    private Ship ship;

    @OneToMany(mappedBy = "voyage", cascade = CascadeType.ALL)
    private List<ShipTracking> trackings;
}
