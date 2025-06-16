package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.response.MyApiResponse;
import com.phamnam.tracking_vessel_flight.service.realtime.KafkaProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Tag(name = "Async Events", description = "APIs for publishing tracking data to Kafka message queues for real-time processing")
public class TrackingPublisherController {

    private final KafkaProducerService kafkaProducerService;

    @Operation(summary = "Publish flight tracking data", description = "Sends flight tracking data to Kafka for processing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data published successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/flight")
    public ResponseEntity<MyApiResponse<Void>> publishFlightTracking(
            @RequestBody FlightTrackingRequestDTO trackingRequest) {
        kafkaProducerService.sendFlightTracking(trackingRequest);

        return ResponseEntity.ok(
                MyApiResponse.<Void>builder()
                        .success(true)
                        .message("Flight tracking data published to processing pipeline")
                        .build());
    }

    @Operation(summary = "Publish vessel tracking data", description = "Sends vessel tracking data to Kafka for processing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data published successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/vessel")
    public ResponseEntity<MyApiResponse<Void>> publishVesselTracking(@RequestBody ShipTrackingRequest trackingRequest) {
        kafkaProducerService.sendVesselTracking(trackingRequest);

        return ResponseEntity.ok(
                MyApiResponse.<Void>builder()
                        .success(true)
                        .message("Vessel tracking data published to processing pipeline")
                        .build());
    }
}