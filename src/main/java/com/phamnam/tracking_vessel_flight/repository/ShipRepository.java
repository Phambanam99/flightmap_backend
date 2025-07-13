package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.Ship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShipRepository extends JpaRepository<Ship, Long> {
    // JpaRepository provides built-in methods:
    // save(), findById(), findAll(), deleteById(), etc.

    /**
     * Find ship by MMSI
     */
    Optional<Ship> findByMmsi(String mmsi);

    /**
     * Find ship by IMO number
     */
    Optional<Ship> findByImo(String imo);

    /**
     * Find ship by callsign
     */
    Optional<Ship> findByCallsign(String callsign);

    /**
     * Find ship by name
     */
    Optional<Ship> findByName(String name);

    /**
     * Check if ship exists by MMSI
     */
    boolean existsByMmsi(String mmsi);

    /**
     * Find active ships
     */
    @Query("SELECT s FROM Ship s WHERE s.isActive = true")
    java.util.List<Ship> findActiveShips();

    /**
     * Find ships by type
     */
    java.util.List<Ship> findByShipType(String shipType);

    /**
     * Find ships by flag
     */
    java.util.List<Ship> findByFlag(String flag);
}
