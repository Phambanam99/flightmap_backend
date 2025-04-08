package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.UserFlight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFlightRepository extends JpaRepository<UserFlight, Long> {
}
