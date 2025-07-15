package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.models.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    List<Flight> findByAircraft_id(Long aircraftId);

    @Query("""
                SELECT f FROM Flight f
                WHERE f.aircraft.id = :aircraftId
                ORDER BY f.departureTime DESC
                LIMIT 1
            """)
    Flight findLatestByAircraftId(@Param("aircraftId") Long aircraftId);

    @Query("""
                SELECT f FROM Flight f
                WHERE f.aircraft.id = :aircraftId
                AND f.callsign = :callsign
                AND f.createdAt > :createdAfter
                ORDER BY f.createdAt DESC
            """)
    List<Flight> findByAircraftIdAndCallsignAndCreatedAtAfter(
            @Param("aircraftId") Long aircraftId,
            @Param("callsign") String callsign,
            @Param("createdAfter") LocalDateTime createdAfter);

    @Query("""
                SELECT f FROM Flight f
                WHERE f.aircraft = :aircraft
                AND f.callsign = :callsign
                AND f.status = :status
                ORDER BY f.createdAt DESC
                LIMIT 1
            """)
    Optional<Flight> findByAircraftAndCallsignAndStatus(
            @Param("aircraft") Aircraft aircraft,
            @Param("callsign") String callsign,
            @Param("status") Flight.FlightStatus status);

    // Alternative method using native Spring Data method naming
    Optional<Flight> findFirstByAircraftAndCallsignAndStatusOrderByCreatedAtDesc(
            Aircraft aircraft, String callsign, Flight.FlightStatus status);
}