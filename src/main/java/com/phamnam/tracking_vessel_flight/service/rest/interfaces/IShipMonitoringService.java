package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.ShipMonitoringRequest;
import com.phamnam.tracking_vessel_flight.dto.response.ShipMonitoringResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IShipMonitoringService {
    List<ShipMonitoringResponse> getAll();

    Page<ShipMonitoringResponse> getAllPaginated(Pageable pageable);

    ShipMonitoringResponse getById(Long id);

    List<ShipMonitoringResponse> getByUserId(Long userId);

    List<ShipMonitoringResponse> getByShipId(Long shipId);

    ShipMonitoringResponse save(ShipMonitoringRequest request);

    void delete(Long id);

    void deleteByUserIdAndShipId(Long userId, Long shipId);
}
