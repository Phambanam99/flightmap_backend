package com.phamnam.tracking_vessel_flight.service.realtime;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

        DataSource dataSource = getOrCreateDataSource("FlightRadar24", DataSourceType.FLIGHT_RADAR);

        try {
            log.debug("Fetching aircraft data from FlightRadar24...");

            String url = buildFlightRadar24Url();
            HttpHeaders headers = createHeaders(flightradar24ApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<AircraftTrackingRequest> aircraftData = parseFlightRadar24Response(response.getBody());

                updateDataSourceStatus(dataSource, SourceStatus.HEALTHY,
                        "Successfully fetched " + aircraftData.size() + " aircraft");

                log.info("Successfully fetched {} aircraft from FlightRadar24", aircraftData.size());
                return CompletableFuture.completedFuture(aircraftData);
            } else {
                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.warn(
                    "FlightRadar24 API endpoint not found (404). This is expected if mock service is not running. Continuing without external data.");
            updateDataSourceStatus(dataSource, SourceStatus.ERROR,
                    "API endpoint not found - possibly mock service not running");
            return CompletableFuture.completedFuture(List.of());
        } catch (ResourceAccessException e) {
            log.warn("Failed to connect to FlightRadar24 API: {}. Continuing without external data.", e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Connection failed: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        } catch (Exception e) {
            log.warn("Failed to fetch aircraft data from FlightRadar24: {}. Continuing without external data.",
                    e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Error: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private String buildFlightRadar24Url() {
        return String.format(
                "%s/zones/fcgi/feed.js?bounds=%f,%f,%f,%f&faa=1&satellite=1&mlat=1&flarm=1&adsb=1&gnd=1&air=1&vehicles=1&estimated=1&maxage=14400&gliders=1&stats=1",
                flightradar24BaseUrl, maxLatitude, minLatitude, minLongitude, maxLongitude);
    }

    private List<AircraftTrackingRequest> parseFlightRadar24Response(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            java.util.List<AircraftTrackingRequest> aircraftList = new java.util.ArrayList<>();

            root.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (!key.equals("full_count") && !key.equals("version") && !key.equals("stats")) {
                    AircraftTrackingRequest aircraft = parseAircraftFromFlightRadar24(key, entry.getValue());
                    if (aircraft != null) {
                        aircraftList.add(aircraft);
                    }
                }
            });

            return aircraftList;
        } catch (Exception e) {
            log.error("Failed to parse FlightRadar24 response", e);
            return List.of();
        }
    }

    private AircraftTrackingRequest parseAircraftFromFlightRadar24(String key, JsonNode data) {
        try {
            return AircraftTrackingRequest.builder()
                    .hexident(key)
                    .latitude(data.get(1) != null ? data.get(1).asDouble() : null)
                    .longitude(data.get(2) != null ? data.get(2).asDouble() : null)
                    .track(data.get(3) != null ? data.get(3).asInt() : null)
                    .altitude(data.get(4) != null ? data.get(4).asInt() : null)
                    .groundSpeed(data.get(5) != null ? data.get(5).asInt() : null)
                    .squawk(data.get(6) != null ? data.get(6).asText() : null)
                    .aircraftType(data.get(8) != null ? data.get(8).asText() : null)
                    .registration(data.get(9) != null ? data.get(9).asText() : null)
                    .timestamp(LocalDateTime.now())
                    .onGround(data.get(14) != null ? data.get(14).asBoolean() : false)
                    .verticalRate(data.get(15) != null ? data.get(15).asInt() : null)
                    .callsign(data.get(16) != null ? data.get(16).asText() : null)
                    .emergency("7500".equals(data.get(6) != null ? data.get(6).asText() : null) ||
                            "7600".equals(data.get(6) != null ? data.get(6).asText() : null) ||
                            "7700".equals(data.get(6) != null ? data.get(6).asText() : null))
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

        DataSource dataSource = getOrCreateDataSource("MarineTraffic", DataSourceType.MARINE_TRAFFIC);

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
            log.warn(
                    "MarineTraffic API endpoint not found (404). This is expected if mock service is not running. Continuing without external data.");
            updateDataSourceStatus(dataSource, SourceStatus.ERROR,
                    "API endpoint not found - possibly mock service not running");
            return CompletableFuture.completedFuture(List.of());
        } catch (ResourceAccessException e) {
            log.warn("Failed to connect to MarineTraffic API: {}. Continuing without external data.", e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Connection failed: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        } catch (Exception e) {
            log.warn("Failed to fetch vessel data from MarineTraffic: {}. Continuing without external data.",
                    e.getMessage());
            updateDataSourceStatus(dataSource, SourceStatus.ERROR, "Error: " + e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private String buildMarineTrafficUrl() {
        return String.format(
                "%s/exportvessels/v:8/X-API-Key:%s/timespan:60/protocol:json/minlat:%f/maxlat:%f/minlon:%f/maxlon:%f",
                marineTrafficBaseUrl, marineTrafficApiKey, minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    private List<VesselTrackingRequest> parseMarineTrafficResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode vessels = root.get("data");

            if (vessels == null || !vessels.isArray()) {
                return List.of();
            }

            java.util.List<VesselTrackingRequest> vesselList = new java.util.ArrayList<>();

            vessels.elements().forEachRemaining(vessel -> {
                VesselTrackingRequest vesselRequest = parseVesselFromMarineTraffic(vessel);
                if (vesselRequest != null) {
                    vesselList.add(vesselRequest);
                }
            });

            return vesselList;
        } catch (Exception e) {
            log.error("Failed to parse MarineTraffic response", e);
            return List.of();
        }
    }

    private VesselTrackingRequest parseVesselFromMarineTraffic(JsonNode data) {
        try {
            return VesselTrackingRequest.builder()
                    .mmsi(data.get("MMSI").asText())
                    .latitude(data.get("LAT").asDouble())
                    .longitude(data.get("LON").asDouble())
                    .speed(data.get("SPEED").asDouble())
                    .course(data.get("COURSE").asInt())
                    .heading(data.get("HEADING").asInt())
                    .navigationStatus(data.get("NAVSTAT").asText())
                    .vesselName(data.get("SHIPNAME").asText())
                    .vesselType(data.get("SHIPTYPE").asText())
                    .imo(data.get("IMO").asText())
                    .callsign(data.get("CALLSIGN").asText())
                    .flag(data.get("FLAG").asText())
                    .length(data.get("LENGTH").asInt())
                    .width(data.get("WIDTH").asInt())
                    .draught(data.get("DRAUGHT").asDouble())
                    .destination(data.get("DESTINATION").asText())
                    .eta(data.get("ETA").asText())
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
                return flightradar24BaseUrl + "/zones/fcgi/feed.js?bounds=1,0,0,1";
            case "MARINE_TRAFFIC":
                return marineTrafficBaseUrl + "/exportvessels/v:8/X-API-Key:" + marineTrafficApiKey + "/timespan:1";
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
}