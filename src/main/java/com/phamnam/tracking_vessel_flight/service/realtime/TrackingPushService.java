package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void pushFlightUpdate(FlightTrackingRequest tracking) {
        log.info("Pushing flight update via WebSocket for flight ID: {}", tracking.getFlightId());
        messagingTemplate.convertAndSend("/topic/flight-tracking", tracking);
    }

    public void pushVesselUpdate(ShipTrackingRequest tracking) {
        log.info("Pushing vessel update via WebSocket for vessel ID: {}", tracking.getVoyageId());
        messagingTemplate.convertAndSend("/topic/ship-tracking", tracking);
    }
}