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
public class VesselFinderApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceStatusRepository dataSourceStatusRepository;

    // VesselFinder Configuration
    @Value("${external.api.vesselfinder.enabled:false}")
    private boolean vesselFinderEnabled;

    @Value("${external.api.vesselfinder.base-url}")
    private String vesselFinderBaseUrl;

    @Value("${external.api.vesselfinder.api-key}")
    private String vesselFinderApiKey;

    @Value("${external.api.vesselfinder.timeout:15000}")
    private int vesselFinderTimeout;

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
     * Fetch vessel data from VesselFinder API
     */
    @Async
    public CompletableFuture<List<VesselTrackingRequest>> fetchVesselData() {
        if (!vesselFinderEnabled) {
            log.debug("VesselFinder API is disabled");
            return CompletableFuture.completedFuture(List.of());
        }

        DataSource dataSource = getOrCreateDataSource(DataSourceType.VESSEL_FINDER.getDisplayName(),
                DataSourceType.VESSEL_FINDER);

        try {
            log.debug("Fetching vessel data from VesselFinder API...");

            String url = buildVesselFinderUrl();
            HttpHeaders headers = createHeaders(vesselFinderApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<VesselTrackingRequest> vesselData = parseVesselFinderResponse(response.getBody());

                updateDataSourceStatus(dataSource, SourceStatus.HEALTHY,
                        "Successfully fetched " + vesselData.size() + " vessels");

                log.info("Successfully fetched {} vessels from VesselFinder", vesselData.size());
                return CompletableFuture.completedFuture(vesselData);
            } else {
                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.debug("VesselFinder API endpoint not found (404). This is expected if mock service is not running.");
            updateDataSourceStatus(dataSource, SourceStatus.ERROR,
                    "API endpoint not found - possibly mock service not running");
            return CompletableFuture.completedFuture(List.of());
        } catch (ResourceAccessException e) {
            log.debug("Failed to connect to VesselFinder API: {}. Continuing without external data.", e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Connection failed: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        } catch (Exception e) {
            log.debug("Failed to fetch vessel data from VesselFinder: {}. Continuing without external data.",
                    e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Error: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    /**
     * Build VesselFinder API URL with geographic bounds
     */
    private String buildVesselFinderUrl() {
        // Mock API format for simulator - encode JSON properly
        try {
            String boundsJson = String.format("{\"minLat\":%.6f,\"maxLat\":%.6f,\"minLon\":%.6f,\"maxLon\":%.6f}",
                    minLatitude, maxLatitude, minLongitude, maxLongitude);
            return String.format("%s?bounds=%s", vesselFinderBaseUrl,
                    java.net.URLEncoder.encode(boundsJson, "UTF-8"));
        } catch (Exception e) {
            // Fallback to simple URL if encoding fails
            log.warn("Failed to encode bounds JSON for VesselFinder URL: {}", e.getMessage());
            return vesselFinderBaseUrl;
        }
    }

    /**
     * Parse VesselFinder API response
     */
    private List<VesselTrackingRequest> parseVesselFinderResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode vessels = root.get("vessels"); // VesselFinder response structure

            if (vessels == null || !vessels.isArray()) {
                // Try alternative structures
                vessels = root.get("data");
                if (vessels == null || !vessels.isArray()) {
                    vessels = root.get("results");
                    if (vessels == null || !vessels.isArray()) {
                        return List.of();
                    }
                }
            }

            java.util.List<VesselTrackingRequest> vesselList = new java.util.ArrayList<>();

            vessels.elements().forEachRemaining(vessel -> {
                VesselTrackingRequest vesselRequest = parseVesselFromVesselFinder(vessel);
                if (vesselRequest != null) {
                    vesselList.add(vesselRequest);
                }
            });

            return vesselList;
        } catch (Exception e) {
            log.error("Failed to parse VesselFinder response", e);
            return List.of();
        }
    }

    /**
     * Parse individual vessel data from VesselFinder format
     */
    private VesselTrackingRequest parseVesselFromVesselFinder(JsonNode data) {
        try {
            return VesselTrackingRequest.builder()
                    .mmsi(data.get("mmsi") != null ? data.get("mmsi").asText()
                            : data.get("MMSI") != null ? data.get("MMSI").asText() : null)
                    .latitude(data.get("latitude") != null ? data.get("latitude").asDouble()
                            : data.get("lat") != null ? data.get("lat").asDouble() : null)
                    .longitude(data.get("longitude") != null ? data.get("longitude").asDouble()
                            : data.get("lon") != null ? data.get("lon").asDouble() : null)
                    .speed(data.get("speed") != null ? data.get("speed").asDouble()
                            : data.get("sog") != null ? data.get("sog").asDouble() : null)
                    .course(data.get("course") != null ? data.get("course").asInt()
                            : data.get("cog") != null ? data.get("cog").asInt() : null)
                    .heading(data.get("heading") != null ? data.get("heading").asInt()
                            : data.get("hdg") != null ? data.get("hdg").asInt() : null)
                    .navigationStatus(data.get("navStatus") != null ? data.get("navStatus").asText()
                            : data.get("status") != null ? data.get("status").asText() : null)
                    .vesselName(data.get("vesselName") != null ? data.get("vesselName").asText()
                            : data.get("name") != null ? data.get("name").asText() : null)
                    .vesselType(data.get("vesselType") != null ? data.get("vesselType").asText()
                            : data.get("type") != null ? data.get("type").asText() : null)
                    .imo(data.get("imo") != null ? data.get("imo").asText()
                            : data.get("IMO") != null ? data.get("IMO").asText() : null)
                    .callsign(data.get("callsign") != null ? data.get("callsign").asText()
                            : data.get("call") != null ? data.get("call").asText() : null)
                    .flag(data.get("flag") != null ? data.get("flag").asText()
                            : data.get("country") != null ? data.get("country").asText() : null)
                    .length(data.get("length") != null ? data.get("length").asInt()
                            : data.get("loa") != null ? data.get("loa").asInt() : null)
                    .width(data.get("width") != null ? data.get("width").asInt()
                            : data.get("beam") != null ? data.get("beam").asInt() : null)
                    .draught(data.get("draught") != null ? data.get("draught").asDouble()
                            : data.get("draft") != null ? data.get("draft").asDouble() : null)
                    .destination(data.get("destination") != null ? data.get("destination").asText()
                            : data.get("dest") != null ? data.get("dest").asText() : null)
                    .eta(data.get("eta") != null ? data.get("eta").asText()
                            : data.get("ETA") != null ? data.get("ETA").asText() : null)
                    // VesselFinder specific fields
                    .cargoType(data.get("cargoType") != null ? data.get("cargoType").asText() : null)
                    .grossTonnage(data.get("grossTonnage") != null ? data.get("grossTonnage").asInt() : null)
                    .buildYear(data.get("buildYear") != null ? data.get("buildYear").asText() : null)
                    .lastPort(data.get("lastPort") != null ? data.get("lastPort").asText() : null)
                    .nextPort(data.get("nextPort") != null ? data.get("nextPort").asText() : null)
                    .route(data.get("route") != null ? data.get("route").asText() : null)
                    .timestamp(LocalDateTime.now())
                    .dataQuality(0.87) // VesselFinder generally has good quality data
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse vessel data from VesselFinder: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create HTTP headers for VesselFinder API
     */
    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TrackingSystem/1.0");
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        if (apiKey != null && !apiKey.equals("mock_key")) {
            // VesselFinder might use different auth methods
            headers.set("X-API-Key", apiKey);
            headers.set("Authorization", "Bearer " + apiKey);
            // Some APIs use this format
            headers.set("VF-API-Key", apiKey);
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
                            .priority(2) // Same priority level as main backup
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
                    .responseTime(1400L) // VesselFinder response time
                    .build();
            dataSourceStatusRepository.save(statusRecord);

        } catch (Exception e) {
            log.error("Failed to update VesselFinder data source status", e);
        }
    }

    /**
     * Check if VesselFinder API is available
     */
    public boolean isVesselFinderAvailable() {
        return vesselFinderEnabled && isDataSourceHealthy(DataSourceType.VESSEL_FINDER.getDisplayName());
    }

    private boolean isDataSourceHealthy(String name) {
        return dataSourceRepository.findByName(name)
                .map(DataSource::getIsActive)
                .orElse(false);
    }

    /**
     * Get VesselFinder API status
     */
    public java.util.Map<String, Object> getVesselFinderStatus() {
        return java.util.Map.of(
                "enabled", vesselFinderEnabled,
                "available", isVesselFinderAvailable(),
                "coverage", "Global vessel tracking with enhanced commercial vessel data",
                "specialization", "Focus on commercial and cargo vessels",
                "priority", 2);
    }
}