package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.VoyageRequest;
import com.phamnam.tracking_vessel_flight.dto.response.VoyageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IVoyageService {
    List<VoyageResponse> getAll();

    Page<VoyageResponse> getAllPaginated(Pageable pageable);

    VoyageResponse getVoyageById(Long id);

    VoyageResponse save(VoyageRequest voyageRequest, Long userId);

    void deleteVoyage(Long id);

    VoyageResponse updateVoyage(Long id, VoyageRequest voyageRequest, Long userId);

    List<VoyageResponse> getVoyagesByShipId(Long shipId);
}
