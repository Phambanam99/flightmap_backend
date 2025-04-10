package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftMonitoringRequest;
import com.phamnam.tracking_vessel_flight.models.AircraftMonitoring;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAircraftMonitoringService {
    List<AircraftMonitoring> getAll();

    Page<AircraftMonitoring> getAllPaginated(Pageable pageable);

    AircraftMonitoring getById(Long id);

    List<AircraftMonitoring> getByUserId(Long userId);

    List<AircraftMonitoring> getByAircraftId(Long aircraftId);

    AircraftMonitoring save(AircraftMonitoringRequest request);

    void delete(Long id);

    void deleteByUserIdAndAircraftId(Long userId, Long aircraftId);
}
