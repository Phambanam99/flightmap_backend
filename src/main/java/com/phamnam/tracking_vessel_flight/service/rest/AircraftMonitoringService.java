package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftMonitoringRequest;
import com.phamnam.tracking_vessel_flight.dto.response.AircraftMonitoringResponse;
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
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AircraftMonitoringService implements IAircraftMonitoringService {

    @Autowired
    private AircraftMonitoringRepository aircraftMonitoringRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    public List<AircraftMonitoringResponse> getAll() {
        List<AircraftMonitoring> monitorings = aircraftMonitoringRepository.findAll();
        return monitorings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<AircraftMonitoringResponse> getAllPaginated(Pageable pageable) {
        Page<AircraftMonitoring> monitorings = aircraftMonitoringRepository.findAll(pageable);
        return monitorings.map(this::convertToResponse);
    }

    public AircraftMonitoringResponse getById(Long id) {
        AircraftMonitoring monitoring = aircraftMonitoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AircraftMonitoring", "id", id));
        return convertToResponse(monitoring);
    }

    public List<AircraftMonitoringResponse> getByUserId(Long userId) {
        List<AircraftMonitoring> monitorings = aircraftMonitoringRepository.findByUserId(userId);
        return monitorings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<AircraftMonitoringResponse> getByAircraftId(Long aircraftId) {
        List<AircraftMonitoring> monitorings = aircraftMonitoringRepository.findByAircraft_Id(aircraftId);
        return monitorings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AircraftMonitoringResponse save(AircraftMonitoringRequest request) {
        Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", request.getAircraftId()));

        AircraftMonitoring monitoring = AircraftMonitoring.builder()
                .userId(request.getUserId())
                .aircraft(aircraft)
                .createdAt(LocalDateTime.now())
                .build();

        AircraftMonitoring savedMonitoring = aircraftMonitoringRepository.save(monitoring);
        return convertToResponse(savedMonitoring);
    }

    @Transactional
    public void delete(Long id) {
        AircraftMonitoring monitoring = aircraftMonitoringRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AircraftMonitoring", "id", id));
        aircraftMonitoringRepository.delete(monitoring);
    }

    @Transactional
    public void deleteByUserIdAndAircraftId(Long userId, Long aircraftId) {
        aircraftMonitoringRepository.deleteByUserIdAndAircraft_Id(userId, aircraftId);
    }

    /**
     * Convert AircraftMonitoring entity to AircraftMonitoringResponse DTO
     */
    private AircraftMonitoringResponse convertToResponse(AircraftMonitoring monitoring) {
        AircraftMonitoringResponse.AircraftMonitoringResponseBuilder builder = AircraftMonitoringResponse.builder()
                .id(monitoring.getId())
                .isActive(true) // Default active status
                .createdAt(monitoring.getCreatedAt());

        // Safely access aircraft information
        if (monitoring.getAircraft() != null) {
            builder.aircraftId(monitoring.getAircraft().getId())
                    .aircraftRegistration(monitoring.getAircraft().getRegister())
                    .hexident(monitoring.getAircraft().getHexident());
        }

        return builder.build();
    }
}
