package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.repository.ShipTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple controller to check database content
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final ShipTrackingRepository shipTrackingRepository;

    /**
     * Get basic database statistics
     */
    @GetMapping("/vessel-count")
    public ResponseEntity<Map<String, Object>> getVesselCount() {
        try {
            long count = shipTrackingRepository.count();
            
            Map<String, Object> result = new HashMap<>();
            result.put("vesselRecordsInDatabase", count);
            result.put("success", true);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to count vessels: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
