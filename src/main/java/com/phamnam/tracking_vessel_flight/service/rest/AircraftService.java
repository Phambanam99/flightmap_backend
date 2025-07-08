package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftRequest;
import com.phamnam.tracking_vessel_flight.dto.response.AircraftResponse;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.repository.AircraftRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.IAircraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AircraftService implements IAircraftService {

    @Autowired
    private AircraftRepository aircraftRepository;

    public List<AircraftResponse> getAll() {
        List<Aircraft> aircraft = aircraftRepository.findAll();
        return aircraft.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<AircraftResponse> getAllPaginated(Pageable pageable) {
        Page<Aircraft> aircraft = aircraftRepository.findAll(pageable);
        return aircraft.map(this::convertToResponse);
    }

    public AircraftResponse getAircraftById(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with id: " + id));
        return convertToResponse(aircraft);
    }

    @Transactional
    public AircraftResponse save(AircraftRequest aircraftRequest, Long userId) {
        Aircraft aircraft = mapToEntity(aircraftRequest);
        Aircraft savedAircraft = aircraftRepository.save(aircraft);
        return convertToResponse(savedAircraft);
    }

    @Transactional
    public AircraftResponse updateAircraft(Long id, AircraftRequest aircraftRequest, Long userId) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with id: " + id));

        aircraft.setHexident(aircraftRequest.getHexident());
        aircraft.setRegister(aircraftRequest.getRegister());
        aircraft.setType(aircraftRequest.getType());
        aircraft.setManufacture(aircraftRequest.getManufacture());
        aircraft.setConstructorNumber(aircraftRequest.getConstructorNumber());
        aircraft.setOperator(aircraftRequest.getOperator());
        aircraft.setOperatorCode(aircraftRequest.getOperatorCode());
        aircraft.setEngines(aircraftRequest.getEngines());
        aircraft.setEngineType(aircraftRequest.getEngineType());
        aircraft.setIsMilitary(aircraftRequest.getIsMilitary());
        aircraft.setCountry(aircraftRequest.getCountry());
        aircraft.setTransponderType(aircraftRequest.getTransponderType());
        aircraft.setYear(aircraftRequest.getYear());
        aircraft.setSource(aircraftRequest.getSource());
        aircraft.setItemType(aircraftRequest.getItemType());

        Aircraft updatedAircraft = aircraftRepository.save(aircraft);
        return convertToResponse(updatedAircraft);
    }

    @Transactional
    public void deleteAircraft(Long id) {
        Aircraft aircraft = aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with id: " + id));
        aircraftRepository.delete(aircraft);
    }

    public AircraftResponse findByHexident(String hexident) {
        Aircraft aircraft = aircraftRepository.findByHexident(hexident)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with hexident: " + hexident));
        return convertToResponse(aircraft);
    }

    /**
     * Convert Aircraft entity to AircraftResponse DTO
     */
    private AircraftResponse convertToResponse(Aircraft aircraft) {
        AircraftResponse.AircraftResponseBuilder builder = AircraftResponse.builder()
                .id(aircraft.getId())
                .hexident(aircraft.getHexident())
                .registration(aircraft.getRegister())
                .aircraftType(aircraft.getType())
                .model(aircraft.getType()) // Using type as model
                .manufacturer(aircraft.getManufacture())
                .operator(aircraft.getOperator())
                .country(aircraft.getCountry())
                .createdAt(aircraft.getCreatedAt())
                .updatedAt(aircraft.getUpdatedAt());

        // Safely access user information
        if (aircraft.getUpdatedBy() != null) {
            builder.updatedByUsername(aircraft.getUpdatedBy().getUsername());
        }

        // Count active flights (simplified - just count total flights for now)
        if (aircraft.getFlights() != null) {
            builder.activeFlightCount(aircraft.getFlights().size());
        } else {
            builder.activeFlightCount(0);
        }

        return builder.build();
    }

    private Aircraft mapToEntity(AircraftRequest request) {
        return Aircraft.builder()
                .hexident(request.getHexident())
                .register(request.getRegister())
                .type(request.getType())
                .manufacture(request.getManufacture())
                .constructorNumber(request.getConstructorNumber())
                .operator(request.getOperator())
                .operatorCode(request.getOperatorCode())
                .engines(request.getEngines())
                .engineType(request.getEngineType())
                .isMilitary(request.getIsMilitary())
                .country(request.getCountry())
                .transponderType(request.getTransponderType())
                .year(request.getYear())
                .source(request.getSource())
                .itemType(request.getItemType())
                .build();
    }
}
