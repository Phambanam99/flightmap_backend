package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShipTrackingRepository extends JpaRepository<ShipTracking, Long> {
    // JpaRepository provides built-in methods:
    // save(), findById(), findAll(), deleteById(), etc.

    @Query("""
                SELECT t FROM ShipTracking t
                WHERE t.voyage.id = :voyageId
                ORDER BY t.timestamp DESC
                LIMIT 1
            """)
    Optional<ShipTracking> findLastTrackingByVoyageId(@Param("voyageId") Long voyageId);

    @Query("""
                SELECT t FROM ShipTracking t
                JOIN t.voyage v
                WHERE v.ship.id = :shipId
                ORDER BY t.timestamp DESC
                LIMIT 1
            """)
    Optional<ShipTracking> findLastTrackingByShipId(@Param("shipId") Long shipId);

    @Query("""
                SELECT t FROM ShipTracking t
                WHERE t.voyage.id = :voyageId
                ORDER BY t.timestamp DESC
            """)
    List<ShipTracking> findByVoyageIdOrderByTimestampDesc(@Param("voyageId") Long voyageId);
}
