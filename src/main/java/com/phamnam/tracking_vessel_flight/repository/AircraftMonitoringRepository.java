package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.AircraftMonitoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AircraftMonitoringRepository extends JpaRepository<AircraftMonitoring, Long> {
    List<AircraftMonitoring> findByUserId(Long userId);

    List<AircraftMonitoring> findByAircraft_Id(Long aircraftId);

    void deleteByUserIdAndAircraft_Id(Long userId, Long aircraftId);
}
