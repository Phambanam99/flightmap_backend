package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.models.Flight;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.repository.FlightRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.IFlightService;
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
    private com.phamnam.tracking_vessel_flight.repository.FlightRepository aircraftRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Flight> getAll() {
        return flightRepository.findAll();
    }

    @Override
    public Page<Flight> getAllPaginated(Pageable pageable) {
        return flightRepository.findAll(pageable);
    }

    @Override
    public Flight getFlightById(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
    }

    @Override
    public List<Flight> getFlightsByAircraftId(Long aircraftId) {
        return flightRepository.findByAircraft_id(aircraftId);
    }

    @Override
    public Flight save(FlightRequest flightRequest, Long userId) {
        Aircraft aircraft = aircraftRepository.findById(flightRequest.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", flightRequest.getAircraftId())).getAircraft();

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        Flight flight = Flight.builder()
                .aircraft(aircraft)
                .callsign(flightRequest.getCallsign())
                .departureTime(flightRequest.getDepartureTime())
                .arrivalTime(flightRequest.getArrivalTime())
                .status(flightRequest.getStatus())
                .originAirport(flightRequest.getOriginAirport())
                .destinationAirport(flightRequest.getDestinationAirport())
                .build();

        flight.setUpdatedBy(user);

        return flightRepository.save(flight);
    }

    @Override
    public Flight updateFlight(Long id, FlightRequest flightRequest, Long userId) {
        Flight flight = getFlightById(id);

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        if (flightRequest.getAircraftId() != null) {
            Aircraft aircraft = aircraftRepository.findById(flightRequest.getAircraftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", flightRequest.getAircraftId())).getAircraft();
            flight.setAircraft(aircraft);
        }

        if (flightRequest.getCallsign() != null) {
            flight.setCallsign(flightRequest.getCallsign());
        }

        if (flightRequest.getDepartureTime() != null) {
            flight.setDepartureTime(flightRequest.getDepartureTime());
        }

        if (flightRequest.getArrivalTime() != null) {
            flight.setArrivalTime(flightRequest.getArrivalTime());
        }

        if (flightRequest.getStatus() != null) {
            flight.setStatus(flightRequest.getStatus());
        }

        if (flightRequest.getOriginAirport() != null) {
            flight.setOriginAirport(flightRequest.getOriginAirport());
        }

        if (flightRequest.getDestinationAirport() != null) {
            flight.setDestinationAirport(flightRequest.getDestinationAirport());
        }

        flight.setUpdatedBy(user);

        return flightRepository.save(flight);
    }

    @Override
    public void deleteFlight(Long id) {
        Flight flight = getFlightById(id);
        flightRepository.delete(flight);
    }
}