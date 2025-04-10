package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftMonitoringRequest;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.models.AircraftMonitoring;
import com.phamnam.tracking_vessel_flight.repository.AircraftMonitoringRepository;
import com.phamnam.tracking_vessel_flight.repository.AircraftRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.IAircraftMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AircraftMonitoringService implements IAircraftMonitoringService {

    @Autowired
    private AircraftMonitoringRepository aircraftMonitoringRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Override
    public List<AircraftMonitoring> getAll() {
        return aircraftMonitoringRepository.findAll();
    }

    @Override
    public Page<AircraftMonitoring> getAllPaginated(Pageable pageable) {
        return aircraftMonitoringRepository.findAll(pageable);
    }

    @Override
    public AircraftMonitoring getById(Long id) {
        return aircraftMonitoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AircraftMonitoring", "id", id));
    }

    @Override
    public List<AircraftMonitoring> getByUserId(Long userId) {
        return aircraftMonitoringRepository.findByUserId(userId);
    }

    @Override
    public List<AircraftMonitoring> getByAircraftId(Long aircraftId) {
        return aircraftMonitoringRepository.findByAircraft_Id(aircraftId);
    }

    @Override
    public AircraftMonitoring save(AircraftMonitoringRequest request) {
        Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", request.getAircraftId()));

        AircraftMonitoring monitoring = AircraftMonitoring.builder()
                .userId(request.getUserId())
                .aircraft(aircraft)
                .createdAt(LocalDateTime.now())
                .build();

        return aircraftMonitoringRepository.save(monitoring);
    }

    @Override
    public void delete(Long id) {
        AircraftMonitoring monitoring = getById(id);
        aircraftMonitoringRepository.delete(monitoring);
    }

    @Override
    @Transactional
    public void deleteByUserIdAndAircraftId(Long userId, Long aircraftId) {
        aircraftMonitoringRepository.deleteByUserIdAndAircraft_Id(userId, aircraftId);
    }
}
