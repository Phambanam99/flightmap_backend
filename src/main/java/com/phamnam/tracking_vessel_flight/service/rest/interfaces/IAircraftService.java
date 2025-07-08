package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftRequest;
import com.phamnam.tracking_vessel_flight.dto.response.AircraftResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAircraftService {
    List<AircraftResponse> getAll();

    Page<AircraftResponse> getAllPaginated(Pageable pageable);

    AircraftResponse getAircraftById(Long id);

    AircraftResponse save(AircraftRequest aircraftRequest, Long userId);

    AircraftResponse updateAircraft(Long id, AircraftRequest aircraftRequest, Long userId);

    void deleteAircraft(Long id);

    AircraftResponse findByHexident(String hexident);
}
