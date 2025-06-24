package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataFusionService {

    @Value("${data.fusion.enabled:true}")
    private boolean fusionEnabled;

    @Value("${data.fusion.deduplication.enabled:true}")
    private boolean deduplicationEnabled;

    @Value("${data.fusion.deduplication.time-window:30000}")
    private long deduplicationTimeWindowMs;

    @Value("${data.fusion.quality.threshold:0.5}")
    private double qualityThreshold;

    // Priority configuration for data sources
    private final Map<String, Integer> sourcePriority = new HashMap<>();

    // Cache for recent data to perform deduplication
    private final Map<String, AircraftDataCache> aircraftCache = new ConcurrentHashMap<>();
    private final Map<String, VesselDataCache> vesselCache = new ConcurrentHashMap<>();

    /**
     * Merge aircraft data from multiple sources
     */
    public List<AircraftTrackingRequest> mergeAircraftData(
            Map<String, List<AircraftTrackingRequest>> dataBySource) {

        if (!fusionEnabled) {
            // Simply concatenate all data if fusion is disabled
            return dataBySource.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        Map<String, List<AircraftDataPoint>> groupedData = new HashMap<>();

        // Group data by aircraft identifier (hexident)
        for (Map.Entry<String, List<AircraftTrackingRequest>> entry : dataBySource.entrySet()) {
            String source = entry.getKey();
            List<AircraftTrackingRequest> dataList = entry.getValue();

            for (AircraftTrackingRequest data : dataList) {
                String hexident = data.getHexident();
                groupedData.computeIfAbsent(hexident, k -> new ArrayList<>())
                        .add(new AircraftDataPoint(data, source, LocalDateTime.now()));
            }
        }

        // Merge data for each aircraft
        List<AircraftTrackingRequest> mergedData = new ArrayList<>();
        for (Map.Entry<String, List<AircraftDataPoint>> entry : groupedData.entrySet()) {
            AircraftTrackingRequest merged = fusionAircraftData(entry.getKey(), entry.getValue());
            if (merged != null && merged.getDataQuality() >= qualityThreshold) {
                mergedData.add(merged);
            }
        }

        return mergedData;
    }

    /**
     * Merge vessel data from multiple sources
     */
    public List<VesselTrackingRequest> mergeVesselData(
            Map<String, List<VesselTrackingRequest>> dataBySource) {

        log.info("🔄 Starting vessel data fusion...");
        log.info("📊 Input data: {} sources with total {} records",
                dataBySource.size(),
                dataBySource.values().stream().mapToInt(List::size).sum());

        if (!fusionEnabled) {
            log.info("⚡ Fusion disabled, returning all data concatenated");
            return dataBySource.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        Map<String, List<VesselDataPoint>> groupedData = new HashMap<>();

        // Group data by vessel identifier (MMSI)
        for (Map.Entry<String, List<VesselTrackingRequest>> entry : dataBySource.entrySet()) {
            String source = entry.getKey();
            List<VesselTrackingRequest> dataList = entry.getValue();

            log.debug("📋 Processing {} records from source: {}", dataList.size(), source);

            for (VesselTrackingRequest data : dataList) {
                String mmsi = data.getMmsi();
                groupedData.computeIfAbsent(mmsi, k -> new ArrayList<>())
                        .add(new VesselDataPoint(data, source, LocalDateTime.now()));
            }
        }

        log.info("🔗 Grouped data by MMSI: {} unique vessels", groupedData.size());

        // Merge data for each vessel
        List<VesselTrackingRequest> mergedData = new ArrayList<>();
        int filteredByQuality = 0;
        int duplicatesSkipped = 0;

        for (Map.Entry<String, List<VesselDataPoint>> entry : groupedData.entrySet()) {
            String mmsi = entry.getKey();
            VesselTrackingRequest merged = fusionVesselData(mmsi, entry.getValue());

            if (merged == null) {
                duplicatesSkipped++;
                log.debug("⏭️ Skipped vessel {} - duplicate or null", mmsi);
            } else if (merged.getDataQuality() < qualityThreshold) {
                filteredByQuality++;
                log.warn("⚠️ Filtered vessel {} - quality {} < threshold {}",
                        mmsi, merged.getDataQuality(), qualityThreshold);
            } else {
                mergedData.add(merged);
                log.debug("✅ Merged vessel {} with quality {}", mmsi, merged.getDataQuality());
            }
        }

        log.info("✅ Fusion completed: {} vessels output, {} filtered by quality, {} duplicates skipped",
                mergedData.size(), filteredByQuality, duplicatesSkipped);
        log.info("🎯 Quality threshold: {}, Deduplication: {}", qualityThreshold, deduplicationEnabled);

        return mergedData;
    }

    /**
     * Fusion algorithm for aircraft data
     */
    private AircraftTrackingRequest fusionAircraftData(String hexident, List<AircraftDataPoint> dataPoints) {
        if (dataPoints.isEmpty()) {
            return null;
        }

        // Check cache for deduplication
        if (deduplicationEnabled) {
            AircraftDataCache cached = aircraftCache.get(hexident);
            if (cached != null && isDuplicate(cached, dataPoints)) {
                log.debug("Duplicate aircraft data detected for {}, skipping", hexident);
                return null;
            }
        }

        // Sort by priority and timestamp
        dataPoints.sort((a, b) -> {
            int priorityCompare = getSourcePriority(a.source).compareTo(getSourcePriority(b.source));
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return b.timestamp.compareTo(a.timestamp);
        });

        // Use highest priority data as base
        AircraftTrackingRequest base = dataPoints.get(0).data;
        AircraftTrackingRequest.AircraftTrackingRequestBuilder fusedBuilder = base.toBuilder();

        // Merge complementary data from other sources
        for (int i = 1; i < dataPoints.size(); i++) {
            AircraftTrackingRequest complement = dataPoints.get(i).data;

            // Fill missing fields from lower priority sources
            if (base.getCallsign() == null && complement.getCallsign() != null) {
                fusedBuilder.callsign(complement.getCallsign());
            }
            if (base.getAircraftType() == null && complement.getAircraftType() != null) {
                fusedBuilder.aircraftType(complement.getAircraftType());
            }
            if (base.getRegistration() == null && complement.getRegistration() != null) {
                fusedBuilder.registration(complement.getRegistration());
            }
        }

        // Calculate average position if multiple recent sources
        List<AircraftDataPoint> recentPoints = dataPoints.stream()
                .filter(p -> ChronoUnit.SECONDS.between(p.timestamp, LocalDateTime.now()) < 30)
                .filter(p -> p.data.getLatitude() != null && p.data.getLongitude() != null)
                .collect(Collectors.toList());

        if (recentPoints.size() > 1) {
            double avgLat = recentPoints.stream()
                    .mapToDouble(p -> p.data.getLatitude())
                    .average().orElse(base.getLatitude() != null ? base.getLatitude() : 0.0);
            double avgLon = recentPoints.stream()
                    .mapToDouble(p -> p.data.getLongitude())
                    .average().orElse(base.getLongitude() != null ? base.getLongitude() : 0.0);

            fusedBuilder.latitude(avgLat);
            fusedBuilder.longitude(avgLon);
        }

        // Calculate data quality score
        double qualityScore = calculateAircraftDataQuality(dataPoints);
        fusedBuilder.dataQuality(qualityScore);

        // Update cache
        AircraftTrackingRequest fused = fusedBuilder.build();
        aircraftCache.put(hexident, new AircraftDataCache(fused, LocalDateTime.now()));

        // log.debug("Fused aircraft data for {} from {} sources with quality {}",
        // hexident, dataPoints.size(), qualityScore);

        return fused;
    }

    /**
     * Fusion algorithm for vessel data
     */
    private VesselTrackingRequest fusionVesselData(String mmsi, List<VesselDataPoint> dataPoints) {
        if (dataPoints.isEmpty()) {
            log.debug("❌ No data points for vessel {}", mmsi);
            return null;
        }

        log.debug("🔧 Fusing vessel {} with {} data points from sources: {}",
                mmsi, dataPoints.size(),
                dataPoints.stream().map(p -> p.source).collect(Collectors.toList()));

        // Check cache for deduplication
        if (deduplicationEnabled) {
            VesselDataCache cached = vesselCache.get(mmsi);
            if (cached != null && isDuplicate(cached, dataPoints)) {
                log.debug("⏭️ Duplicate vessel data detected for {}, skipping", mmsi);
                return null;
            }
        }

        // Sort by priority and timestamp
        dataPoints.sort((a, b) -> {
            int priorityCompare = getSourcePriority(a.source).compareTo(getSourcePriority(b.source));
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return b.timestamp.compareTo(a.timestamp);
        });

        // Use highest priority data as base
        VesselTrackingRequest base = dataPoints.get(0).data;
        VesselTrackingRequest.VesselTrackingRequestBuilder fusedBuilder = base.toBuilder();

        // Merge complementary data
        for (int i = 1; i < dataPoints.size(); i++) {
            VesselTrackingRequest complement = dataPoints.get(i).data;

            if (base.getVesselName() == null && complement.getVesselName() != null) {
                fusedBuilder.vesselName(complement.getVesselName());
            }
            if (base.getDestination() == null && complement.getDestination() != null) {
                fusedBuilder.destination(complement.getDestination());
            }
            if (base.getImo() == null && complement.getImo() != null) {
                fusedBuilder.imo(complement.getImo());
            }
        }

        // Calculate average position if multiple recent sources
        List<VesselDataPoint> recentPoints = dataPoints.stream()
                .filter(p -> ChronoUnit.SECONDS.between(p.timestamp, LocalDateTime.now()) < 60)
                .filter(p -> p.data.getLatitude() != null && p.data.getLongitude() != null)
                .toList();

        if (recentPoints.size() > 1) {
            double avgLat = recentPoints.stream()
                    .mapToDouble(p -> p.data.getLatitude())
                    .average().orElse(base.getLatitude() != null ? base.getLatitude() : 0.0);
            double avgLon = recentPoints.stream()
                    .mapToDouble(p -> p.data.getLongitude())
                    .average().orElse(base.getLongitude() != null ? base.getLongitude() : 0.0);

            fusedBuilder.latitude(avgLat);
            fusedBuilder.longitude(avgLon);
        }

        // Calculate data quality score
        double qualityScore = calculateVesselDataQuality(dataPoints);
        fusedBuilder.dataQuality(qualityScore);

        // Update cache
        VesselTrackingRequest fused = fusedBuilder.build();
        vesselCache.put(mmsi, new VesselDataCache(fused, LocalDateTime.now()));

        log.debug("✅ Fused vessel {} from {} sources with quality {} (base: {}, threshold: {})",
                mmsi, dataPoints.size(), qualityScore,
                dataPoints.get(0).data.getDataQuality(), qualityThreshold);

        return fused;
    }

    /**
     * Check if data is duplicate based on cache
     */
    private boolean isDuplicate(AircraftDataCache cached, List<AircraftDataPoint> newData) {
        long timeDiff = ChronoUnit.MILLIS.between(cached.timestamp, LocalDateTime.now());
        if (timeDiff > deduplicationTimeWindowMs) {
            return false;
        }

        // Check if position has changed significantly
        AircraftTrackingRequest cachedData = cached.data;
        AircraftTrackingRequest latestData = newData.get(0).data;

        // Skip duplicate check if any coordinates are null
        if (cachedData.getLatitude() == null || cachedData.getLongitude() == null ||
                latestData.getLatitude() == null || latestData.getLongitude() == null) {
            log.debug("Skipping duplicate check for aircraft - missing coordinates");
            return false; // Don't consider as duplicate if we can't compare positions
        }

        double distance = calculateDistance(
                cachedData.getLatitude(), cachedData.getLongitude(),
                latestData.getLatitude(), latestData.getLongitude());

        return distance < 0.001; // Less than ~100 meters
    }

    private boolean isDuplicate(VesselDataCache cached, List<VesselDataPoint> newData) {
        long timeDiff = ChronoUnit.MILLIS.between(cached.timestamp, LocalDateTime.now());
        if (timeDiff > deduplicationTimeWindowMs) {
            return false;
        }

        VesselTrackingRequest cachedData = cached.data;
        VesselTrackingRequest latestData = newData.get(0).data;

        // Skip duplicate check if any coordinates are null
        if (cachedData.getLatitude() == null || cachedData.getLongitude() == null ||
                latestData.getLatitude() == null || latestData.getLongitude() == null) {
            log.debug("Skipping duplicate check for vessel - missing coordinates");
            return false; // Don't consider as duplicate if we can't compare positions
        }

        double distance = calculateDistance(
                cachedData.getLatitude(), cachedData.getLongitude(),
                latestData.getLatitude(), latestData.getLongitude());

        return distance < 0.001; // Less than ~100 meters
    }

    /**
     * Calculate data quality score for aircraft
     */
    private double calculateAircraftDataQuality(List<AircraftDataPoint> dataPoints) {
        double baseQuality = dataPoints.get(0).data.getDataQuality();

        // Increase quality if multiple sources agree
        if (dataPoints.size() > 1) {
            double agreementBonus = Math.min(0.2, dataPoints.size() * 0.05);
            baseQuality = Math.min(1.0, baseQuality + agreementBonus);
        }

        // Check data freshness
        long ageSeconds = ChronoUnit.SECONDS.between(dataPoints.get(0).timestamp, LocalDateTime.now());
        if (ageSeconds > 60) {
            baseQuality *= 0.8; // Reduce quality for older data
        }

        return baseQuality;
    }

    /**
     * Calculate data quality score for vessel
     */
    private double calculateVesselDataQuality(List<VesselDataPoint> dataPoints) {
        double baseQuality = dataPoints.get(0).data.getDataQuality();

        if (dataPoints.size() > 1) {
            double agreementBonus = Math.min(0.2, dataPoints.size() * 0.05);
            baseQuality = Math.min(1.0, baseQuality + agreementBonus);
        }

        long ageSeconds = ChronoUnit.SECONDS.between(dataPoints.get(0).timestamp, LocalDateTime.now());
        if (ageSeconds > 120) {
            baseQuality *= 0.8;
        }

        return baseQuality;
    }

    /**
     * Calculate distance between two coordinates (Haversine formula)
     */
    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        // Null safety check
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            log.warn("Cannot calculate distance with null coordinates: lat1={}, lon1={}, lat2={}, lon2={}",
                    lat1, lon1, lat2, lon2);
            return Double.MAX_VALUE; // Return max value to indicate invalid distance
        }

        double earthRadius = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * Get source priority (lower number = higher priority)
     */
    private Integer getSourcePriority(String source) {
        return sourcePriority.getOrDefault(source.toLowerCase(), 999);
    }

    /**
     * Initialize source priorities from configuration
     */
    @Value("${data.fusion.priority.flightradar24:1}")
    public void setFlightRadar24Priority(int priority) {
        sourcePriority.put("flightradar24", priority);
    }

    @Value("${data.fusion.priority.adsbexchange:2}")
    public void setAdsbExchangePriority(int priority) {
        sourcePriority.put("adsbexchange", priority);
    }

    @Value("${data.fusion.priority.marinetraffic:1}")
    public void setMarineTrafficPriority(int priority) {
        sourcePriority.put("marinetraffic", priority);
    }

    @Value("${data.fusion.priority.vesselfinder:2}")
    public void setVesselFinderPriority(int priority) {
        sourcePriority.put("vesselfinder", priority);
    }

    @Value("${data.fusion.priority.chinaports:3}")
    public void setChinaportsPriority(int priority) {
        sourcePriority.put("chinaports", priority);
    }

    @Value("${data.fusion.priority.marinetrafficv2:4}")
    public void setMarineTrafficV2Priority(int priority) {
        sourcePriority.put("marinetrafficv2", priority);
    }

    // Inner classes for data points
    private static class AircraftDataPoint {
        final AircraftTrackingRequest data;
        final String source;
        final LocalDateTime timestamp;

        AircraftDataPoint(AircraftTrackingRequest data, String source, LocalDateTime timestamp) {
            this.data = data;
            this.source = source;
            this.timestamp = timestamp;
        }
    }

    private static class VesselDataPoint {
        final VesselTrackingRequest data;
        final String source;
        final LocalDateTime timestamp;

        VesselDataPoint(VesselTrackingRequest data, String source, LocalDateTime timestamp) {
            this.data = data;
            this.source = source;
            this.timestamp = timestamp;
        }
    }

    // Cache classes
    private static class AircraftDataCache {
        final AircraftTrackingRequest data;
        final LocalDateTime timestamp;

        AircraftDataCache(AircraftTrackingRequest data, LocalDateTime timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }

    private static class VesselDataCache {
        final VesselTrackingRequest data;
        final LocalDateTime timestamp;

        VesselDataCache(VesselTrackingRequest data, LocalDateTime timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}