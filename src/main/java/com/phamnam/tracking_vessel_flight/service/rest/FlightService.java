package com.phamnam.tracking_vessel_flight.service.rest;

import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.dto.response.FlightResponse;
import com.phamnam.tracking_vessel_flight.exception.ResourceNotFoundException;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.models.Flight;
import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.repository.AircraftRepository;
import com.phamnam.tracking_vessel_flight.repository.FlightRepository;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.IFlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FlightService implements IFlightService {

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private UserRepository userRepository;

    public List<FlightResponse> getAll() {
        List<Flight> flights = flightRepository.findAll();
        return flights.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<FlightResponse> getAllPaginated(Pageable pageable) {
        Page<Flight> flights = flightRepository.findAll(pageable);
        return flights.map(this::convertToResponse);
    }

    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
        return convertToResponse(flight);
    }

    public List<FlightResponse> getFlightsByAircraftId(Long aircraftId) {
        List<Flight> flights = flightRepository.findByAircraft_id(aircraftId);
        return flights.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FlightResponse save(FlightRequest flightRequest, Long userId) {
        Aircraft aircraft = aircraftRepository.findById(flightRequest.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", flightRequest.getAircraftId()));

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
                .status(convertStringToFlightStatus(flightRequest.getStatus()))
                .originAirport(flightRequest.getOriginAirport())
                .destinationAirport(flightRequest.getDestinationAirport())
                .build();

        flight.setUpdatedBy(user);
        Flight savedFlight = flightRepository.save(flight);

        return convertToResponse(savedFlight);
    }

    @Transactional
    public FlightResponse updateFlight(Long id, FlightRequest flightRequest, Long userId) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        }

        if (flightRequest.getAircraftId() != null) {
            Aircraft aircraft = aircraftRepository.findById(flightRequest.getAircraftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "id", flightRequest.getAircraftId()));
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
            flight.setStatus(convertStringToFlightStatus(flightRequest.getStatus()));
        }

        if (flightRequest.getOriginAirport() != null) {
            flight.setOriginAirport(flightRequest.getOriginAirport());
        }

        if (flightRequest.getDestinationAirport() != null) {
            flight.setDestinationAirport(flightRequest.getDestinationAirport());
        }

        flight.setUpdatedBy(user);
        Flight updatedFlight = flightRepository.save(flight);

        return convertToResponse(updatedFlight);
    }

    @Transactional
    public void deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
        flightRepository.delete(flight);
    }

    /**
     * Convert Flight entity to FlightResponse DTO
     */
    private FlightResponse convertToResponse(Flight flight) {
        FlightResponse.FlightResponseBuilder builder = FlightResponse.builder()
                .id(flight.getId())
                .callsign(flight.getCallsign())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .status(flight.getStatus() != null ? flight.getStatus().toString() : null)
                .originAirport(flight.getOriginAirport())
                .destinationAirport(flight.getDestinationAirport())
                .createdAt(flight.getCreatedAt())
                .updatedAt(flight.getUpdatedAt());

        // Safely access aircraft information
        if (flight.getAircraft() != null) {
            builder.aircraftId(flight.getAircraft().getId())
                    .aircraftName(flight.getAircraft().getRegister())
                    .aircraftRegistration(flight.getAircraft().getRegister())
                    .aircraftModel(flight.getAircraft().getType());
        }

        // Safely access user information
        if (flight.getUpdatedBy() != null) {
            builder.updatedByUsername(flight.getUpdatedBy().getUsername());
        }

        return builder.build();
    }

    /**
     * Convert string status to FlightStatus enum
     */
    private Flight.FlightStatus convertStringToFlightStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return Flight.FlightStatus.SCHEDULED;
        }

        try {
            // Try direct enum conversion first
            return Flight.FlightStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Handle common status mappings
            switch (status.toLowerCase()) {
                case "scheduled":
                case "planned":
                case "boarding":
                case "preparing":
                    return Flight.FlightStatus.SCHEDULED;
                case "departed":
                case "takeoff":
                case "airborne":
                    return Flight.FlightStatus.DEPARTED;
                case "in air":
                case "in_air":
                case "cruise":
                case "flying":
                    return Flight.FlightStatus.IN_AIR;
                case "approaching":
                case "descent":
                case "landing":
                    return Flight.FlightStatus.APPROACHING;
                case "landed":
                    return Flight.FlightStatus.LANDED;
                case "arrived":
                    return Flight.FlightStatus.ARRIVED;
                case "cancelled":
                case "canceled":
                    return Flight.FlightStatus.CANCELLED;
                case "delayed":
                    return Flight.FlightStatus.DELAYED;
                case "diverted":
                    return Flight.FlightStatus.DIVERTED;
                case "returned":
                    return Flight.FlightStatus.RETURNED;
                default:
                    return Flight.FlightStatus.UNKNOWN;
            }
        }
    }
}