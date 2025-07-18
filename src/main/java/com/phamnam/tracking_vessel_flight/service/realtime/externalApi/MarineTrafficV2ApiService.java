package com.phamnam.tracking_vessel_flight.service.realtime.externalApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.response.external.MarineTrafficV2Response;
import com.phamnam.tracking_vessel_flight.models.DataSource;
import com.phamnam.tracking_vessel_flight.models.DataSourceStatus;
import com.phamnam.tracking_vessel_flight.models.enums.DataSourceType;
import com.phamnam.tracking_vessel_flight.models.enums.SourceStatus;
import com.phamnam.tracking_vessel_flight.repository.DataSourceRepository;
import com.phamnam.tracking_vessel_flight.repository.DataSourceStatusRepository;
import com.phamnam.tracking_vessel_flight.service.realtime.externalApi.mapper.ExternalApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarineTrafficV2ApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceStatusRepository dataSourceStatusRepository;
    private final ExternalApiMapper externalApiMapper;

    // MarineTraffic V2 Configuration
    @Value("${external.api.marinetrafficv2.enabled:false}")
    private boolean marineTrafficV2Enabled;

    @Value("${external.api.marinetrafficv2.base-url}")
    private String marineTrafficV2BaseUrl;

    @Value("${external.api.marinetrafficv2.api-key}")
    private String marineTrafficV2ApiKey;

    @Value("${external.api.marinetrafficv2.timeout:15000}")
    private int marineTrafficV2Timeout;

    // Geographic bounds (same as main config but can be customized)
    @Value("${external.api.bounds.min-latitude:8.5}")
    private double minLatitude;

    @Value("${external.api.bounds.max-latitude:23.5}")
    private double maxLatitude;

    @Value("${external.api.bounds.min-longitude:102.0}")
    private double minLongitude;

    @Value("${external.api.bounds.max-longitude:109.5}")
    private double maxLongitude;

    /**
     * Fetch vessel data from MarineTraffic V2 API
     */
    @Async("taskExecutor")
    public CompletableFuture<List<VesselTrackingRequest>> fetchVesselData() {
        if (!marineTrafficV2Enabled) {
            log.debug("MarineTraffic V2 API is disabled");
            return CompletableFuture.completedFuture(List.of());
        }

        DataSource dataSource = getOrCreateDataSource(DataSourceType.MARINE_TRAFFIC.getDisplayName() + " V2",
                DataSourceType.MARINE_TRAFFIC);

        try {
            log.debug("Fetching vessel data from MarineTraffic V2 API...");

            String url = buildMarineTrafficV2Url();
            HttpHeaders headers = createHeaders(marineTrafficV2ApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<VesselTrackingRequest> vesselData = parseMarineTrafficV2Response(response.getBody());

                updateDataSourceStatus(dataSource, SourceStatus.HEALTHY,
                        "Successfully fetched " + vesselData.size() + " vessels");

                log.info("Successfully fetched {} vessels from MarineTraffic V2", vesselData.size());
                return CompletableFuture.completedFuture(vesselData);
            } else {
                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.debug(
                    "MarineTraffic V2 API endpoint not found (404). This is expected if mock service is not running.");
            updateDataSourceStatus(dataSource, SourceStatus.ERROR,
                    "API endpoint not found - possibly mock service not running");
            return CompletableFuture.completedFuture(List.of());
        } catch (ResourceAccessException e) {
            log.debug("Failed to connect to MarineTraffic V2 API: {}. Continuing without external data.",
                    e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Connection failed: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        } catch (Exception e) {
            log.debug("Failed to fetch vessel data from MarineTraffic V2: {}. Continuing without external data.",
                    e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Error: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    /**
     * Build MarineTraffic V2 API URL
     */
    private String buildMarineTrafficV2Url() {
        // Mock API format for simulator - encode JSON properly
        try {
            String boundsJson = String.format("{\"minLat\":%.6f,\"maxLat\":%.6f,\"minLon\":%.6f,\"maxLon\":%.6f}",
                    minLatitude, maxLatitude, minLongitude, maxLongitude);
            return String.format("%s?bounds=%s", marineTrafficV2BaseUrl,
                    java.net.URLEncoder.encode(boundsJson, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Fallback to simple URL if encoding fails
            log.warn("Failed to encode bounds JSON for MarineTraffic V2 URL: {}", e.getMessage());
            return marineTrafficV2BaseUrl;
        }
    }

    /**
     * Parse MarineTraffic V2 API response
     */
    /**
     * Parse MarineTraffic V2 API response using ObjectMapper for direct DTO mapping
     */
    private List<VesselTrackingRequest> parseMarineTrafficV2Response(String responseBody) {
        try {
            // Use ObjectMapper to directly map JSON to DTO
            MarineTrafficV2Response response = objectMapper.readValue(responseBody, MarineTrafficV2Response.class);

            if (response.getVessels() == null || response.getVessels().isEmpty()) {
                return List.of();
            }

            // Convert each vessel using the mapper
            return response.getVessels().stream()
                    .map(externalApiMapper::fromMarineTrafficV2)
                    .filter(vessel -> vessel != null) // Filter out null results
                    .toList();

        } catch (Exception e) {
            log.error("Failed to parse MarineTraffic V2 response using ObjectMapper", e);
            return List.of();
        }
    }

    /**
     * Create HTTP headers for MarineTraffic V2 API
     */
    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TrackingSystem/1.0");
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        if (apiKey != null && !apiKey.equals("mock_key")) {
            headers.set("X-API-Key", apiKey);
            // V2 might use different auth method
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
                            .priority(4) // Lower priority than main MarineTraffic
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
            checkHealthSource(dataSource, status, dataSourceRepository);

            DataSourceStatus statusRecord = DataSourceStatus.builder()
                    .dataSource(dataSource)
                    .checkTime(LocalDateTime.now())
                    .responseTime(1200L) // V2 might be faster than V1
                    .build();
            dataSourceStatusRepository.save(statusRecord);

        } catch (Exception e) {
            log.error("Failed to update MarineTraffic V2 data source status", e);
        }
    }

    static void checkHealthSource(DataSource dataSource, SourceStatus status,
            DataSourceRepository dataSourceRepository) {
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
    }

    /**
     * Check if MarineTraffic V2 API is available
     */
    public boolean isMarineTrafficV2Available() {
        return marineTrafficV2Enabled && isDataSourceHealthy(DataSourceType.MARINE_TRAFFIC.getDisplayName() + " V2");
    }

    private boolean isDataSourceHealthy(String name) {
        return dataSourceRepository.findByName(name)
                .map(DataSource::getIsActive)
                .orElse(false);
    }

    /**
     * Get MarineTraffic V2 API status
     */
    public java.util.Map<String, Object> getMarineTrafficV2Status() {
        return java.util.Map.of(
                "enabled", marineTrafficV2Enabled,
                "available", isMarineTrafficV2Available(),
                "version", "v2",
                "improvements", "Enhanced data quality and additional fields",
                "priority", 4);
    }
}