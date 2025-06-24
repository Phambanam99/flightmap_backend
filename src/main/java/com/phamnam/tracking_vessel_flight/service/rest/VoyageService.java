package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.VoyageRequest;
import com.phamnam.tracking_vessel_flight.dto.response.VoyageResponse;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Ship;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.models.Voyage;
import com.phamnam.tracking_vessel_flight.repository.ShipRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.repository.VoyageRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.IVoyageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VoyageService implements IVoyageService {
    @Autowired
    private VoyageRepository voyageRepository;

    @Autowired
    private ShipRepository shipRepository;

    @Autowired
    private UserRepository userRepository;

    public List<VoyageResponse> getAll() {
        List<Voyage> voyages = voyageRepository.findAll();
        return voyages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<VoyageResponse> getAllPaginated(Pageable pageable) {
        Page<Voyage> voyages = voyageRepository.findAll(pageable);
        return voyages.map(this::convertToResponse);
    }

    public VoyageResponse getVoyageById(Long id) {
        Voyage voyage = voyageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voyage", "id", id));
        return convertToResponse(voyage);
    }

    @Transactional
    public VoyageResponse save(VoyageRequest voyageRequest, Long userId) {
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

        Voyage savedVoyage = voyageRepository.save(voyage);
        return convertToResponse(savedVoyage);
    }

    @Transactional
    public void deleteVoyage(Long id) {
        Voyage voyage = voyageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voyage", "id", id));
        voyageRepository.delete(voyage);
    }

    @Transactional
    public VoyageResponse updateVoyage(Long id, VoyageRequest voyageRequest, Long userId) {
        Voyage voyage = voyageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voyage", "id", id));
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

        Voyage updatedVoyage = voyageRepository.save(voyage);
        return convertToResponse(updatedVoyage);
    }

    public List<VoyageResponse> getVoyagesByShipId(Long shipId) {
        Ship ship = shipRepository.findById(shipId)
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", shipId));

        List<Voyage> voyages = voyageRepository.findAll().stream()
                .filter(voyage -> voyage.getShip().getId().equals(shipId))
                .collect(Collectors.toList());

        return voyages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Voyage entity to VoyageResponse DTO
     */
    private VoyageResponse convertToResponse(Voyage voyage) {
        VoyageResponse.VoyageResponseBuilder builder = VoyageResponse.builder()
                .id(voyage.getId())
                .voyageNumber(voyage.getVoyageNumber())
                .departureTime(voyage.getDepartureTime())
                .arrivalTime(voyage.getArrivalTime())
                .originPort(voyage.getDeparturePort())
                .destinationPort(voyage.getArrivalPort())
                .createdAt(voyage.getCreatedAt())
                .updatedAt(voyage.getUpdatedAt());

        // Safely access ship information
        if (voyage.getShip() != null) {
            builder.shipId(voyage.getShip().getId())
                    .shipName(voyage.getShip().getName())
                    .mmsi(voyage.getShip().getMmsi());
        }

        // Safely access user information
        if (voyage.getUpdatedBy() != null) {
            builder.updatedByUsername(voyage.getUpdatedBy().getUsername());
        }

        return builder.build();
    }
}
