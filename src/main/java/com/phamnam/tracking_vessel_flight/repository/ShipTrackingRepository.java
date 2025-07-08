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

    // Methods for IntelligentStorageService
    @Query("SELECT st FROM ShipTracking st WHERE st.mmsi = :mmsi AND st.timestamp BETWEEN :fromTime AND :toTime ORDER BY st.timestamp ASC")
    List<ShipTracking> findByMmsiAndTimestampBetweenOrderByTimestampAsc(@Param("mmsi") String mmsi,
            @Param("fromTime") java.time.LocalDateTime fromTime, @Param("toTime") java.time.LocalDateTime toTime);

    @Query("SELECT COUNT(st) FROM ShipTracking st WHERE st.timestamp > :afterTime")
    long countByTimestampAfter(@Param("afterTime") java.time.LocalDateTime afterTime);

    // Additional methods for data comparison
    @Query("SELECT st FROM ShipTracking st WHERE st.mmsi = :mmsi AND st.timestamp BETWEEN :start AND :end ORDER BY st.timestamp ASC")
    List<ShipTracking> findByMmsiAndTimestampBetween(@Param("mmsi") String mmsi,
            @Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
}
