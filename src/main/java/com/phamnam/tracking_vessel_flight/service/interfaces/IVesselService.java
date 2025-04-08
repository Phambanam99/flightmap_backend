package com.phamnam.tracking_vessel_flight.service.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.VesselRequest;
import com.phamnam.tracking_vessel_flight.models.Vessel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IVesselService {
    List<Vessel> getAll();

    Page<Vessel> getAllPaginated(Pageable pageable);

    Vessel getVesselById(Long id);

    Vessel save(VesselRequest vesselRequest, Long userId);

    void deleteVessel(Long id);

    Vessel updateVessel(Long id, VesselRequest vesselRequest, Long userId);
}
