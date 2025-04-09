package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    List<Flight> findByAircraft_id(Long aircraftId);

    @Query("""
                SELECT f FROM Flight f
                WHERE f.aircraft.id = :aircraftId
                ORDER BY f.departureTime DESC
                LIMIT 1
            """)
    Flight findLatestByAircraftId(@Param("aircraftId") Long aircraftId);
}