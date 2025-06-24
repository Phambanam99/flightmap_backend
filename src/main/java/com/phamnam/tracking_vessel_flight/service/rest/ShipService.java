package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.ShipRequest;
import com.phamnam.tracking_vessel_flight.dto.response.ShipResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ShipService implements IShipService {
    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ShipResponse> getAll() {
        List<Ship> ships = shipRepository.findAll();
        return ships.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<ShipResponse> getAllPaginated(Pageable pageable) {
        Page<Ship> ships = shipRepository.findAll(pageable);
        return ships.map(this::convertToResponse);
    }

    public ShipResponse getShipById(Long id) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", id));
        return convertToResponse(ship);
    }

    @Transactional
    public ShipResponse save(ShipRequest shipRequest, Long userId) {
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
        Ship savedShip = shipRepository.save(ship);

        return convertToResponse(savedShip);
    }

    @Transactional
    public void deleteShip(Long id) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", id));
        shipRepository.delete(ship);
    }

    @Transactional
    public ShipResponse updateShip(Long id, ShipRequest shipRequest, Long userId) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", id));

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

        Ship updatedShip = shipRepository.save(ship);
        return convertToResponse(updatedShip);
    }

    /**
     * Convert Ship entity to ShipResponse DTO
     */
    private ShipResponse convertToResponse(Ship ship) {
        ShipResponse.ShipResponseBuilder builder = ShipResponse.builder()
                .id(ship.getId())
                .name(ship.getName())
                .mmsi(ship.getMmsi())
                .imo(ship.getImo())
                .callsign(ship.getCallsign())
                .shipType(ship.getShipType())
                .flag(ship.getFlag())
                .length(ship.getLength() != null ? ship.getLength().floatValue() : null)
                .width(ship.getWidth() != null ? ship.getWidth().floatValue() : null)
                .buildYear(ship.getBuildYear())
                .createdAt(ship.getCreatedAt())
                .updatedAt(ship.getUpdatedAt());

        // Safely access user information
        if (ship.getUpdatedBy() != null) {
            builder.updatedByUsername(ship.getUpdatedBy().getUsername());
        }

        // Count active voyages (simplified - avoid loading lazy collection)
        try {
            if (ship.getVoyages() != null) {
                builder.activeVoyageCount(ship.getVoyages().size());
            } else {
                builder.activeVoyageCount(0);
            }
        } catch (Exception e) {
            // If lazy loading fails, set to 0
            builder.activeVoyageCount(0);
        }

        return builder.build();
    }
}
