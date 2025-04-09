package com.phamnam.tracking_vessel_flight.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.phamnam.tracking_vessel_flight.config.serializer.PointSerializer;
import jakarta.persistence.Entity;
import lombok.*;
import org.locationtech.jts.geom.Point;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "flight_tracking")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "trackingId")
public class FlightTracking extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trackingId;

    @ManyToOne
    @JoinColumn(name = "flight_id")
    @JsonIgnore // Prevents infinite recursion when serializing
    private Flight flight;

    private Float altitude;
    private String altitudeType;
    private Float targetAlt;
    private String callsign;
    private Float speed;
    private String speedType;
    private Float verticalSpeed;
    private Integer squawk;
    private Float distance;
    private Float bearing;
    private Long unixTime;
    private LocalDateTime updateTime;

    // Define the column with specific PostGIS type and add the custom serializer
    @Column(columnDefinition = "geometry(Point, 4326)")
    @JsonSerialize(using = PointSerializer.class)
    private Point location;

    private Long landingUnixTimes;
    private LocalDateTime landingTimes;

    @JsonProperty("flightId")
    public Long getFlightId() {
        return flight != null ? flight.getId() : null;
    }

    @Transient
    @JsonProperty("aircraftId")
    public Long getAircraftId() {
        return flight != null && flight.getAircraft() != null ? flight.getAircraft().getId() : null;
    }
}