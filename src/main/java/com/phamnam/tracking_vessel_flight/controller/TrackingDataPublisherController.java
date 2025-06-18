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
@RequestMapping("/api/tracking/publish")
@RequiredArgsConstructor
@Tag(name = "Tracking Data Publisher", description = "REST APIs for publishing tracking data to Kafka message queues for real-time processing. This controller handles data ingestion from external sources.")
public class TrackingDataPublisherController {

        private final KafkaProducerService kafkaProducerService;

        @Operation(summary = "Publish flight tracking data to Kafka", description = "Accepts flight tracking data and publishes it to the flight-tracking Kafka topic for real-time processing by consumers. This endpoint is typically used by data collectors, simulators, or external tracking systems.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Flight tracking data successfully published to Kafka queue"),
                        @ApiResponse(responseCode = "400", description = "Invalid flight tracking data format"),
                        @ApiResponse(responseCode = "500", description = "Internal server error - failed to publish to Kafka")
        })
        @PostMapping("/flight")
        public ResponseEntity<MyApiResponse<Void>> publishFlightTracking(
                        @Valid @RequestBody FlightTrackingRequestDTO trackingRequest) {
                kafkaProducerService.sendFlightTracking(trackingRequest);

                return ResponseEntity.ok(
                                MyApiResponse.<Void>builder()
                                                .success(true)
                                                .message("Flight tracking data published to Kafka processing pipeline")
                                                .build());
        }

        @Operation(summary = "Publish vessel tracking data to Kafka", description = "Accepts vessel/ship tracking data and publishes it to the ship-tracking Kafka topic for real-time processing by consumers. This endpoint is typically used by maritime tracking systems, AIS receivers, or vessel monitoring services.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Vessel tracking data successfully published to Kafka queue"),
                        @ApiResponse(responseCode = "400", description = "Invalid vessel tracking data format"),
                        @ApiResponse(responseCode = "500", description = "Internal server error - failed to publish to Kafka")
        })
        @PostMapping("/vessel")
        public ResponseEntity<MyApiResponse<Void>> publishVesselTracking(
                        @Valid @RequestBody ShipTrackingRequest trackingRequest) {
                kafkaProducerService.sendVesselTracking(trackingRequest);

                return ResponseEntity.ok(
                                MyApiResponse.<Void>builder()
                                                .success(true)
                                                .message("Vessel tracking data published to Kafka processing pipeline")
                                                .build());
        }
}