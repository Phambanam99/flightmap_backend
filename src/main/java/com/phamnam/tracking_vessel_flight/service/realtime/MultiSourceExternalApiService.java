package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiSourceExternalApiService {

    private final ExternalApiService externalApiService;
    private final DataFusionService dataFusionService;
    private final RealTimeDataProcessor dataProcessor;
    private final RawDataStorageService rawDataStorageService;

    // New API services
    private final ChinaportsApiService chinaportsApiService;
    private final MarineTrafficV2ApiService marineTrafficV2ApiService;
    private final AdsbExchangeApiService adsbExchangeApiService;
    private final VesselFinderApiService vesselFinderApiService;

    /**
     * Collect aircraft data from all available sources
     */
    @Async
    public CompletableFuture<List<AircraftTrackingRequest>> collectAllAircraftData() {
        Map<String, CompletableFuture<List<AircraftTrackingRequest>>> futures = new HashMap<>();

        // Existing FlightRadar24 API
        futures.put("flightradar24", externalApiService.fetchAircraftData());

        // New aircraft API sources
        futures.put("adsbexchange", adsbExchangeApiService.fetchAircraftData());

        // Wait for all futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.values().toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> {
            Map<String, List<AircraftTrackingRequest>> dataBySource = new HashMap<>();

            // Collect results from each source and store raw data
            futures.forEach((source, future) -> {
                try {
                    long startTime = System.currentTimeMillis();
                    List<AircraftTrackingRequest> data = future.join();
                    long responseTime = System.currentTimeMillis() - startTime;

                    if (data != null && !data.isEmpty()) {
                        dataBySource.put(source, data);
                        log.info("Collected {} aircraft from {}", data.size(), source);

                        // Store raw data for audit and analysis
                        String apiEndpoint = getAircraftApiEndpoint(source);
                        rawDataStorageService.storeRawAircraftData(source, data, apiEndpoint, responseTime);
                    }
                } catch (Exception e) {
                    log.error("Failed to get data from {}: {}", source, e.getMessage());
                }
            });

            // Merge data using fusion service
            if (dataBySource.isEmpty()) {
                return new ArrayList<>();
            }

            return dataFusionService.mergeAircraftData(dataBySource);
        });
    }

    /**
     * Collect vessel data from all available sources
     */
    @Async
    public CompletableFuture<List<VesselTrackingRequest>> collectAllVesselData() {
        Map<String, CompletableFuture<List<VesselTrackingRequest>>> futures = new HashMap<>();

        // Existing MarineTraffic API
        futures.put("marinetraffic", externalApiService.fetchVesselData());

        // New vessel API sources
        futures.put("chinaports", chinaportsApiService.fetchVesselData());
        futures.put("marinetrafficv2", marineTrafficV2ApiService.fetchVesselData());
        futures.put("vesselfinder", vesselFinderApiService.fetchVesselData());

        // Wait for all futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.values().toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> {
            Map<String, List<VesselTrackingRequest>> dataBySource = new HashMap<>();

            // Collect results from each source and store raw data
            futures.forEach((source, future) -> {
                try {
                    long startTime = System.currentTimeMillis();
                    List<VesselTrackingRequest> data = future.join();
                    long responseTime = System.currentTimeMillis() - startTime;

                    if (data != null && !data.isEmpty()) {
                        dataBySource.put(source, data);
                        log.info("Collected {} vessels from {}", data.size(), source);

                        // Store raw data for audit and analysis
                        String apiEndpoint = getVesselApiEndpoint(source);
                        rawDataStorageService.storeRawVesselData(source, data, apiEndpoint, responseTime);
                    }
                } catch (Exception e) {
                    log.error("Failed to get data from {}: {}", source, e.getMessage());
                }
            });

            // Merge data using fusion service
            if (dataBySource.isEmpty()) {
                return new ArrayList<>();
            }

            return dataFusionService.mergeVesselData(dataBySource);
        });
    }

    /**
     * Scheduled task to collect and process data from all sources
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Async
    public void collectAndProcessMultiSourceData() {
        log.debug("Starting multi-source data collection...");

        try {
            // Collect from all sources in parallel
            CompletableFuture<List<AircraftTrackingRequest>> aircraftFuture = collectAllAircraftData();
            CompletableFuture<List<VesselTrackingRequest>> vesselFuture = collectAllVesselData();

            // Wait for both to complete
            CompletableFuture.allOf(aircraftFuture, vesselFuture).join();

            // Process the merged results
            List<AircraftTrackingRequest> aircraftData = aircraftFuture.get();
            List<VesselTrackingRequest> vesselData = vesselFuture.get();

            // Send to real-time processor
            if (!aircraftData.isEmpty()) {
                dataProcessor.processAircraftData(aircraftData);
                log.info("Processed {} merged aircraft records from multiple sources", aircraftData.size());
            }

            if (!vesselData.isEmpty()) {
                dataProcessor.processVesselData(vesselData);
                log.info("Processed {} merged vessel records from multiple sources", vesselData.size());
            }

        } catch (Exception e) {
            log.error("Error during multi-source data collection", e);
        }
    }

    /**
     * Get status of all data sources
     */
    public Map<String, Object> getAllSourcesStatus() {
        Map<String, Object> status = new HashMap<>();

        // Get existing API status
        status.put("currentSources", externalApiService.getApiStatus());

        // All new API sources status
        status.put("newSources", Map.of(
                "adsbexchange", adsbExchangeApiService.getAdsbExchangeStatus(),
                "vesselfinder", vesselFinderApiService.getVesselFinderStatus(),
                "chinaports", chinaportsApiService.getChinaportsStatus(),
                "marinetrafficv2", marineTrafficV2ApiService.getMarineTrafficV2Status()));

        // Fusion service status
        status.put("dataFusion", Map.of(
                "enabled", true,
                "deduplicationEnabled", true,
                "activeSources", 6, // FlightRadar24, AdsB, MarineTraffic, VesselFinder, Chinaports, MarineTrafficV2
                "aircraftSources", 2, // FlightRadar24, AdsB Exchange
                "vesselSources", 4 // MarineTraffic, VesselFinder, Chinaports, MarineTrafficV2
        ));

        return status;
    }

    /**
     * Get API endpoint for aircraft data source
     */
    private String getAircraftApiEndpoint(String source) {
        return switch (source) {
            case "flightradar24" -> "/api/aircraft/flightradar24";
            case "adsbexchange" -> "/api/aircraft/adsbexchange";
            default -> "/api/aircraft/" + source;
        };
    }

    /**
     * Get API endpoint for vessel data source
     */
    private String getVesselApiEndpoint(String source) {
        return switch (source) {
            case "marinetraffic" -> "/api/vessels/marinetraffic";
            case "vesselfinder" -> "/api/vessels/vesselfinder";
            case "chinaports" -> "/api/vessels/chinaports";
            case "marinetrafficv2" -> "/api/vessels/marinetrafficv2";
            default -> "/api/vessels/" + source;
        };
    }
}