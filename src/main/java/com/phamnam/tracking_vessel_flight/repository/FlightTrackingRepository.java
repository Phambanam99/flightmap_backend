package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FlightTrackingRepository extends JpaRepository<FlightTracking, Long> {
    @Query(value = "SELECT * FROM tracking WHERE ST_DWithin(location, ST_SetSRID(ST_Point(:lon, :lat), 4326), :radius)", nativeQuery = true)
    List<FlightTracking> findWithinRadius(@Param("lon") double lon, @Param("lat") double lat,
            @Param("radius") double radius);

    @Query(value = "SELECT * FROM tracking WHERE ST_DWithin(location, ST_SetSRID(ST_Point(:lon, :lat), 4326), :radius)", countQuery = "SELECT COUNT(*) FROM tracking WHERE ST_DWithin(location, ST_SetSRID(ST_Point(:lon, :lat), 4326), :radius)", nativeQuery = true)
    Page<FlightTracking> findWithinRadiusPaginated(@Param("lon") double lon, @Param("lat") double lat,
            @Param("radius") double radius, Pageable pageable);

    List<FlightTracking> findByFlight_id(Long flightId);

    Page<FlightTracking> findByFlight_id(Long flightId, Pageable pageable);

    @Query("""
                SELECT t FROM FlightTracking t
                WHERE t.flight.id = :flightId
                ORDER BY t.updateTime DESC
            """)
    Optional<FlightTracking> findLastTrackingByFlightId(@Param("flightId") Long flightId);
}
