package com.phamnam.tracking_vessel_flight.service.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
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
public class ChinaportsApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceStatusRepository dataSourceStatusRepository;

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
    @Async
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
        // Mock API format for simulator - use main bounds instead of China-specific
        return String.format(
                "%s?bounds={\"minLat\":%f,\"maxLat\":%f,\"minLon\":%f,\"maxLon\":%f}",
                chinaportsBaseUrl, 8.5, 23.5, 102.0, 109.5);
    }

    /**
     * Parse Chinaports API response
     */
    private List<VesselTrackingRequest> parseChinaportsResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode vessels = root.get("vessels"); // Assuming the response has a "vessels" array

            if (vessels == null || !vessels.isArray()) {
                return List.of();
            }

            java.util.List<VesselTrackingRequest> vesselList = new java.util.ArrayList<>();

            vessels.elements().forEachRemaining(vessel -> {
                VesselTrackingRequest vesselRequest = parseVesselFromChinaports(vessel);
                if (vesselRequest != null) {
                    vesselList.add(vesselRequest);
                }
            });

            return vesselList;
        } catch (Exception e) {
            log.error("Failed to parse Chinaports response", e);
            return List.of();
        }
    }

    /**
     * Parse individual vessel data from Chinaports format
     */
    private VesselTrackingRequest parseVesselFromChinaports(JsonNode data) {
        try {
            return VesselTrackingRequest.builder()
                    .mmsi(data.get("mmsi") != null ? data.get("mmsi").asText() : null)
                    .latitude(data.get("lat") != null ? data.get("lat").asDouble() : null)
                    .longitude(data.get("lon") != null ? data.get("lon").asDouble() : null)
                    .speed(data.get("speed") != null ? data.get("speed").asDouble() : null)
                    .course(data.get("course") != null ? data.get("course").asInt() : null)
                    .heading(data.get("heading") != null ? data.get("heading").asInt() : null)
                    .navigationStatus(data.get("navStatus") != null ? data.get("navStatus").asText() : null)
                    .vesselName(data.get("vesselName") != null ? data.get("vesselName").asText() : null)
                    .vesselType(data.get("vesselType") != null ? data.get("vesselType").asText() : null)
                    .imo(data.get("imo") != null ? data.get("imo").asText() : null)
                    .callsign(data.get("callsign") != null ? data.get("callsign").asText() : null)
                    .flag(data.get("flag") != null ? data.get("flag").asText() : "CN") // Default to China
                    .length(data.get("length") != null ? data.get("length").asInt() : null)
                    .width(data.get("width") != null ? data.get("width").asInt() : null)
                    .draught(data.get("draught") != null ? data.get("draught").asDouble() : null)
                    .destination(data.get("destination") != null ? data.get("destination").asText() : null)
                    .eta(data.get("eta") != null ? data.get("eta").asText() : null)
                    .lastPort(data.get("lastPort") != null ? data.get("lastPort").asText() : null)
                    .nextPort(data.get("nextPort") != null ? data.get("nextPort").asText() : null)
                    .timestamp(LocalDateTime.now())
                    .dataQuality(0.85) // Chinaports generally has good quality data for Chinese waters
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse vessel data from Chinaports: {}", e.getMessage());
            return null;
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