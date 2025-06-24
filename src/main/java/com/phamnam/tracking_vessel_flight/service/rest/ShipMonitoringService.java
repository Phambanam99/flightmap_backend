package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.ShipMonitoringRequest;
import com.phamnam.tracking_vessel_flight.dto.response.ShipMonitoringResponse;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Ship;
import com.phamnam.tracking_vessel_flight.models.ShipMonitoring;
import com.phamnam.tracking_vessel_flight.repository.ShipMonitoringRepository;
import com.phamnam.tracking_vessel_flight.repository.ShipRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.IShipMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ShipMonitoringService implements IShipMonitoringService {

    @Autowired
    private ShipMonitoringRepository shipMonitoringRepository;

    @Autowired
    private ShipRepository shipRepository;

    public List<ShipMonitoringResponse> getAll() {
        List<ShipMonitoring> monitorings = shipMonitoringRepository.findAll();
        return monitorings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<ShipMonitoringResponse> getAllPaginated(Pageable pageable) {
        Page<ShipMonitoring> monitorings = shipMonitoringRepository.findAll(pageable);
        return monitorings.map(this::convertToResponse);
    }

    public ShipMonitoringResponse getById(Long id) {
        ShipMonitoring monitoring = shipMonitoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipMonitoring", "id", id));
        return convertToResponse(monitoring);
    }

    public List<ShipMonitoringResponse> getByUserId(Long userId) {
        List<ShipMonitoring> monitorings = shipMonitoringRepository.findByUserId(userId);
        return monitorings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ShipMonitoringResponse> getByShipId(Long shipId) {
        List<ShipMonitoring> monitorings = shipMonitoringRepository.findByShip_Id(shipId);
        return monitorings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShipMonitoringResponse save(ShipMonitoringRequest request) {
        Ship ship = shipRepository.findById(request.getShipId())
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", request.getShipId()));

        ShipMonitoring monitoring = ShipMonitoring.builder()
                .userId(request.getUserId())
                .ship(ship)
                .createdAt(LocalDateTime.now())
                .build();

        ShipMonitoring savedMonitoring = shipMonitoringRepository.save(monitoring);
        return convertToResponse(savedMonitoring);
    }

    @Transactional
    public void delete(Long id) {
        ShipMonitoring monitoring = shipMonitoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipMonitoring", "id", id));
        shipMonitoringRepository.delete(monitoring);
    }

    @Transactional
    public void deleteByUserIdAndShipId(Long userId, Long shipId) {
        shipMonitoringRepository.deleteByUserIdAndShip_Id(userId, shipId);
    }

    /**
     * Convert ShipMonitoring entity to ShipMonitoringResponse DTO
     */
    private ShipMonitoringResponse convertToResponse(ShipMonitoring monitoring) {
        ShipMonitoringResponse.ShipMonitoringResponseBuilder builder = ShipMonitoringResponse.builder()
                .id(monitoring.getId())
                .isActive(true) // Default active status
                .createdAt(monitoring.getCreatedAt());

        // Safely access ship information
        if (monitoring.getShip() != null) {
            builder.shipId(monitoring.getShip().getId())
                    .shipName(monitoring.getShip().getName())
                    .mmsi(monitoring.getShip().getMmsi())
                    .imo(monitoring.getShip().getImo());
        }

        return builder.build();
    }
}
