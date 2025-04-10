package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.ShipRequest;
import com.phamnam.tracking_vessel_flight.models.Ship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IShipService {
    List<Ship> getAll();

    Page<Ship> getAllPaginated(Pageable pageable);

    Ship getShipById(Long id);

    Ship save(ShipRequest shipRequest, Long userId);

    void deleteShip(Long id);

    Ship updateShip(Long id, ShipRequest shipRequest, Long userId);
}
