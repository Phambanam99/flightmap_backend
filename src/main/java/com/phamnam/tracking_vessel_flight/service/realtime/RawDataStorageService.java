package com.phamnam.tracking_vessel_flight.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.RawVesselData;
import com.phamnam.tracking_vessel_flight.repository.RawAircraftDataRepository;
import com.phamnam.tracking_vessel_flight.repository.RawVesselDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawDataStorageService {

    private final RawAircraftDataRepository rawAircraftDataRepository;
    private final RawVesselDataRepository rawVesselDataRepository;
    private final ObjectMapper objectMapper;

    // Injected filtering and optimization services
    private final RawDataFilteringService filteringService;
    private final RawDataCompressionService compressionService;

    @Value("${raw.data.storage.enabled:true}")
    private boolean storageEnabled;

    @Value("${raw.data.retention.days:30}")
    private int retentionDays;

    @Value("${raw.data.compression.enabled:true}")
    private boolean compressionEnabled;

    // Source priority mapping (should match DataFusionService)
    private final Map<String, Integer> sourcePriorities = Map.of(
            "flightradar24", 1,
            "adsbexchange", 2,
            "marinetraffic", 1,
            "vesselfinder", 2,
            "chinaports", 3,
            "marinetrafficv2", 4);

    /**
     * Store raw aircraft data from external source
     */
    @Async("taskExecutor")
    @Transactional
    public void storeRawAircraftData(String dataSource,
                                     List<AircraftTrackingRequest> aircraftData,
                                     String apiEndpoint,
                                     long responseTimeMs) {
        if (!storageEnabled || aircraftData == null || aircraftData.isEmpty()) {
            CompletableFuture.completedFuture(null);
            return;
        }

        try {
            // Apply filtering and compression
            List<RawAircraftData> rawDataList = aircraftData.stream()
                    .filter(filteringService::shouldStoreAircraftRawData)
                    .map(aircraft -> convertToRawAircraftData(aircraft, dataSource, apiEndpoint, responseTimeMs))
                    .filter(Objects::nonNull)
                    .toList();

            rawAircraftDataRepository.saveAll(rawDataList);

            log.debug("Stored {} raw aircraft records from source: {} (filtered from {} total)",
                    rawDataList.size(), dataSource, aircraftData.size());
            CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Failed to store raw aircraft data from source {}: {}", dataSource, e.getMessage());
            CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Store raw vessel data from external source
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> storeRawVesselData(String dataSource,
            List<VesselTrackingRequest> vesselData,
            String apiEndpoint,
            long responseTimeMs) {
        if (!storageEnabled || vesselData == null || vesselData.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            // Apply filtering and compression
            List<RawVesselData> rawDataList = vesselData.stream()
                    .filter(vessel -> filteringService.shouldStoreVesselRawData(vessel))
                    .map(vessel -> convertToRawVesselData(vessel, dataSource, apiEndpoint, responseTimeMs))
                    .filter(data -> data != null)
                    .toList();

            rawVesselDataRepository.saveAll(rawDataList);

            log.debug("Stored {} raw vessel records from source: {} (filtered from {} total)",
                    rawDataList.size(), dataSource, vesselData.size());
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Failed to store raw vessel data from source {}: {}", dataSource, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Convert AircraftTrackingRequest to RawAircraftData
     */
    private RawAircraftData convertToRawAircraftData(AircraftTrackingRequest aircraft,
            String dataSource,
            String apiEndpoint,
            long responseTimeMs) {
        try {
            return RawAircraftData.builder()
                    .dataSource(dataSource)
                    .sourcePriority(sourcePriorities.getOrDefault(dataSource, 999))
                    .apiResponseTime(responseTimeMs)
                    .hexident(aircraft.getHexident())
                    .callsign(aircraft.getCallsign())
                    .registration(aircraft.getRegistration())
                    .aircraftType(aircraft.getAircraftType())
                    .latitude(aircraft.getLatitude())
                    .longitude(aircraft.getLongitude())
                    .altitude(aircraft.getAltitude())
                    .groundSpeed(aircraft.getGroundSpeed())
                    .track(aircraft.getTrack())
                    .verticalRate(aircraft.getVerticalRate())
                    .squawk(aircraft.getSquawk())
                    .onGround(aircraft.getOnGround())
                    .emergency(aircraft.getEmergency())
                    .dataQuality(aircraft.getDataQuality())
                    .originalTimestamp(aircraft.getTimestamp())
                    .receivedAt(LocalDateTime.now())
                    .apiEndpoint(apiEndpoint)
                    .rawJson(compressionService.compressJsonData(convertToJson(aircraft)))
                    .isValid(validateAircraftData(aircraft))
                    .retentionDays(retentionDays)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to convert aircraft data for {}: {}", aircraft.getHexident(), e.getMessage());
            return null;
        }
    }

    /**
     * Convert VesselTrackingRequest to RawVesselData
     */
    private RawVesselData convertToRawVesselData(VesselTrackingRequest vessel,
            String dataSource,
            String apiEndpoint,
            long responseTimeMs) {
        try {
            return RawVesselData.builder()
                    .dataSource(dataSource)
                    .sourcePriority(sourcePriorities.getOrDefault(dataSource, 999))
                    .apiResponseTime(responseTimeMs)
                    .mmsi(vessel.getMmsi())
                    .imo(vessel.getImo())
                    .callsign(vessel.getCallsign())
                    .vesselName(vessel.getVesselName())
                    .vesselType(vessel.getVesselType())
                    .latitude(vessel.getLatitude())
                    .longitude(vessel.getLongitude())
                    .speed(vessel.getSpeed())
                    .course(vessel.getCourse())
                    .heading(vessel.getHeading())
                    .navigationStatus(vessel.getNavigationStatus())
                    .destination(vessel.getDestination())
                    .eta(vessel.getEta())
                    .length(vessel.getLength())
                    .width(vessel.getWidth())
                    .draught(vessel.getDraught())
                    .flag(vessel.getFlag())
                    .cargoType(vessel.getCargoType())
                    .grossTonnage(vessel.getGrossTonnage())
                    .deadweight(vessel.getDeadweight())
                    .buildYear(vessel.getBuildYear())
                    .lastPort(vessel.getLastPort())
                    .nextPort(vessel.getNextPort())
                    .route(vessel.getRoute())
                    .dataQuality(vessel.getDataQuality())
                    .originalTimestamp(vessel.getTimestamp())
                    .receivedAt(LocalDateTime.now())
                    .apiEndpoint(apiEndpoint)
                    .rawJson(compressionService.compressJsonData(convertToJson(vessel)))
                    .isValid(validateVesselData(vessel))
                    .dangerousCargo(vessel.getDangerousCargo())
                    .securityAlert(vessel.getSecurityAlert())
                    .retentionDays(retentionDays)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to convert vessel data for {}: {}", vessel.getMmsi(), e.getMessage());
            return null;
        }
    }

    /**
     * Convert object to JSON string for storage
     */
    private String convertToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate aircraft data
     */
    private boolean validateAircraftData(AircraftTrackingRequest aircraft) {
        return aircraft.getHexident() != null &&
                aircraft.getLatitude() != null &&
                aircraft.getLongitude() != null &&
                aircraft.getLatitude() >= -90 && aircraft.getLatitude() <= 90 &&
                aircraft.getLongitude() >= -180 && aircraft.getLongitude() <= 180;
    }

    /**
     * Validate vessel data
     */
    private boolean validateVesselData(VesselTrackingRequest vessel) {
        return vessel.getMmsi() != null &&
                vessel.getLatitude() != null &&
                vessel.getLongitude() != null &&
                vessel.getLatitude() >= -90 && vessel.getLatitude() <= 90 &&
                vessel.getLongitude() >= -180 && vessel.getLongitude() <= 180;
    }

    /**
     * Link fusion result back to raw data
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> linkFusionResult(String identifier,
            Long fusionResultId,
            boolean isAircraft,
            LocalDateTime processedAt) {
        try {
            if (isAircraft) {
                List<RawAircraftData> rawData = rawAircraftDataRepository
                        .findByHexidentAndReceivedAtBetween(identifier,
                                processedAt.minusMinutes(5), processedAt.plusMinutes(1));
                rawData.forEach(data -> {
                    data.setFusionResultId(fusionResultId);
                    data.setProcessedAt(processedAt);
                });
                rawAircraftDataRepository.saveAll(rawData);
            } else {
                List<RawVesselData> rawData = rawVesselDataRepository
                        .findByMmsiAndReceivedAtBetween(identifier,
                                processedAt.minusMinutes(5), processedAt.plusMinutes(1));
                rawData.forEach(data -> {
                    data.setFusionResultId(fusionResultId);
                    data.setProcessedAt(processedAt);
                });
                rawVesselDataRepository.saveAll(rawData);
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to link fusion result for {}: {}", identifier, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Get raw data statistics
     */
    public Map<String, Object> getRawDataStatistics(LocalDateTime start, LocalDateTime end) {
        try {
            List<Object[]> aircraftStats = rawAircraftDataRepository.countRecordsBySource(start, end);
            List<Object[]> vesselStats = rawVesselDataRepository.countRecordsBySource(start, end);
            List<Object[]> aircraftQuality = rawAircraftDataRepository.getDataQualityBySource(start, end);
            List<Object[]> vesselQuality = rawVesselDataRepository.getDataQualityBySource(start, end);

            return Map.of(
                    "period", Map.of("start", start, "end", end),
                    "aircraftRecordsBySource", aircraftStats,
                    "vesselRecordsBySource", vesselStats,
                    "aircraftQualityBySource", aircraftQuality,
                    "vesselQualityBySource", vesselQuality,
                    "storageEnabled", storageEnabled,
                    "retentionDays", retentionDays);
        } catch (Exception e) {
            log.error("Failed to get raw data statistics: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Cleanup old raw data based on retention policy
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupOldRawData() {
        if (!storageEnabled) {
            return;
        }

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            List<RawAircraftData> oldAircraftData = rawAircraftDataRepository.findByReceivedAtBefore(cutoff);
            List<RawVesselData> oldVesselData = rawVesselDataRepository.findByReceivedAtBefore(cutoff);

            if (!oldAircraftData.isEmpty()) {
                rawAircraftDataRepository.deleteAll(oldAircraftData);
                log.info("Cleaned up {} old aircraft raw data records", oldAircraftData.size());
            }

            if (!oldVesselData.isEmpty()) {
                rawVesselDataRepository.deleteAll(oldVesselData);
                log.info("Cleaned up {} old vessel raw data records", oldVesselData.size());
            }

        } catch (Exception e) {
            log.error("Failed to cleanup old raw data: {}", e.getMessage());
        }
    }

    /**
     * Manual trigger for data cleanup
     */
    public void triggerDataCleanup() {
        log.info("Manually triggering raw data cleanup...");
        cleanupOldRawData();
    }
}