package com.phamnam.tracking_vessel_flight.service.realtime.externalApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.response.external.ChinaportsResponse;
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
public class ChinaportsApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceStatusRepository dataSourceStatusRepository;
    private final ExternalApiMapper externalApiMapper;

    // Chinaports Configuration
    @Value("${external.api.chinaports.enabled:false}")
    private boolean chinaportsEnabled;

    @Value("${external.api.chinaports.base-url}")
    private String chinaportsBaseUrl;

    @Value("${external.api.chinaports.api-key}")
    private String chinaportsApiKey;

    @Value("${external.api.chinaports.timeout:20000}")
    private int chinaportsTimeout;

    // Geographic bounds for China Sea area
    @Value("${external.api.bounds.china.min-latitude:18.0}")
    private double chinaMinLatitude;

    @Value("${external.api.bounds.china.max-latitude:41.0}")
    private double chinaMaxLatitude;

    @Value("${external.api.bounds.china.min-longitude:108.0}")
    private double chinaMinLongitude;

    @Value("${external.api.bounds.china.max-longitude:126.0}")
    private double chinaMaxLongitude;

    /**
     * Fetch vessel data from Chinaports API
     */
    @Async("taskExecutor")
    public CompletableFuture<List<VesselTrackingRequest>> fetchVesselData() {
        if (!chinaportsEnabled) {
            log.debug("Chinaports API is disabled");
            return CompletableFuture.completedFuture(List.of());
        }

        DataSource dataSource = getOrCreateDataSource(DataSourceType.SHIP_TRACKING.getDisplayName(),
                DataSourceType.SHIP_TRACKING);

        try {
            log.debug("Fetching vessel data from Chinaports API...");

            String url = buildChinaportsUrl();
            HttpHeaders headers = createHeaders(chinaportsApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<VesselTrackingRequest> vesselData = parseChinaportsResponse(response.getBody());

                updateDataSourceStatus(dataSource, SourceStatus.HEALTHY,
                        "Successfully fetched " + vesselData.size() + " vessels");

                log.info("Successfully fetched {} vessels from Chinaports", vesselData.size());
                return CompletableFuture.completedFuture(vesselData);
            } else {
                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Chinaports API endpoint not found (404). This is expected if mock service is not running.");
            updateDataSourceStatus(dataSource, SourceStatus.ERROR,
                    "API endpoint not found - possibly mock service not running");
            return CompletableFuture.completedFuture(List.of());
        } catch (ResourceAccessException e) {
            log.debug("Failed to connect to Chinaports API: {}. Continuing without external data.", e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Connection failed: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        } catch (Exception e) {
            log.debug("Failed to fetch vessel data from Chinaports: {}. Continuing without external data.",
                    e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Error: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    /**
     * Build Chinaports API URL with geographic bounds
     */
    private String buildChinaportsUrl() {
        // Mock API format for simulator - encode JSON properly
        try {
            String boundsJson = String.format("{\"minLat\":%.6f,\"maxLat\":%.6f,\"minLon\":%.6f,\"maxLon\":%.6f}",
                    8.5, 23.5, 102.0, 109.5); // Use main bounds instead of China-specific
            return String.format("%s?bounds=%s", chinaportsBaseUrl,
                    java.net.URLEncoder.encode(boundsJson, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Fallback to simple URL if encoding fails
            log.warn("Failed to encode bounds JSON for Chinaports URL: {}", e.getMessage());
            return chinaportsBaseUrl;
        }
    }

    /**
     * Parse Chinaports API response using ObjectMapper for direct DTO mapping
     */
    private List<VesselTrackingRequest> parseChinaportsResponse(String responseBody) {
        try {
            // Use ObjectMapper to directly map JSON to DTO
            ChinaportsResponse response = objectMapper.readValue(responseBody, ChinaportsResponse.class);

            if (response.getVessels() == null || response.getVessels().isEmpty()) {
                return List.of();
            }

            // Convert each vessel using the mapper
            return response.getVessels().stream()
                    .map(externalApiMapper::fromChinaports)
                    .filter(vessel -> vessel != null) // Filter out null results
                    .toList();

        } catch (Exception e) {
            log.error("Failed to parse Chinaports response using ObjectMapper", e);
            return List.of();
        }
    }

    /**
     * Create HTTP headers for Chinaports API
     */
    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TrackingSystem/1.0");
        headers.set("Accept", "application/json");
        if (apiKey != null && !apiKey.equals("mock_key")) {
            headers.set("X-API-Key", apiKey);
            // Chinaports might use different auth header
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
                            .priority(3) // Lower priority than main MarineTraffic
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
            MarineTrafficV2ApiService.checkHealthSource(dataSource, status, dataSourceRepository);

            DataSourceStatus statusRecord = DataSourceStatus.builder()
                    .dataSource(dataSource)
                    .checkTime(LocalDateTime.now())
                    .responseTime(1500L) // Chinaports might be slower due to geographic distance
                    .build();
            dataSourceStatusRepository.save(statusRecord);

        } catch (Exception e) {
            log.error("Failed to update Chinaports data source status", e);
        }
    }

    /**
     * Check if Chinaports API is available
     */
    public boolean isChinaportsAvailable() {
        return chinaportsEnabled && isDataSourceHealthy(DataSourceType.SHIP_TRACKING.getDisplayName());
    }

    private boolean isDataSourceHealthy(String name) {
        return dataSourceRepository.findByName(name)
                .map(DataSource::getIsActive)
                .orElse(false);
    }

    /**
     * Get Chinaports API status
     */
    public java.util.Map<String, Object> getChinaportsStatus() {
        return java.util.Map.of(
                "enabled", chinaportsEnabled,
                "available", isChinaportsAvailable(),
                "coverage", "China Sea and major Chinese ports",
                "priority", 3);
    }
}