package com.phamnam.tracking_vessel_flight.service;

import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.repository.FlightRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.service.interfaces.IFlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlightService implements IFlightService {
    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Aircraft> getAll() {
        return flightRepository.findAll();
    }

    public Page<Aircraft> getAllPaginated(Pageable pageable) {
        return flightRepository.findAll(pageable);
    }

    public Aircraft getFlightById(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
    }

    public Aircraft save(FlightRequest flightRequest, Long userId) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        Aircraft aircraft = Aircraft.builder()
                .hexident(flightRequest.getHexident())
                .register(flightRequest.getRegister())
                .type(flightRequest.getType())
                .manufacture(flightRequest.getManufacture())
                .constructorNumber(flightRequest.getConstructorNumber())
                .operator(flightRequest.getOperator())
                .operatorCode(flightRequest.getOperatorCode())
                .engines(flightRequest.getEngines())
                .engineType(flightRequest.getEngineType())
                .isMilitary(flightRequest.getIsMilitary())
                .country(flightRequest.getCountry())
                .transponderType(flightRequest.getTransponderType())
                .year(flightRequest.getYear())
                .source(flightRequest.getSource())
                .itemType(flightRequest.getItemType())
                .build();

        aircraft.setUpdatedBy(user);

        return flightRepository.save(aircraft);
    }

    public Aircraft updateFlight(Long id, FlightRequest flightRequest, Long userId) {
        Aircraft aircraft = getFlightById(id);

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        aircraft.setHexident(flightRequest.getHexident());
        aircraft.setRegister(flightRequest.getRegister());
        aircraft.setType(flightRequest.getType());
        aircraft.setManufacture(flightRequest.getManufacture());
        aircraft.setConstructorNumber(flightRequest.getConstructorNumber());
        aircraft.setOperator(flightRequest.getOperator());
        aircraft.setOperatorCode(flightRequest.getOperatorCode());
        aircraft.setEngines(flightRequest.getEngines());
        aircraft.setEngineType(flightRequest.getEngineType());
        aircraft.setIsMilitary(flightRequest.getIsMilitary());
        aircraft.setCountry(flightRequest.getCountry());
        aircraft.setTransponderType(flightRequest.getTransponderType());
        aircraft.setYear(flightRequest.getYear());
        aircraft.setSource(flightRequest.getSource());
        aircraft.setItemType(flightRequest.getItemType());

        aircraft.setUpdatedBy(user);

        return flightRepository.save(aircraft);
    }

    public void deleteFlight(Long id) {
        Aircraft aircraft = getFlightById(id);
        flightRepository.delete(aircraft);
    }
}