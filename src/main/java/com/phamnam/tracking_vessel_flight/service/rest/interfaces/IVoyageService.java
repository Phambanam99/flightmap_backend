package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.VoyageRequest;
import com.phamnam.tracking_vessel_flight.models.Voyage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IVoyageService {
    List<Voyage> getAll();

    Page<Voyage> getAllPaginated(Pageable pageable);

    Voyage getVoyageById(Long id);

    Voyage save(VoyageRequest voyageRequest, Long userId);

    void deleteVoyage(Long id);

    Voyage updateVoyage(Long id, VoyageRequest voyageRequest, Long userId);

    List<Voyage> getVoyagesByShipId(Long shipId);
}
