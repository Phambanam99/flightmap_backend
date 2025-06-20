package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ship_tracking")
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShipTracking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;
    private Double latitude;
    private Double longitude;
    private Double speed; // knots
    private Double course; // hướng di chuyển (heading)
    private Double draught; // mớn nước (nếu có)

    @ManyToOne
    @JoinColumn(name = "voyage_id")
    private Voyage voyage;
}
