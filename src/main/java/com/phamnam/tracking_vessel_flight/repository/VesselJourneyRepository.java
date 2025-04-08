package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.VesselJourney;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VesselJourneyRepository extends JpaRepository<VesselJourney, Long> {}
