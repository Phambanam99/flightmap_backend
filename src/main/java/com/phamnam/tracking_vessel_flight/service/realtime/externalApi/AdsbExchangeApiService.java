package com.phamnam.tracking_vessel_flight.service.realtime.externalApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.response.external.AdsbExchangeResponse;
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
    private final ExternalApiMapper externalApiMapper;

    // ADS-B Exchange Configuration
    @Value("${external.api.adsbexchange.enabled:true}")
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
    @Async("taskExecutor")
    public CompletableFuture<List<AircraftTrackingRequest>> fetchAircraftData() {
        if (!adsbExchangeEnabled) {
            log.debug("ADS-B Exchange API is disabled");
            return CompletableFuture.completedFuture(List.of());
        }

        DataSource dataSource = getOrCreateDataSource(DataSourceType.ADS_B.getDisplayName(), DataSourceType.ADS_B);

        try {
            log.info("🔄 Fetching aircraft data from ADS-B Exchange API...");

            String url = buildAdsbExchangeUrl();
            log.info("🌐 ADS-B Exchange URL: {}", url);

            HttpHeaders headers = createHeaders(adsbExchangeApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ ADS-B Exchange API response received, body length: {}",
                        response.getBody() != null ? response.getBody().length() : 0);

                List<AircraftTrackingRequest> aircraftData = parseAdsbExchangeResponse(response.getBody());

                updateDataSourceStatus(dataSource, SourceStatus.HEALTHY,
                        "Successfully fetched " + aircraftData.size() + " aircraft");

                log.info("✈️ Successfully fetched {} aircraft from ADS-B Exchange", aircraftData.size());
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
        // For mock API, just return base URL without bounds
        // Real ADS-B Exchange API would use bounds parameters
        log.info("🌐 Building ADS-B Exchange URL (mock mode): {}", adsbExchangeBaseUrl);
        return adsbExchangeBaseUrl;
    }

    /**
     * Parse ADS-B Exchange API response
     */
    /**
     * Parse ADS-B Exchange API response using ObjectMapper for direct DTO mapping
     */
    private List<AircraftTrackingRequest> parseAdsbExchangeResponse(String responseBody) {
        try {
            log.info("🔍 Parsing ADS-B Exchange response using ObjectMapper, length: {}",
                    responseBody != null ? responseBody.length() : 0);

            // Use ObjectMapper to directly map JSON to DTO
            AdsbExchangeResponse response = objectMapper.readValue(responseBody, AdsbExchangeResponse.class);
            log.info("📝 ADS-B Exchange response parsed successfully");

            List<com.phamnam.tracking_vessel_flight.dto.response.external.AdsbExchangeAircraftData> aircraft = response
                    .getActualAircraft();
            if (aircraft == null || aircraft.isEmpty()) {
                log.warn("❌ No aircraft found in ADS-B Exchange response");
                return List.of();
            }

            log.info("📊 Found {} aircraft in response", aircraft.size());

            // Convert each aircraft using the mapper
            List<AircraftTrackingRequest> result = aircraft.stream()
                    .map(externalApiMapper::fromAdsbExchange)
                    .filter(ac -> ac != null) // Filter out null results
                    .toList();

            log.info("✅ Successfully converted {} aircraft using ObjectMapper", result.size());
            return result;

        } catch (Exception e) {
            log.error("❌ Failed to parse ADS-B Exchange response using ObjectMapper", e);
            return List.of();
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
            MarineTrafficV2ApiService.checkHealthSource(dataSource, status, dataSourceRepository);

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