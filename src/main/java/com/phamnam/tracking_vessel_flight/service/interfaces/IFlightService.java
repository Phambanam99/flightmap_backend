package com.phamnam.tracking_vessel_flight.service.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.models.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IFlightService {
    List<Flight> getAll();

    Page<Flight> getAllPaginated(Pageable pageable);

    Flight getFlightById(Long id);

    List<Flight> getFlightsByAircraftId(Long aircraftId);

    Flight save(FlightRequest flightRequest, Long userId);

    Flight updateFlight(Long id, FlightRequest flightRequest, Long userId);

    void deleteFlight(Long id);
}
