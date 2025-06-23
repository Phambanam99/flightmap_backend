package com.phamnam.tracking_vessel_flight.service.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.DataSource;
import com.phamnam.tracking_vessel_flight.models.DataSourceStatus;
import com.phamnam.tracking_vessel_flight.models.enums.DataSourceType;
import com.phamnam.tracking_vessel_flight.models.enums.SourceStatus;
import com.phamnam.tracking_vessel_flight.repository.DataSourceRepository;
import com.phamnam.tracking_vessel_flight.repository.DataSourceStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdsbExchangeApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceStatusRepository dataSourceStatusRepository;

    // ADS-B Exchange Configuration
    @Value("${external.api.adsbexchange.enabled:false}")
    private boolean adsbExchangeEnabled;

    @Value("${external.api.adsbexchange.base-url}")
    private String adsbExchangeBaseUrl;

    @Value("${external.api.adsbexchange.api-key}")
    private String adsbExchangeApiKey;

    @Value("${external.api.adsbexchange.timeout:10000}")
    private int adsbExchangeTimeout;

    // Geographic bounds
    @Value("${external.api.bounds.min-latitude:8.5}")
    private double minLatitude;

    @Value("${external.api.bounds.max-latitude:23.5}")
    private double maxLatitude;

    @Value("${external.api.bounds.min-longitude:102.0}")
    private double minLongitude;

    @Value("${external.api.bounds.max-longitude:109.5}")
    private double maxLongitude;

    /**
     * Fetch aircraft data from ADS-B Exchange API
     */
    @Async
    public CompletableFuture<List<AircraftTrackingRequest>> fetchAircraftData() {
        if (!adsbExchangeEnabled) {
            log.debug("ADS-B Exchange API is disabled");
            return CompletableFuture.completedFuture(List.of());
        }

        DataSource dataSource = getOrCreateDataSource(DataSourceType.ADS_B.getDisplayName(), DataSourceType.ADS_B);

        try {
            log.debug("Fetching aircraft data from ADS-B Exchange API...");

            String url = buildAdsbExchangeUrl();
            HttpHeaders headers = createHeaders(adsbExchangeApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<AircraftTrackingRequest> aircraftData = parseAdsbExchangeResponse(response.getBody());

                updateDataSourceStatus(dataSource, SourceStatus.HEALTHY,
                        "Successfully fetched " + aircraftData.size() + " aircraft");

                log.info("Successfully fetched {} aircraft from ADS-B Exchange", aircraftData.size());
                return CompletableFuture.completedFuture(aircraftData);
            } else {
                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.debug("ADS-B Exchange API endpoint not found (404). This is expected if mock service is not running.");
            updateDataSourceStatus(dataSource, SourceStatus.ERROR,
                    "API endpoint not found - possibly mock service not running");
            return CompletableFuture.completedFuture(List.of());
        } catch (ResourceAccessException e) {
            log.debug("Failed to connect to ADS-B Exchange API: {}. Continuing without external data.", e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Connection failed: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        } catch (Exception e) {
            log.debug("Failed to fetch aircraft data from ADS-B Exchange: {}. Continuing without external data.",
                    e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Error: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    /**
     * Build ADS-B Exchange API URL with geographic bounds
     */
    private String buildAdsbExchangeUrl() {
        // Mock API format for simulator
        return String.format(
                "%s?bounds={\"minLat\":%f,\"maxLat\":%f,\"minLon\":%f,\"maxLon\":%f}",
                adsbExchangeBaseUrl, minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    /**
     * Parse ADS-B Exchange API response
     */
    private List<AircraftTrackingRequest> parseAdsbExchangeResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode aircraft = root.get("aircraft"); // ADS-B Exchange typically uses "aircraft" array

            if (aircraft == null || !aircraft.isArray()) {
                // Try alternative structure
                aircraft = root.get("ac");
                if (aircraft == null || !aircraft.isArray()) {
                    return List.of();
                }
            }

            java.util.List<AircraftTrackingRequest> aircraftList = new java.util.ArrayList<>();

            aircraft.elements().forEachRemaining(ac -> {
                AircraftTrackingRequest aircraftRequest = parseAircraftFromAdsbExchange(ac);
                if (aircraftRequest != null) {
                    aircraftList.add(aircraftRequest);
                }
            });

            return aircraftList;
        } catch (Exception e) {
            log.error("Failed to parse ADS-B Exchange response", e);
            return List.of();
        }
    }

    /**
     * Parse individual aircraft data from ADS-B Exchange format
     */
    private AircraftTrackingRequest parseAircraftFromAdsbExchange(JsonNode data) {
        try {
            return AircraftTrackingRequest.builder()
                    .hexident(data.get("hex") != null ? data.get("hex").asText()
                            : data.get("icao") != null ? data.get("icao").asText() : null)
                    .latitude(data.get("lat") != null ? data.get("lat").asDouble() : null)
                    .longitude(data.get("lon") != null ? data.get("lon").asDouble() : null)
                    .altitude(data.get("alt_baro") != null ? data.get("alt_baro").asInt()
                            : data.get("altitude") != null ? data.get("altitude").asInt() : null)
                    .groundSpeed(data.get("gs") != null ? data.get("gs").asInt()
                            : data.get("speed") != null ? data.get("speed").asInt() : null)
                    .track(data.get("track") != null ? data.get("track").asInt()
                            : data.get("heading") != null ? data.get("heading").asInt() : null)
                    .verticalRate(data.get("vert_rate") != null ? data.get("vert_rate").asInt()
                            : data.get("vr") != null ? data.get("vr").asInt() : null)
                    .squawk(data.get("squawk") != null ? data.get("squawk").asText() : null)
                    .aircraftType(data.get("t") != null ? data.get("t").asText()
                            : data.get("type") != null ? data.get("type").asText() : null)
                    .registration(data.get("r") != null ? data.get("r").asText()
                            : data.get("reg") != null ? data.get("reg").asText() : null)
                    .callsign(data.get("flight") != null ? data.get("flight").asText().trim()
                            : data.get("callsign") != null ? data.get("callsign").asText().trim() : null)
                    .onGround(data.get("alt_baro") != null && data.get("alt_baro").asInt() == 0)
                    .emergency(data.get("squawk") != null &&
                            ("7500".equals(data.get("squawk").asText()) ||
                                    "7600".equals(data.get("squawk").asText()) ||
                                    "7700".equals(data.get("squawk").asText())))
                    .timestamp(LocalDateTime.now())
                    .dataQuality(0.88) // ADS-B Exchange generally has good quality data
                    // Additional ADS-B Exchange specific fields
                    .trueAirspeed(data.get("tas") != null ? data.get("tas").asDouble() : null)
                    .magneticHeading(data.get("mag_heading") != null ? data.get("mag_heading").asDouble() : null)
                    .transponderCode(data.get("squawk") != null ? data.get("squawk").asText() : null)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse aircraft data from ADS-B Exchange: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create HTTP headers for ADS-B Exchange API
     */
    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TrackingSystem/1.0");
        headers.set("Accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate");
        if (apiKey != null && !apiKey.equals("mock_key")) {
            // ADS-B Exchange might use different auth methods
            headers.set("X-API-Key", apiKey);
            headers.set("Authorization", "Bearer " + apiKey);
        }
        return headers;
    }

    /**
     * Get or create data source record
     */
    private DataSource getOrCreateDataSource(String name, DataSourceType type) {
        return dataSourceRepository.findByName(name)
                .orElseGet(() -> {
                    DataSource dataSource = DataSource.builder()
                            .name(name)
                            .sourceType(type)
                            .isEnabled(true)
                            .isActive(true)
                            .priority(2) // Lower priority than FlightRadar24
                            .consecutiveFailures(0)
                            .build();
                    return dataSourceRepository.save(dataSource);
                });
    }

    /**
     * Update data source status
     */
    private void updateDataSourceStatus(DataSource dataSource, SourceStatus status, String message) {
        try {
            if (status == SourceStatus.HEALTHY) {
                dataSource.setLastSuccessTime(LocalDateTime.now());
                dataSource.setConsecutiveFailures(0);
                dataSource.setIsActive(true);
            } else {
                dataSource.setConsecutiveFailures(dataSource.getConsecutiveFailures() + 1);
                if (dataSource.getConsecutiveFailures() >= 3) {
                    dataSource.setIsActive(false);
                }
            }
            dataSourceRepository.save(dataSource);

            DataSourceStatus statusRecord = DataSourceStatus.builder()
                    .dataSource(dataSource)
                    .checkTime(LocalDateTime.now())
                    .responseTime(1300L) // ADS-B Exchange response time
                    .build();
            dataSourceStatusRepository.save(statusRecord);

        } catch (Exception e) {
            log.error("Failed to update ADS-B Exchange data source status", e);
        }
    }

    /**
     * Check if ADS-B Exchange API is available
     */
    public boolean isAdsbExchangeAvailable() {
        return adsbExchangeEnabled && isDataSourceHealthy(DataSourceType.ADS_B.getDisplayName());
    }

    private boolean isDataSourceHealthy(String name) {
        return dataSourceRepository.findByName(name)
                .map(DataSource::getIsActive)
                .orElse(false);
    }

    /**
     * Get ADS-B Exchange API status
     */
    public java.util.Map<String, Object> getAdsbExchangeStatus() {
        return java.util.Map.of(
                "enabled", adsbExchangeEnabled,
                "available", isAdsbExchangeAvailable(),
                "coverage", "Global ADS-B data with focus on US/Europe",
                "dataSource", "Community-driven ADS-B receivers",
                "priority", 2);
    }
}