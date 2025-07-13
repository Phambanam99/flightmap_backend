package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.dto.response.FlightResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IFlightService {
    List<FlightResponse> getAll();

    Page<FlightResponse> getAllPaginated(Pageable pageable);

    FlightResponse getFlightById(Long id);

    List<FlightResponse> getFlightsByAircraftId(Long aircraftId);

    FlightResponse save(FlightRequest flightRequest, Long userId);

    FlightResponse updateFlight(Long id, FlightRequest flightRequest, Long userId);

    void deleteFlight(Long id);
}
