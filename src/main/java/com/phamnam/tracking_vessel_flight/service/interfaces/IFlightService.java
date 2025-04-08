package com.phamnam.tracking_vessel_flight.service.interfaces;

import com.phamnam.tracking_vessel_flight.dto.request.FlightRequest;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IFlightService {
    List<Aircraft> getAll();

    Page<Aircraft> getAllPaginated(Pageable pageable);

    Aircraft getFlightById(Long id);

    Aircraft save(FlightRequest flightRequest, Long userId);

    Aircraft updateFlight(Long id, FlightRequest flightRequest, Long userId);

    void deleteFlight(Long id);
}
