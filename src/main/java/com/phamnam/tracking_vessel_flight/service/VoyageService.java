package com.phamnam.tracking_vessel_flight.service;

import com.phamnam.tracking_vessel_flight.dto.request.VoyageRequest;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Ship;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.models.Voyage;
import com.phamnam.tracking_vessel_flight.repository.ShipRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.repository.VoyageRepository;
import com.phamnam.tracking_vessel_flight.service.interfaces.IVoyageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VoyageService implements IVoyageService {
    @Autowired
    private VoyageRepository voyageRepository;

    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Voyage> getAll() {
        return voyageRepository.findAll();
    }

    @Override
    public Page<Voyage> getAllPaginated(Pageable pageable) {
        return voyageRepository.findAll(pageable);
    }

    @Override
    public Voyage getVoyageById(Long id) {
        return voyageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voyage", "id", id));
    }

    @Override
    public Voyage save(VoyageRequest voyageRequest, Long userId) {
        Ship ship = shipRepository.findById(voyageRequest.getShipId())
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", voyageRequest.getShipId()));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        Voyage voyage = Voyage.builder()
                .voyageNumber(voyageRequest.getVoyageNumber())
                .departureTime(voyageRequest.getDepartureTime())
                .arrivalTime(voyageRequest.getArrivalTime())
                .departurePort(voyageRequest.getDeparturePort())
                .arrivalPort(voyageRequest.getArrivalPort())
                .ship(ship)
                .build();

        voyage.setUpdatedBy(user);

        return voyageRepository.save(voyage);
    }

    @Override
    public void deleteVoyage(Long id) {
        Voyage voyage = getVoyageById(id);
        voyageRepository.delete(voyage);
    }

    @Override
    public Voyage updateVoyage(Long id, VoyageRequest voyageRequest, Long userId) {
        Voyage voyage = getVoyageById(id);
        Ship ship = shipRepository.findById(voyageRequest.getShipId())
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", voyageRequest.getShipId()));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        voyage.setVoyageNumber(voyageRequest.getVoyageNumber());
        voyage.setDepartureTime(voyageRequest.getDepartureTime());
        voyage.setArrivalTime(voyageRequest.getArrivalTime());
        voyage.setDeparturePort(voyageRequest.getDeparturePort());
        voyage.setArrivalPort(voyageRequest.getArrivalPort());
        voyage.setShip(ship);
        voyage.setUpdatedBy(user);

        return voyageRepository.save(voyage);
    }

    @Override
    public List<Voyage> getVoyagesByShipId(Long shipId) {
        Ship ship = shipRepository.findById(shipId)
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", shipId));

        return voyageRepository.findAll().stream()
                .filter(voyage -> voyage.getShip().getId().equals(shipId))
                .collect(Collectors.toList());
    }
}
