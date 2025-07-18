package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.ShipTrackingRequestDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendFlightTracking(FlightTrackingRequestDTO tracking) {
        String topic = "flight-tracking";
        String key = tracking.getId() != null ? tracking.getId().toString() : "unknown";
        kafkaTemplate.send(topic, key, tracking);
        log.info("Flight tracking data sent to Kafka: {}", tracking);
    }

    public void sendVesselTracking(ShipTrackingRequest tracking) {
        String topic = "ship-tracking";
        String key = tracking.getVoyageId() != null ? tracking.getVoyageId().toString() : "unknown";

        kafkaTemplate.send(topic, key, tracking);
        log.info("Vessel tracking data sent to Kafka: {}", tracking);
    }

    public void sendVesselTrackingDTO(ShipTrackingRequestDTO tracking) {
        String topic = "ship-tracking-dto";
        String key = tracking.getId() != null ? tracking.getId().toString() : "unknown";
        kafkaTemplate.send(topic, key, tracking);
        log.info("Vessel tracking DTO data sent to Kafka: {}", tracking);
    }
}