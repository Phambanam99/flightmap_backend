package com.phamnam.tracking_vessel_flight.service;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftRequest;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.repository.AircraftRepository;
import com.phamnam.tracking_vessel_flight.service.interfaces.IAircraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AircraftService implements IAircraftService {

    @Autowired
    private AircraftRepository aircraftRepository;

    @Override
    public List<Aircraft> getAll() {
        return aircraftRepository.findAll();
    }

    @Override
    public Page<Aircraft> getAllPaginated(Pageable pageable) {
        return aircraftRepository.findAll(pageable);
    }

    @Override
    public Aircraft getAircraftById(Long id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with id: " + id));
    }

    @Override
    public Aircraft save(AircraftRequest aircraftRequest, Long userId) {
        Aircraft aircraft = mapToEntity(aircraftRequest);
        return aircraftRepository.save(aircraft);
    }

    @Override
    public Aircraft updateAircraft(Long id, AircraftRequest aircraftRequest, Long userId) {
        Aircraft aircraft = getAircraftById(id);

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

        return aircraftRepository.save(aircraft);
    }

    @Override
    public void deleteAircraft(Long id) {
        Aircraft aircraft = getAircraftById(id);
        aircraftRepository.delete(aircraft);
    }

    @Override
    public Aircraft findByHexident(String hexident) {
        return aircraftRepository.findByHexident(hexident)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with hexident: " + hexident));
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
