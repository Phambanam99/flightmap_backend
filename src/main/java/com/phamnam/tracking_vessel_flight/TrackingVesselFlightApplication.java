package com.phamnam.tracking_vessel_flight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrackingVesselFlightApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrackingVesselFlightApplication.class, args);
	}

}
