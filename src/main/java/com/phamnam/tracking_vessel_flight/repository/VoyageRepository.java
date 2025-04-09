package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.Voyage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoyageRepository extends JpaRepository<Voyage, Long> {
    // JpaRepository provides built-in methods:
    // save(), findById(), findAll(), deleteById(), etc.

    @Query("""
                SELECT v FROM Voyage v
                WHERE v.ship.id = :shipId
                ORDER BY v.departureTime DESC
                LIMIT 1
            """)
    Optional<Voyage> findLatestVoyageByShipId(@Param("shipId") Long shipId);

    List<Voyage> findByShipIdOrderByDepartureTimeDesc(Long shipId);
}
