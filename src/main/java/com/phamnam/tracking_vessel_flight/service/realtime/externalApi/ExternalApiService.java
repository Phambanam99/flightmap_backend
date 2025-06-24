package com.phamnam.tracking_vessel_flight.service.realtime.externalApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceStatusRepository dataSourceStatusRepository;

    // FlightRadar24 Configuration
    @Value("${external.api.flightradar24.enabled:true}")
    private boolean flightradar24Enabled;

    @Value("${external.api.flightradar24.base-url}")
    private String flightradar24BaseUrl;

    @Value("${external.api.flightradar24.api-key}")
    private String flightradar24ApiKey;

    @Value("${external.api.flightradar24.timeout:10000}")
    private int flightradar24Timeout;

    // MarineTraffic Configuration
    @Value("${external.api.marinetraffic.enabled:true}")
    private boolean marineTrafficEnabled;

    @Value("${external.api.marinetraffic.base-url}")
    private String marineTrafficBaseUrl;

    @Value("${external.api.marinetraffic.api-key}")
    private String marineTrafficApiKey;

    @Value("${external.api.marinetraffic.timeout:15000}")
    private int marineTrafficTimeout;

    // Geographic bounds
    @Value("${external.api.bounds.min-latitude:8.5}")
    private double minLatitude;

    @Value("${external.api.bounds.max-latitude:23.5}")
    private double maxLatitude;

    @Value("${external.api.bounds.min-longitude:102.0}")
    private double minLongitude;

    @Value("${external.api.bounds.max-longitude:109.5}")
    private double maxLongitude;

    // ============================================================================
    // AIRCRAFT DATA RETRIEVAL
    // ============================================================================

    @Async
    public CompletableFuture<List<AircraftTrackingRequest>> fetchAircraftData() {
        if (!flightradar24Enabled) {
            log.debug("FlightRadar24 API is disabled");
            return CompletableFuture.completedFuture(List.of());
        }

        // Smart endpoint checking
        if (!isEndpointAvailable(flightradar24BaseUrl, "")) {
            log.debug("FlightRadar24 endpoint not available, skipping data fetch");
            return CompletableFuture.completedFuture(List.of());
        }

        DataSource dataSource = getOrCreateDataSource(DataSourceType.FLIGHT_RADAR.getDisplayName(),
                DataSourceType.FLIGHT_RADAR);

        try {
            log.debug("Fetching aircraft data from FlightRadar24...");

            String url = buildFlightRadar24Url();
            HttpHeaders headers = createHeaders(flightradar24ApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
//                System.out.println(response.getBody());
                List<AircraftTrackingRequest> aircraftData = parseFlightRadar24Response(response.getBody());

                updateDataSourceStatus(dataSource, SourceStatus.HEALTHY,
                        "Successfully fetched " + aircraftData.size() + " aircraft");

                log.info("Successfully fetched {} aircraft from FlightRadar24", aircraftData.size());
                return CompletableFuture.completedFuture(aircraftData);
            } else {
                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.debug(
                    "FlightRadar24 API endpoint not found (404). This is expected if mock service is not running. Continuing without external data.");
            updateDataSourceStatus(dataSource, SourceStatus.ERROR,
                    "API endpoint not found - possibly mock service not running");
            return CompletableFuture.completedFuture(List.of());
        } catch (ResourceAccessException e) {
            log.debug("Failed to connect to FlightRadar24 API: {}. Continuing without external data.", e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Connection failed: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        } catch (Exception e) {
            log.debug("Failed to fetch aircraft data from FlightRadar24: {}. Continuing without external data.",
                    e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Error: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private String buildFlightRadar24Url() {
        // Mock API format - base URL already includes the full path
        try {
            String boundsJson = String.format("{\"minLat\":%.6f,\"maxLat\":%.6f,\"minLon\":%.6f,\"maxLon\":%.6f}",
                    minLatitude, maxLatitude, minLongitude, maxLongitude);
            return String.format("%s?bounds=%s", flightradar24BaseUrl,
                    java.net.URLEncoder.encode(boundsJson, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Fallback to simple URL if encoding fails
            return flightradar24BaseUrl;
        }
    }

    private List<AircraftTrackingRequest> parseFlightRadar24Response(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Handle mock API response format: {"data": {"full_count": 50000, "version": 4,
            // "hexident": [flight_data]}}
            JsonNode dataWrapper = root.path("data");
            if (dataWrapper.isMissingNode()) {
                log.debug("No data wrapper found in FlightRadar24 response");
                return List.of();
            }

            java.util.List<AircraftTrackingRequest> aircraftList = new java.util.ArrayList<>();

            dataWrapper.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (!key.equals("full_count") && !key.equals("version") && !key.equals("stats")) {
                    AircraftTrackingRequest aircraft = parseAircraftFromFlightRadar24(key, entry.getValue());
                    if (aircraft != null) {
                        aircraftList.add(aircraft);
                    }
                }
            });

            log.debug("Parsed {} aircraft from FlightRadar24 response", aircraftList.size());
            return aircraftList;
        } catch (Exception e) {
            log.error("Failed to parse FlightRadar24 response", e);
            return List.of();
        }
    }

    private AircraftTrackingRequest parseAircraftFromFlightRadar24(String key, JsonNode data) {
        try {
            // FlightRadar24 array format: [hexident, lat, lon, heading, alt, speed, squawk,
            // ...]
            String squawk = getArrayElementTextSafely(data, 6);

            return AircraftTrackingRequest.builder()
                    .hexident(key)
                    .latitude(getArrayElementDoubleSafely(data, 1))
                    .longitude(getArrayElementDoubleSafely(data, 2))
                    .track(getArrayElementIntegerSafely(data, 3))
                    .altitude(getArrayElementIntegerSafely(data, 4))
                    .groundSpeed(getArrayElementIntegerSafely(data, 5))
                    .squawk(squawk)
                    .aircraftType(getArrayElementTextSafely(data, 8))
                    .registration(getArrayElementTextSafely(data, 9))
                    .timestamp(LocalDateTime.now())
                    .onGround(getArrayElementBooleanSafely(data, 14))
                    .verticalRate(getArrayElementIntegerSafely(data, 15))
                    .callsign(getArrayElementTextSafely(data, 16))
                    .emergency("7500".equals(squawk) || "7600".equals(squawk) || "7700".equals(squawk))
                    .dataQuality(0.8) // FlightRadar24 usually has good data quality
                    .build();
        } catch (Exception e) {
            log.error("Error parsing aircraft data for {}: {}", key, e.getMessage());
            return null;
        }
    }

    // ============================================================================
    // VESSEL DATA RETRIEVAL
    // ============================================================================

    @Async
    public CompletableFuture<List<VesselTrackingRequest>> fetchVesselData() {
        if (!marineTrafficEnabled) {
            log.debug("MarineTraffic API is disabled");
            return CompletableFuture.completedFuture(List.of());
        }

        // Smart endpoint checking
        if (!isEndpointAvailable(marineTrafficBaseUrl, "")) {
            log.debug("MarineTraffic endpoint not available, skipping data fetch");
            return CompletableFuture.completedFuture(List.of());
        }

        DataSource dataSource = getOrCreateDataSource(DataSourceType.MARINE_TRAFFIC.getDisplayName(),
                DataSourceType.MARINE_TRAFFIC);

        try {
            log.debug("Fetching vessel data from MarineTraffic...");

            String url = buildMarineTrafficUrl();
            HttpHeaders headers = createHeaders(marineTrafficApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<VesselTrackingRequest> vesselData = parseMarineTrafficResponse(response.getBody());

                updateDataSourceStatus(dataSource, SourceStatus.HEALTHY,
                        "Successfully fetched " + vesselData.size() + " vessels");

                log.info("Successfully fetched {} vessels from MarineTraffic", vesselData.size());
                return CompletableFuture.completedFuture(vesselData);
            } else {
                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.debug(
                    "MarineTraffic API endpoint not found (404). This is expected if mock service is not running. Continuing without external data.");
            updateDataSourceStatus(dataSource, SourceStatus.ERROR,
                    "API endpoint not found - possibly mock service not running");
            return CompletableFuture.completedFuture(List.of());
        } catch (ResourceAccessException e) {
            log.debug("Failed to connect to MarineTraffic API: {}. Continuing without external data.", e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Connection failed: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        } catch (Exception e) {
            log.debug("Failed to fetch vessel data from MarineTraffic: {}. Continuing without external data.",
                    e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Error: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private String buildMarineTrafficUrl() {
        // Mock API format - base URL already includes the full path
        try {
            String boundsJson = String.format("{\"minLat\":%.6f,\"maxLat\":%.6f,\"minLon\":%.6f,\"maxLon\":%.6f}",
                    minLatitude, maxLatitude, minLongitude, maxLongitude);
            return String.format("%s?bounds=%s", marineTrafficBaseUrl,
                    java.net.URLEncoder.encode(boundsJson, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Fallback to simple URL if encoding fails
            return marineTrafficBaseUrl;
        }
    }

    private List<VesselTrackingRequest> parseMarineTrafficResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Handle mock API response format: {"data": {"data": [vessels], "meta": {...}}}
            JsonNode dataWrapper = root.path("data");
            JsonNode vessels = dataWrapper.path("data");

            if (vessels == null || vessels.isMissingNode() || !vessels.isArray()) {
                log.debug("No vessels data found in response or invalid format");
                return List.of();
            }

            java.util.List<VesselTrackingRequest> vesselList = new java.util.ArrayList<>();

            vessels.elements().forEachRemaining(vessel -> {
                VesselTrackingRequest vesselRequest = parseVesselFromMarineTraffic(vessel);
                vesselRequest.setSource(DataSourceType.MARINE_TRAFFIC.getDisplayName());
                if (vesselRequest != null) {
                    vesselList.add(vesselRequest);
                }
            });

            log.debug("Parsed {} vessels from MarineTraffic response", vesselList.size());
            return vesselList;
        } catch (Exception e) {
            log.error("Failed to parse MarineTraffic response", e);
            return List.of();
        }
    }

    private VesselTrackingRequest parseVesselFromMarineTraffic(JsonNode data) {
        System.out.println(data);
        try {
            return VesselTrackingRequest.builder()
                    .mmsi(getTextSafely(data, "MMSI"))
                    .latitude(getDoubleSafely(data, "LAT"))
                    .longitude(getDoubleSafely(data, "LON"))
                    .speed(getDoubleSafely(data, "SPEED"))
                    .course(getIntegerSafely(data, "COURSE"))
                    .heading(getIntegerSafely(data, "HEADING"))
                    .navigationStatus(getTextSafely(data, "STATUS")) // Use STATUS instead of NAVSTAT
                    .vesselName(getTextSafely(data, "SHIPNAME"))
                    .vesselType(getTextSafely(data, "SHIPTYPE"))
                    .imo(getTextSafely(data, "IMO"))
                    .callsign(getTextSafely(data, "CALLSIGN"))
                    .flag(getTextSafely(data, "FLAG"))
                    .length(getIntegerSafely(data, "LENGTH"))
                    .width(getIntegerSafely(data, "WIDTH"))
                    .draught(getDoubleSafely(data, "DRAUGHT"))
                    .destination(getTextSafely(data, "DESTINATION"))
                    .eta(getTextSafely(data, "ETA"))
                    .timestamp(LocalDateTime.now())
                    .dataQuality(0.90) // MarineTraffic generally has good quality data
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse vessel data: {}", e.getMessage());
            return null;
        }
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    private HttpHeaders createHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "TrackingSystem/1.0");
        headers.set("Accept", "application/json");
        if (apiKey != null && !apiKey.equals("mock_key")) {
            headers.set("X-API-Key", apiKey);
        }
        return headers;
    }

    private DataSource getOrCreateDataSource(String name, DataSourceType type) {
        return dataSourceRepository.findByName(name)
                .orElseGet(() -> {
                    DataSource dataSource = DataSource.builder()
                            .name(name)
                            .sourceType(type)
                            .isEnabled(true)
                            .isActive(true)
                            .priority(1)
                            .consecutiveFailures(0)
                            // .successRate(100.0)
                            .build();
                    return dataSourceRepository.save(dataSource);
                });
    }

    private void updateDataSourceStatus(DataSource dataSource, SourceStatus status, String message) {
        try {
            // Update data source
            if (status == SourceStatus.HEALTHY) {
                dataSource.setLastSuccessTime(LocalDateTime.now());
                dataSource.setConsecutiveFailures(0);
                dataSource.setIsActive(true);
            } else {
                dataSource.setConsecutiveFailures(dataSource.getConsecutiveFailures() + 1);
                if (dataSource.getConsecutiveFailures() >= 3) { // Hardcode for now
                    dataSource.setIsActive(false);
                }
            }
            dataSourceRepository.save(dataSource);

            // Create status record
            DataSourceStatus statusRecord = DataSourceStatus.builder()
                    .dataSource(dataSource)
                    .checkTime(LocalDateTime.now())
                    .responseTime(1000L) // TODO: Calculate actual response time
                    // .dataCount(0)
                    // .errorCount(status == SourceStatus.ERROR ? 1 : 0)
                    .build();
            dataSourceStatusRepository.save(statusRecord);

        } catch (Exception e) {
            log.error("Failed to update data source status", e);
        }
    }

    // ============================================================================
    // SCHEDULED HEALTH CHECKS
    // ============================================================================

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void performHealthChecks() {
        log.debug("Performing external API health checks...");

        List<DataSource> dataSources = dataSourceRepository.findByIsEnabledTrue();

        for (DataSource dataSource : dataSources) {
            performHealthCheck(dataSource);
        }
    }

    private void performHealthCheck(DataSource dataSource) {
        try {
            String healthCheckUrl = getHealthCheckUrl(dataSource);
            if (healthCheckUrl == null) {
                return;
            }

            long startTime = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.getForEntity(healthCheckUrl, String.class);
            long responseTime = System.currentTimeMillis() - startTime;

            SourceStatus status = response.getStatusCode() == HttpStatus.OK ? SourceStatus.HEALTHY : SourceStatus.ERROR;

            updateDataSourceHealthStatus(dataSource, status, responseTime,
                    "Health check: " + response.getStatusCode());

        } catch (Exception e) {
            updateDataSourceHealthStatus(dataSource, SourceStatus.ERROR, 5000L,
                    "Health check failed: " + e.getMessage());
        }
    }

    private String getHealthCheckUrl(DataSource dataSource) {
        if (dataSource.getSourceType() == null) {
            return null;
        }

        String sourceTypeName = dataSource.getSourceType().name();
        switch (sourceTypeName) {
            case "FLIGHT_RADAR":
                return flightradar24BaseUrl;
            case "MARINE_TRAFFIC":
                return marineTrafficBaseUrl;
            default:
                return null;
        }
    }

    private void updateDataSourceHealthStatus(DataSource dataSource, SourceStatus status,
            long responseTime, String message) {
        DataSourceStatus statusRecord = DataSourceStatus.builder()
                .dataSource(dataSource)
                // .status(status)
                // .message(message)
                .checkTime(LocalDateTime.now())
                .responseTime(responseTime)
                // .dataCount(0)
                // .errorCount(status == SourceStatus.ERROR ? 1 : 0)
                .build();

        dataSourceStatusRepository.save(statusRecord);
    }

    // ============================================================================
    // PUBLIC API METHODS
    // ============================================================================

    public boolean isFlightRadar24Available() {
        return flightradar24Enabled && isDataSourceHealthy("FlightRadar24");
    }

    public boolean isMarineTrafficAvailable() {
        return marineTrafficEnabled && isDataSourceHealthy("MarineTraffic");
    }

    private boolean isDataSourceHealthy(String name) {
        return dataSourceRepository.findByName(name)
                .map(DataSource::getIsActive)
                .orElse(false);
    }

    public Map<String, Object> getApiStatus() {
        return Map.of(
                "flightRadar24", Map.of(
                        "enabled", flightradar24Enabled,
                        "available", isFlightRadar24Available()),
                "marineTraffic", Map.of(
                        "enabled", marineTrafficEnabled,
                        "available", isMarineTrafficAvailable()));
    }

    // ============================================================================
    // SMART ENDPOINT CHECKING
    // ============================================================================

    private boolean isEndpointAvailable(String baseUrl, String path) {
        try {
            String healthCheckUrl = baseUrl + path;
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TrackingSystem/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    healthCheckUrl, HttpMethod.GET, entity, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Endpoint {} not available: {}", baseUrl + path, e.getMessage());
            return false;
        }
    }

    // ============================================================================
    // NULL-SAFE JSON PARSING UTILITIES
    // ============================================================================

    // For object-based JSON (vessels)
    private String getTextSafely(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    private Double getDoubleSafely(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asDouble() : null;
    }

    private Integer getIntegerSafely(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asInt() : null;
    }

    // For array-based JSON (aircraft)
    private String getArrayElementTextSafely(JsonNode array, int index) {
        if (array.isArray() && array.size() > index) {
            JsonNode element = array.get(index);
            return (element != null && !element.isNull()) ? element.asText() : null;
        }
        return null;
    }

    private Double getArrayElementDoubleSafely(JsonNode array, int index) {
        if (array.isArray() && array.size() > index) {
            JsonNode element = array.get(index);
            return (element != null && !element.isNull()) ? element.asDouble() : null;
        }
        return null;
    }

    private Integer getArrayElementIntegerSafely(JsonNode array, int index) {
        if (array.isArray() && array.size() > index) {
            JsonNode element = array.get(index);
            return (element != null && !element.isNull()) ? element.asInt() : null;
        }
        return null;
    }

    private Boolean getArrayElementBooleanSafely(JsonNode array, int index) {
        if (array.isArray() && array.size() > index) {
            JsonNode element = array.get(index);
            return element != null && !element.isNull() && element.asBoolean();
        }
        return false;
    }
}