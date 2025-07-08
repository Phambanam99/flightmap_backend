package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.ShipRequest;
import com.phamnam.tracking_vessel_flight.dto.response.ShipResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IShipService {
    List<ShipResponse> getAll();

    Page<ShipResponse> getAllPaginated(Pageable pageable);

    ShipResponse getShipById(Long id);

    ShipResponse save(ShipRequest shipRequest, Long userId);

    void deleteShip(Long id);

    ShipResponse updateShip(Long id, ShipRequest shipRequest, Long userId);
}
