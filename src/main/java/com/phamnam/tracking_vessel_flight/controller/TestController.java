package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.service.realtime.externalApi.AdsbExchangeApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final AdsbExchangeApiService adsbExchangeApiService;

    @GetMapping("/adsbexchange")
    public String testAdsbExchange() {
        try {
            log.info("🧪 DIRECT TEST: Calling ADS-B Exchange service...");

            CompletableFuture<List<AircraftTrackingRequest>> future = adsbExchangeApiService.fetchAircraftData();
            List<AircraftTrackingRequest> result = future.get(); // Synchronous call for testing

            log.info("🧪 DIRECT TEST: ADS-B Exchange returned {} aircraft", result != null ? result.size() : 0);

            return String.format("ADS-B Exchange test completed. Returned %d aircraft",
                    result != null ? result.size() : 0);
        } catch (Exception e) {
            log.error("🧪 DIRECT TEST: ADS-B Exchange failed", e);
            return "ADS-B Exchange test failed: " + e.getMessage();
        }
    }

    @GetMapping("/adsbexchange/raw")
    public String testAdsbExchangeRaw() {
        try {
            log.info("🧪 RAW TEST: Getting raw response from mock API...");

            // Direct call to mock API
            String url = "http://localhost:3001/api/mock/adsbexchange";
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            log.info("🧪 RAW TEST: Response length: {}", response != null ? response.length() : 0);

            if (response != null && response.length() > 1000) {
                return "Raw response received. Length: " + response.length() +
                        ". First 500 chars: " + response.substring(0, 500) + "...";
            } else {
                return "Raw response: " + response;
            }
        } catch (Exception e) {
            log.error("🧪 RAW TEST: Failed", e);
            return "Raw test failed: " + e.getMessage();
        }
    }
}