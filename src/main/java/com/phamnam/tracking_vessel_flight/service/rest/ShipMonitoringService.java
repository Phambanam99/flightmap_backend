package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.ShipMonitoringRequest;
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

@Service
public class ShipMonitoringService implements IShipMonitoringService {

    @Autowired
    private ShipMonitoringRepository shipMonitoringRepository;

    @Autowired
    private ShipRepository shipRepository;

    @Override
    public List<ShipMonitoring> getAll() {
        return shipMonitoringRepository.findAll();
    }

    @Override
    public Page<ShipMonitoring> getAllPaginated(Pageable pageable) {
        return shipMonitoringRepository.findAll(pageable);
    }

    @Override
    public ShipMonitoring getById(Long id) {
        return shipMonitoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShipMonitoring", "id", id));
    }

    @Override
    public List<ShipMonitoring> getByUserId(Long userId) {
        return shipMonitoringRepository.findByUserId(userId);
    }

    @Override
    public List<ShipMonitoring> getByShipId(Long shipId) {
        return shipMonitoringRepository.findByShip_Id(shipId);
    }

    @Override
    public ShipMonitoring save(ShipMonitoringRequest request) {
        Ship ship = shipRepository.findById(request.getShipId())
                .orElseThrow(() -> new ResourceNotFoundException("Ship", "id", request.getShipId()));

        ShipMonitoring monitoring = ShipMonitoring.builder()
                .userId(request.getUserId())
                .ship(ship)
                .createdAt(LocalDateTime.now())
                .build();

        return shipMonitoringRepository.save(monitoring);
    }

    @Override
    public void delete(Long id) {
        ShipMonitoring monitoring = getById(id);
        shipMonitoringRepository.delete(monitoring);
    }

    @Override
    @Transactional
    public void deleteByUserIdAndShipId(Long userId, Long shipId) {
        shipMonitoringRepository.deleteByUserIdAndShip_Id(userId, shipId);
    }
}
