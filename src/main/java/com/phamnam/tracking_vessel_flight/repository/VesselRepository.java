package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.Vessel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VesselRepository extends JpaRepository<Vessel, Long> {
    // JpaRepository provides built-in methods:
    // save(), findById(), findAll(), deleteById(), etc.
}
