package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.service.realtime.MultiSourceExternalApiService;
import com.phamnam.tracking_vessel_flight.config.TimescaleConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system-health")
@RequiredArgsConstructor
public class SystemHealthController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MultiSourceExternalApiService multiSourceExternalApiService;
    private final TimescaleConfig timescaleConfig;

    @GetMapping("/configuration")
    public ResponseEntity<Map<String, Object>> getConfigurationStatus() {
        Map<String, Object> config = new HashMap<>();

        // Poll interval configurations
        config.put("pollIntervals", multiSourceExternalApiService.getPollIntervalStatus());

        // TimescaleDB configuration
        config.put("timescale", Map.of(
                "enabled", timescaleConfig.isTimescaleEnabled(),
                "shipTrackingTable", timescaleConfig.getShipTrackingTable(),
                "aircraftTrackingTable", timescaleConfig.getAircraftTrackingTable(),
                "timeColumn", timescaleConfig.getTimeColumn()));

        return ResponseEntity.ok(config);
    }
}