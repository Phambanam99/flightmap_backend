package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlightTrackingRepository extends JpaRepository<FlightTracking, Long> {
        @Query(value = "SELECT * FROM flight_tracking WHERE ST_DWithin(location, ST_SetSRID(ST_Point(:lon, :lat), 4326), :radius)", nativeQuery = true)
        List<FlightTracking> findWithinRadius(@Param("lon") double lon, @Param("lat") double lat,
                        @Param("radius") double radius);

        @Query(value = "SELECT * FROM flight_tracking WHERE ST_DWithin(location, ST_SetSRID(ST_Point(:lon, :lat), 4326), :radius)", countQuery = "SELECT COUNT(*) FROM flight_tracking WHERE ST_DWithin(location, ST_SetSRID(ST_Point(:lon, :lat), 4326), :radius)", nativeQuery = true)
        Page<FlightTracking> findWithinRadiusPaginated(@Param("lon") double lon, @Param("lat") double lat,
                        @Param("radius") double radius, Pageable pageable);

        List<FlightTracking> findByFlight_id(Long flightId);

        Page<FlightTracking> findByFlight_id(Long flightId, Pageable pageable);

        @Query(value = """
                            SELECT * FROM flight_tracking t
                            WHERE t.flight_id = :flightId
                            ORDER BY t.update_time DESC, t.tracking_id DESC
                            LIMIT 1
                        """, nativeQuery = true)
        Optional<FlightTracking> findLastTrackingByFlightId(@Param("flightId") Long flightId);

        @Query("SELECT ft FROM FlightTracking ft WHERE ft.updateTime < :date")
        List<FlightTracking> findByUpdateTimeBefore(LocalDateTime date);

        @Modifying
        @Transactional
        @Query("DELETE FROM FlightTracking ft WHERE ft.updateTime < :date")
        void deleteByUpdateTimeBefore(LocalDateTime date);

        // Methods for IntelligentStorageService
        @Query("SELECT ft FROM FlightTracking ft WHERE ft.hexident = :hexident AND ft.lastSeen BETWEEN :fromTime AND :toTime ORDER BY ft.lastSeen ASC")
        List<FlightTracking> findByHexIdentAndLastSeenBetweenOrderByLastSeenAsc(@Param("hexident") String hexident,
                        @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);

        @Query("SELECT COUNT(ft) FROM FlightTracking ft WHERE ft.lastSeen > :afterTime")
        long countByLastSeenAfter(@Param("afterTime") LocalDateTime afterTime);
}
