package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftRequest;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAircraftService {
    List<Aircraft> getAll();

    Page<Aircraft> getAllPaginated(Pageable pageable);

    Aircraft getAircraftById(Long id);

    Aircraft save(AircraftRequest aircraftRequest, Long userId);

    Aircraft updateAircraft(Long id, AircraftRequest aircraftRequest, Long userId);

    void deleteAircraft(Long id);

    Aircraft findByHexident(String hexident);
}
