package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.service.realtime.externalApi.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    // Poll interval configurations
    @Value("${external.api.data-collection.poll-interval:30000}")
    private long dataCollectionPollInterval;

    @Value("${external.api.flightradar24.poll-interval:30000}")
    private long flightradar24PollInterval;

    @Value("${external.api.adsbexchange.poll-interval:30000}")
    private long adsbexchangePollInterval;

    @Value("${external.api.marinetraffic.poll-interval:60000}")
    private long marinetrafficPollInterval;

    @Value("${external.api.vesselfinder.poll-interval:60000}")
    private long vesselfinderPollInterval;

    @Value("${external.api.chinaports.poll-interval:60000}")
    private long chinaportsPollInterval;

    @Value("${external.api.marinetrafficv2.poll-interval:45000}")
    private long marinetrafficv2PollInterval;

    /**
     * Collect aircraft data from all available sources
     */
    @Async("taskExecutor")
    public CompletableFuture<List<AircraftTrackingRequest>> collectAllAircraftData() {
        Map<String, CompletableFuture<List<AircraftTrackingRequest>>> futures = new HashMap<>();

        // Đặt timeout để tránh treo lâu
        futures.put("flightradar24", externalApiService.fetchAircraftData().orTimeout(10, TimeUnit.SECONDS));
        futures.put("adsbexchange", adsbExchangeApiService.fetchAircraftData().orTimeout(10, TimeUnit.SECONDS));
        // khi can them nguan
        // futures.put("anotherApi", anotherApiService.fetchAircraftData().orTimeout(10,
        // TimeUnit.SECONDS));
        // Chờ tất cả hoàn thành
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.values().toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> {
            Map<String, List<AircraftTrackingRequest>> dataBySource = new HashMap<>();

            futures.forEach((source, future) -> {
                List<AircraftTrackingRequest> data = safeGetAndStore(source, future);
                if (!data.isEmpty()) {
                    dataBySource.put(source, data);
                }
            });

            if (dataBySource.isEmpty()) {
                return Collections.emptyList();
            }

            return dataFusionService.mergeAircraftData(dataBySource);
        });
    }

    private List<AircraftTrackingRequest> safeGetAndStore(String source,
            CompletableFuture<List<AircraftTrackingRequest>> future) {
        try {
            log.info("🔄 Processing aircraft data from source: {}", source);
            long startTime = System.currentTimeMillis();
            List<AircraftTrackingRequest> data = future.join();
            long duration = System.currentTimeMillis() - startTime;

            log.info("📊 Source {} returned {} aircraft records in {}ms", source,
                    data != null ? data.size() : 0, duration);

            if (data != null && !data.isEmpty()) {
                log.info("✅ Collected {} aircraft from {}", data.size(), source);

                String apiEndpoint = getAircraftApiEndpoint(source);
                rawDataStorageService.storeRawAircraftData(source, data, apiEndpoint, duration);

                return data;
            } else {
                log.warn("⚠️ Source {} returned empty or null data", source);
            }
        } catch (Exception e) {
            Throwable root = (e.getCause() != null) ? e.getCause() : e;
            log.error("❌ Failed to get data from {}: {} - {}", source, root.getClass().getSimpleName(),
                    root.getMessage(), root);
        }

        return Collections.emptyList();
    }

    /**
     * Collect vessel data from all available sources
     */
    @Async("taskExecutor")
    public CompletableFuture<List<VesselTrackingRequest>> collectAllVesselData() {
        Map<String, CompletableFuture<List<VesselTrackingRequest>>> futures = new HashMap<>();

        // Các API nguồn dữ liệu tàu thuyền, có timeout để tránh treo
        futures.put("marinetraffic", externalApiService.fetchVesselData().orTimeout(10, TimeUnit.SECONDS));
        futures.put("chinaports", chinaportsApiService.fetchVesselData().orTimeout(10, TimeUnit.SECONDS));
        futures.put("marinetrafficv2", marineTrafficV2ApiService.fetchVesselData().orTimeout(10, TimeUnit.SECONDS));
        futures.put("vesselfinder", vesselFinderApiService.fetchVesselData().orTimeout(10, TimeUnit.SECONDS));

        // Chờ tất cả hoàn thành
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.values().toArray(new CompletableFuture[0]));

        return allFutures.thenApply(v -> {
            Map<String, List<VesselTrackingRequest>> dataBySource = new HashMap<>();

            futures.forEach((source, future) -> {
                List<VesselTrackingRequest> data = safeGetAndStoreVessel(source, future);
                if (!data.isEmpty()) {
                    dataBySource.put(source, data);
                    log.info("Collected {} vessels from {}", data.size(), source);
                }
            });

            if (dataBySource.isEmpty()) {
                return Collections.emptyList();
            }

            return dataFusionService.mergeVesselData(dataBySource);
        });
    }

    private List<VesselTrackingRequest> safeGetAndStoreVessel(String source,
            CompletableFuture<List<VesselTrackingRequest>> future) {
        try {
            long startTime = System.currentTimeMillis();
            List<VesselTrackingRequest> data = future.join();
            long duration = System.currentTimeMillis() - startTime;

            if (data != null && !data.isEmpty()) {
                log.info("Collected {} vessels from {}", data.size(), source);

                String apiEndpoint = getVesselApiEndpoint(source);
                rawDataStorageService.storeRawVesselData(source, data, apiEndpoint, duration);

                return data;
            }
        } catch (Exception e) {
            Throwable root = (e.getCause() != null) ? e.getCause() : e;
            log.error("Failed to get vessel data from {}: {}", source, root.getMessage(), root);
        }

        return Collections.emptyList();
    }

    /**
     * Scheduled task to collect and process data from all sources
     * Use fixedDelay to ensure previous execution completes before starting next
     * one
     */
    @Scheduled(fixedDelayString = "${external.api.data-collection.poll-interval:30000}", initialDelay = 5000)
    @Async("scheduledTaskExecutor")
    public void collectAndProcessMultiSourceData() {
        String threadName = Thread.currentThread().getName();
        log.info("🚀 Starting multi-source data collection on thread: {}", threadName);

        try {
            // Collect from all sources in parallel
            CompletableFuture<List<AircraftTrackingRequest>> aircraftFuture = collectAllAircraftData();
            CompletableFuture<List<VesselTrackingRequest>> vesselFuture = collectAllVesselData();

            log.debug("⏳ Waiting for both aircraft and vessel futures to complete...");

            // Wait for both to complete
            CompletableFuture.allOf(aircraftFuture, vesselFuture).join();

            log.debug("✅ Both futures completed, getting results...");

            // Process the merged results
            List<AircraftTrackingRequest> aircraftData;
            List<VesselTrackingRequest> vesselData;

            try {
                aircraftData = aircraftFuture.get();
                log.info("📄 Retrieved {} aircraft data after merge", aircraftData != null ? aircraftData.size() : 0);
            } catch (Exception e) {
                log.error("❌ Failed to get aircraft data: {}", e.getMessage(), e);
                aircraftData = Collections.emptyList();
            }

            try {
                vesselData = vesselFuture.get();
                log.info("🚢 Retrieved {} vessel data after merge", vesselData != null ? vesselData.size() : 0);
            } catch (Exception e) {
                log.error("❌ Failed to get vessel data: {}", e.getMessage(), e);
                vesselData = Collections.emptyList();
            }

            // Send to real-time processor
            if (!aircraftData.isEmpty()) {
                try {
                    log.info("🔄 Sending {} aircraft records to processor...", aircraftData.size());

                    // CRITICAL FIX: Await the async processing with timeout!
                    CompletableFuture<Void> aircraftProcessingFuture = dataProcessor.processAircraftData(aircraftData);
                    aircraftProcessingFuture.get(60, TimeUnit.SECONDS); // Wait max 60 seconds

                    log.info("✅ Processed {} merged aircraft records from multiple sources", aircraftData.size());
                } catch (TimeoutException e) {
                    log.error("⏱️ Aircraft processing timed out after 60 seconds for {} records", aircraftData.size());
                } catch (Exception e) {
                    log.error("❌ Failed to process aircraft data: {}", e.getMessage(), e);
                }
            } else {
                log.warn("⚠️ No aircraft data to process (empty list)");
            }

            // Ultra-detailed vessel processing debugging
            log.info("🔍 VESSEL DEBUG: vesselData is null? {}", vesselData == null);
            log.info("🔍 VESSEL DEBUG: vesselData size: {}", vesselData != null ? vesselData.size() : "NULL");
            log.info("🔍 VESSEL DEBUG: vesselData.isEmpty(): {}", vesselData != null ? vesselData.isEmpty() : "NULL");

            if (vesselData != null && !vesselData.isEmpty()) {
                try {
                    log.info("🔄 Sending {} vessel records to processor...", vesselData.size());
                    log.info("🔍 About to call dataProcessor.processVesselData()...");

                    // CRITICAL FIX: Await the async processing with timeout!
                    CompletableFuture<Void> vesselProcessingFuture = dataProcessor.processVesselData(vesselData);
                    vesselProcessingFuture.get(90, TimeUnit.SECONDS); // Wait max 90 seconds for large datasets

                    log.info("✅ Processed {} merged vessel records from multiple sources", vesselData.size());
                } catch (TimeoutException e) {
                    log.error("⏱️ Vessel processing timed out after 90 seconds for {} records", vesselData.size());
                } catch (Exception e) {
                    log.error("❌ Failed to process vessel data: {}", e.getMessage(), e);
                    e.printStackTrace();
                }
            } else {
                log.warn("⚠️ No vessel data to process (vesselData={}, isEmpty={})",
                        vesselData, vesselData != null ? vesselData.isEmpty() : "NULL");
            }

        } catch (Exception e) {
            log.error("❌ Critical error during multi-source data collection on thread {}: {}",
                    Thread.currentThread().getName(), e.getMessage(), e);
        }

        log.info("🏁 Multi-source data collection completed on thread: {}", Thread.currentThread().getName());
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
     * Get poll interval configurations
     */
    public Map<String, Object> getPollIntervalStatus() {
        return Map.of(
                "dataCollectionInterval", dataCollectionPollInterval,
                "flightradar24Interval", flightradar24PollInterval,
                "adsbexchangeInterval", adsbexchangePollInterval,
                "marinetrafficInterval", marinetrafficPollInterval,
                "vesselfinderInterval", vesselfinderPollInterval,
                "chinaportsInterval", chinaportsPollInterval,
                "marinetrafficv2Interval", marinetrafficv2PollInterval,
                "unit", "milliseconds");
    }

    /**
     * Log current configuration status
     */
    public void logConfigurationStatus() {
        log.info("⚙️ MultiSource External API Configuration:");
        log.info("  📊 Data Collection Interval: {}ms", dataCollectionPollInterval);
        log.info("  ✈️ Aircraft Sources:");
        log.info("    - FlightRadar24: {}ms", flightradar24PollInterval);
        log.info("    - ADS-B Exchange: {}ms", adsbexchangePollInterval);
        log.info("  🚢 Vessel Sources:");
        log.info("    - MarineTraffic: {}ms", marinetrafficPollInterval);
        log.info("    - VesselFinder: {}ms", vesselfinderPollInterval);
        log.info("    - Chinaports: {}ms", chinaportsPollInterval);
        log.info("    - MarineTrafficV2: {}ms", marinetrafficv2PollInterval);
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