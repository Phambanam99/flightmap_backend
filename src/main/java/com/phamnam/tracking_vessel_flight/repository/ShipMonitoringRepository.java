package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.ShipMonitoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipMonitoringRepository extends JpaRepository<ShipMonitoring, Long> {
    List<ShipMonitoring> findByUserId(Long userId);

    List<ShipMonitoring> findByShip_Id(Long shipId);

    void deleteByUserIdAndShip_Id(Long userId, Long shipId);
}
