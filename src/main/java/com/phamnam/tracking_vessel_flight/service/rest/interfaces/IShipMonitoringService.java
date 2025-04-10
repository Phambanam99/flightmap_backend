package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.ShipMonitoringRequest;
import com.phamnam.tracking_vessel_flight.models.ShipMonitoring;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IShipMonitoringService {
    List<ShipMonitoring> getAll();

    Page<ShipMonitoring> getAllPaginated(Pageable pageable);

    ShipMonitoring getById(Long id);

    List<ShipMonitoring> getByUserId(Long userId);

    List<ShipMonitoring> getByShipId(Long shipId);

    ShipMonitoring save(ShipMonitoringRequest request);

    void delete(Long id);

    void deleteByUserIdAndShipId(Long userId, Long shipId);
}
