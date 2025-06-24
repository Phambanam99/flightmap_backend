package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftMonitoringRequest;
import com.phamnam.tracking_vessel_flight.dto.response.AircraftMonitoringResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAircraftMonitoringService {
    List<AircraftMonitoringResponse> getAll();

    Page<AircraftMonitoringResponse> getAllPaginated(Pageable pageable);

    AircraftMonitoringResponse getById(Long id);

    List<AircraftMonitoringResponse> getByUserId(Long userId);

    List<AircraftMonitoringResponse> getByAircraftId(Long aircraftId);

    AircraftMonitoringResponse save(AircraftMonitoringRequest request);

    void delete(Long id);

    void deleteByUserIdAndAircraftId(Long userId, Long aircraftId);
}
