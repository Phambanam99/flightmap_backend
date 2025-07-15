package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.repository.ShipTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

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
    
    @Autowired
    private RestTemplate restTemplate;

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

    /**
     * Test simulator connectivity to debug data retrieval issues
     */
    @GetMapping("/test-simulator")
    public ResponseEntity<Map<String, Object>> testSimulatorConnectivity() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test FlightRadar24 endpoint
            log.info("🔍 Testing FlightRadar24 simulator endpoint...");
            String flightRadar24Url = "http://localhost:3001/api/mock/flightradar24";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "DebugTest/1.0");
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> flightResponse = restTemplate.exchange(
                flightRadar24Url, HttpMethod.GET, entity, String.class);
            
            result.put("flightradar24", Map.of(
                "status", flightResponse.getStatusCode().toString(),
                "bodyLength", flightResponse.getBody() != null ? flightResponse.getBody().length() : 0,
                "bodyPreview", flightResponse.getBody() != null ? 
                    flightResponse.getBody().substring(0, Math.min(200, flightResponse.getBody().length())) : "null"
            ));
            
            log.info("✅ FlightRadar24 test successful: {} chars received", 
                flightResponse.getBody() != null ? flightResponse.getBody().length() : 0);
            
        } catch (Exception e) {
            log.error("❌ FlightRadar24 test failed: {}", e.getMessage(), e);
            result.put("flightradar24", Map.of("error", e.getMessage()));
        }
        
        try {
            // Test ADS-B Exchange endpoint
            log.info("🔍 Testing ADS-B Exchange simulator endpoint...");
            String adsbUrl = "http://localhost:3001/api/mock/adsbexchange";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "DebugTest/1.0");
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> adsbResponse = restTemplate.exchange(
                adsbUrl, HttpMethod.GET, entity, String.class);
            
            result.put("adsbexchange", Map.of(
                "status", adsbResponse.getStatusCode().toString(),
                "bodyLength", adsbResponse.getBody() != null ? adsbResponse.getBody().length() : 0,
                "bodyPreview", adsbResponse.getBody() != null ? 
                    adsbResponse.getBody().substring(0, Math.min(200, adsbResponse.getBody().length())) : "null"
            ));
            
            log.info("✅ ADS-B Exchange test successful: {} chars received", 
                adsbResponse.getBody() != null ? adsbResponse.getBody().length() : 0);
            
        } catch (Exception e) {
            log.error("❌ ADS-B Exchange test failed: {}", e.getMessage(), e);
            result.put("adsbexchange", Map.of("error", e.getMessage()));
        }
        
        try {
            // Test MarineTraffic endpoint
            log.info("🔍 Testing MarineTraffic simulator endpoint...");
            String marineUrl = "http://localhost:3001/api/mock/marinetraffic";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "DebugTest/1.0");
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> marineResponse = restTemplate.exchange(
                marineUrl, HttpMethod.GET, entity, String.class);
            
            result.put("marinetraffic", Map.of(
                "status", marineResponse.getStatusCode().toString(),
                "bodyLength", marineResponse.getBody() != null ? marineResponse.getBody().length() : 0,
                "bodyPreview", marineResponse.getBody() != null ? 
                    marineResponse.getBody().substring(0, Math.min(200, marineResponse.getBody().length())) : "null"
            ));
            
            log.info("✅ MarineTraffic test successful: {} chars received", 
                marineResponse.getBody() != null ? marineResponse.getBody().length() : 0);
            
        } catch (Exception e) {
            log.error("❌ MarineTraffic test failed: {}", e.getMessage(), e);
            result.put("marinetraffic", Map.of("error", e.getMessage()));
        }
        
        result.put("timestamp", System.currentTimeMillis());
        result.put("summary", "Debug test completed - check logs for details");
        
        return ResponseEntity.ok(result);
    }
}
