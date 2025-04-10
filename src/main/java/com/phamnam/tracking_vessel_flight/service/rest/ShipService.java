package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.ShipRequest;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Ship;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.repository.ShipRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.IShipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipService implements IShipService {
    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Ship> getAll() {
        return shipRepository.findAll();
    }

    @Override
    public Page<Ship> getAllPaginated(Pageable pageable) {
        return shipRepository.findAll(pageable);
    }

    @Override
    public Ship getShipById(Long id) {
        return shipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", id));
    }

    @Override
    public Ship save(ShipRequest shipRequest, Long userId) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        Ship ship = Ship.builder()
                .name(shipRequest.getName())
                .mmsi(shipRequest.getMmsi())
                .imo(shipRequest.getImo())
                .callsign(shipRequest.getCallsign())
                .shipType(shipRequest.getShipType())
                .flag(shipRequest.getFlag())
                .length(shipRequest.getLength())
                .width(shipRequest.getWidth())
                .buildYear(shipRequest.getBuildYear())
                .build();

        ship.setUpdatedBy(user);

        return shipRepository.save(ship);
    }

    @Override
    public void deleteShip(Long id) {
        Ship ship = getShipById(id);
        shipRepository.delete(ship);
    }

    @Override
    public Ship updateShip(Long id, ShipRequest shipRequest, Long userId) {
        Ship ship = getShipById(id);

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        ship.setName(shipRequest.getName());
        ship.setMmsi(shipRequest.getMmsi());
        ship.setImo(shipRequest.getImo());
        ship.setCallsign(shipRequest.getCallsign());
        ship.setShipType(shipRequest.getShipType());
        ship.setFlag(shipRequest.getFlag());
        ship.setLength(shipRequest.getLength());
        ship.setWidth(shipRequest.getWidth());
        ship.setBuildYear(shipRequest.getBuildYear());
        ship.setUpdatedBy(user);

        return shipRepository.save(ship);
    }
}
