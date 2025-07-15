package com.phamnam.tracking_vessel_flight.service.simulator;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.enums.DataSourceType;
import com.phamnam.tracking_vessel_flight.util.SampleDataGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Multi-Source Data Simulator
 * 
 * Provides realistic mock data for all external API sources used by
 * MultiSourceExternalApiService.
 * This simulator generates data that mimics real-world scenarios including:
 * - Different data qualities from different sources
 * - Realistic geographic distributions
 * - Varying response times and data volumes
 * - Error conditions and edge cases
 * 
 * Data Sources Supported:
 * Aircraft: FlightRadar24, ADS-B Exchange
 * Vessels: MarineTraffic, VesselFinder, Chinaports, MarineTrafficV2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiSourceDataSimulator {

    @Value("${simulator.enabled:true}")
    private boolean simulatorEnabled;

    @Value("${simulator.aircraft.count.min:5}")
    private int aircraftCountMin;

    @Value("${simulator.aircraft.count.max:50}")
    private int aircraftCountMax;

    @Value("${simulator.vessel.count.min:10}")
    private int vesselCountMin;

    @Value("${simulator.vessel.count.max:100}")
    private int vesselCountMax;

    @Value("${simulator.data.quality.variation:true}")
    private boolean enableQualityVariation;

    @Value("${simulator.response.delay.min:100}")
    private int responseDelayMin;

    @Value("${simulator.response.delay.max:2000}")
    private int responseDelayMax;

    private final Random random = new Random();

    // ============================================================================
    // AIRCRAFT DATA SIMULATION
    // ============================================================================

    /**
     * Simulate FlightRadar24 aircraft data
     * Characteristics: High quality, comprehensive data, reliable
     */
    public CompletableFuture<List<AircraftTrackingRequest>> simulateFlightRadar24Data() {
        return CompletableFuture.supplyAsync(() -> {
            if (!simulatorEnabled) {
                return Collections.emptyList();
            }

            try {
                simulateNetworkDelay();

                int count = ThreadLocalRandom.current().nextInt(aircraftCountMin, aircraftCountMax + 1);
                List<AircraftTrackingRequest> aircraft = new ArrayList<>();

                log.info("🛩️ FlightRadar24 Simulator: Generating {} aircraft records", count);

                for (int i = 0; i < count; i++) {
                    AircraftTrackingRequest request = generateHighQualityAircraftData("flightradar24");
                    aircraft.add(request);
                }

                log.info("✅ FlightRadar24 Simulator: Generated {} aircraft with high data quality", aircraft.size());
                return aircraft;

            } catch (Exception e) {
                log.error("❌ FlightRadar24 Simulator error: {}", e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Simulate ADS-B Exchange aircraft data
     * Characteristics: Real-time, good quality, technical focus
     */
    public CompletableFuture<List<AircraftTrackingRequest>> simulateAdsbExchangeData() {
        return CompletableFuture.supplyAsync(() -> {
            if (!simulatorEnabled) {
                return Collections.emptyList();
            }

            try {
                simulateNetworkDelay();

                int count = ThreadLocalRandom.current().nextInt(aircraftCountMin, aircraftCountMax + 1);
                List<AircraftTrackingRequest> aircraft = new ArrayList<>();

                log.info("📡 ADS-B Exchange Simulator: Generating {} aircraft records", count);

                for (int i = 0; i < count; i++) {
                    AircraftTrackingRequest request = generateTechnicalAircraftData("adsbexchange");
                    aircraft.add(request);
                }

                log.info("✅ ADS-B Exchange Simulator: Generated {} aircraft with technical data", aircraft.size());
                return aircraft;

            } catch (Exception e) {
                log.error("❌ ADS-B Exchange Simulator error: {}", e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    // ============================================================================
    // VESSEL DATA SIMULATION
    // ============================================================================

    /**
     * Simulate MarineTraffic vessel data
     * Characteristics: Comprehensive maritime data, good coverage
     */
    public CompletableFuture<List<VesselTrackingRequest>> simulateMarineTrafficData() {
        return CompletableFuture.supplyAsync(() -> {
            if (!simulatorEnabled) {
                return Collections.emptyList();
            }

            try {
                simulateNetworkDelay();

                int count = ThreadLocalRandom.current().nextInt(vesselCountMin, vesselCountMax + 1);
                List<VesselTrackingRequest> vessels = new ArrayList<>();

                log.info("🚢 MarineTraffic Simulator: Generating {} vessel records", count);

                for (int i = 0; i < count; i++) {
                    VesselTrackingRequest request = generateCommerciVesselData("marinetraffic");
                    vessels.add(request);
                }

                log.info("✅ MarineTraffic Simulator: Generated {} commercial vessels", vessels.size());
                return vessels;

            } catch (Exception e) {
                log.error("❌ MarineTraffic Simulator error: {}", e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Simulate VesselFinder vessel data
     * Characteristics: Alternative vessel tracking, varied quality
     */
    public CompletableFuture<List<VesselTrackingRequest>> simulateVesselFinderData() {
        return CompletableFuture.supplyAsync(() -> {
            if (!simulatorEnabled) {
                return Collections.emptyList();
            }

            try {
                simulateNetworkDelay();

                int count = ThreadLocalRandom.current().nextInt(vesselCountMin / 2, vesselCountMax / 2 + 1);
                List<VesselTrackingRequest> vessels = new ArrayList<>();

                log.info("🔍 VesselFinder Simulator: Generating {} vessel records", count);

                for (int i = 0; i < count; i++) {
                    VesselTrackingRequest request = generateVariedQualityVesselData("vesselfinder");
                    vessels.add(request);
                }

                log.info("✅ VesselFinder Simulator: Generated {} vessels with varied quality", vessels.size());
                return vessels;

            } catch (Exception e) {
                log.error("❌ VesselFinder Simulator error: {}", e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Simulate Chinaports vessel data
     * Characteristics: Chinese ports focus, regional data
     */
    public CompletableFuture<List<VesselTrackingRequest>> simulateChinaportsData() {
        return CompletableFuture.supplyAsync(() -> {
            if (!simulatorEnabled) {
                return Collections.emptyList();
            }

            try {
                simulateNetworkDelay();

                int count = ThreadLocalRandom.current().nextInt(vesselCountMin / 3, vesselCountMax / 3 + 1);
                List<VesselTrackingRequest> vessels = new ArrayList<>();

                log.info("🇨🇳 Chinaports Simulator: Generating {} vessel records", count);

                for (int i = 0; i < count; i++) {
                    VesselTrackingRequest request = generateChineseRegionalVesselData("chinaports");
                    vessels.add(request);
                }

                log.info("✅ Chinaports Simulator: Generated {} regional Chinese vessels", vessels.size());
                return vessels;

            } catch (Exception e) {
                log.error("❌ Chinaports Simulator error: {}", e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    /**
     * Simulate MarineTrafficV2 vessel data
     * Characteristics: Enhanced API version, better data structure
     */
    public CompletableFuture<List<VesselTrackingRequest>> simulateMarineTrafficV2Data() {
        return CompletableFuture.supplyAsync(() -> {
            if (!simulatorEnabled) {
                return Collections.emptyList();
            }

            try {
                simulateNetworkDelay();

                int count = ThreadLocalRandom.current().nextInt(vesselCountMin, vesselCountMax + 1);
                List<VesselTrackingRequest> vessels = new ArrayList<>();

                log.info("🚢🔄 MarineTrafficV2 Simulator: Generating {} vessel records", count);

                for (int i = 0; i < count; i++) {
                    VesselTrackingRequest request = generateEnhancedVesselData("marinetrafficv2");
                    vessels.add(request);
                }

                log.info("✅ MarineTrafficV2 Simulator: Generated {} enhanced vessels", vessels.size());
                return vessels;

            } catch (Exception e) {
                log.error("❌ MarineTrafficV2 Simulator error: {}", e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    // ============================================================================
    // SPECIALIZED DATA GENERATORS
    // ============================================================================

    /**
     * Generate high-quality aircraft data (FlightRadar24 style)
     */
    private AircraftTrackingRequest generateHighQualityAircraftData(String source) {
        AircraftTrackingRequest base = SampleDataGenerator.generateAircraftTrackingRequest();

        return base.toBuilder()
                .source(source)
                .dataQuality(0.85 + (random.nextDouble() * 0.15)) // 0.85-1.0 quality
                .timestamp(LocalDateTime.now())
                // Ensure all critical fields are present
                .hexident(base.getHexident() != null ? base.getHexident() : generateHexident())
                .callsign(base.getCallsign() != null ? base.getCallsign() : generateCallsign())
                .latitude(base.getLatitude() != null ? base.getLatitude() : generateVietnamLatitude())
                .longitude(base.getLongitude() != null ? base.getLongitude() : generateVietnamLongitude())
                .build();
    }

    /**
     * Generate technical aircraft data (ADS-B Exchange style)
     */
    private AircraftTrackingRequest generateTechnicalAircraftData(String source) {
        AircraftTrackingRequest base = SampleDataGenerator.generateAircraftTrackingRequest();

        return base.toBuilder()
                .source(source)
                .dataQuality(0.75 + (random.nextDouble() * 0.25)) // 0.75-1.0 quality
                .timestamp(LocalDateTime.now())
                // ADS-B focuses on technical parameters
                .verticalRate(random.nextInt(-2000, 2000))
                .squawk(String.format("%04d", random.nextInt(1000, 7777)))
                .build();
    }

    /**
     * Generate commercial vessel data (MarineTraffic style)
     */
    private VesselTrackingRequest generateCommerciVesselData(String source) {
        VesselTrackingRequest base = SampleDataGenerator.generateVesselTrackingRequest();

        String[] commercialTypes = {
                "Container Ship", "Bulk Carrier", "Tanker", "Cargo Ship",
                "Passenger Ship", "RoRo Ship", "Chemical Tanker"
        };

        return base.toBuilder()
                .source(source)
                .dataQuality(0.80 + (random.nextDouble() * 0.20)) // 0.8-1.0 quality
                .vesselType(commercialTypes[random.nextInt(commercialTypes.length)])
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Generate varied quality vessel data (VesselFinder style)
     */
    private VesselTrackingRequest generateVariedQualityVesselData(String source) {
        VesselTrackingRequest base = SampleDataGenerator.generateVesselTrackingRequest();

        // Simulate some missing data
        return base.toBuilder()
                .source(source)
                .dataQuality(0.60 + (random.nextDouble() * 0.40)) // 0.6-1.0 quality
                .timestamp(LocalDateTime.now())
                .imo(random.nextBoolean() ? base.getImo() : null) // 50% chance missing IMO
                .destination(random.nextBoolean() ? base.getDestination() : null) // 50% chance missing destination
                .build();
    }

    /**
     * Generate Chinese regional vessel data (Chinaports style)
     */
    private VesselTrackingRequest generateChineseRegionalVesselData(String source) {
        VesselTrackingRequest base = SampleDataGenerator.generateVesselTrackingRequest();

        String[] chineseFlags = { "CN", "HK", "TW" };
        String[] chinesePorts = {
                "SHANGHAI", "SHENZHEN", "GUANGZHOU", "QINGDAO", "TIANJIN",
                "NINGBO", "DALIAN", "XIAMEN", "HONG KONG"
        };

        return base.toBuilder()
                .source(source)
                .dataQuality(0.70 + (random.nextDouble() * 0.30)) // 0.7-1.0 quality
                .flag(chineseFlags[random.nextInt(chineseFlags.length)])
                .destination(chinesePorts[random.nextInt(chinesePorts.length)])
                .timestamp(LocalDateTime.now())
                // Chinese region coordinates
                .latitude(18.0 + (random.nextDouble() * 23.0)) // 18°N to 41°N
                .longitude(108.0 + (random.nextDouble() * 18.0)) // 108°E to 126°E
                .build();
    }

    /**
     * Generate enhanced vessel data (MarineTrafficV2 style)
     */
    private VesselTrackingRequest generateEnhancedVesselData(String source) {
        VesselTrackingRequest base = SampleDataGenerator.generateVesselTrackingRequest();

        return base.toBuilder()
                .source(source)
                .dataQuality(0.90 + (random.nextDouble() * 0.10)) // 0.9-1.0 quality
                .timestamp(LocalDateTime.now())
                // Enhanced API provides more accurate data
                .navigationStatus("Under way using engine")
                .build();
    }

    // ============================================================================
    // ERROR AND EDGE CASE SIMULATION
    // ============================================================================

    /**
     * Simulate network issues for specific sources
     */
    public CompletableFuture<List<AircraftTrackingRequest>> simulateAircraftNetworkError(String source) {
        return CompletableFuture.supplyAsync(() -> {
            log.warn("⚠️ Simulating network error for aircraft source: {}", source);
            throw new RuntimeException("Simulated network timeout for " + source);
        });
    }

    /**
     * Simulate network issues for vessel sources
     */
    public CompletableFuture<List<VesselTrackingRequest>> simulateVesselNetworkError(String source) {
        return CompletableFuture.supplyAsync(() -> {
            log.warn("⚠️ Simulating network error for vessel source: {}", source);
            throw new RuntimeException("Simulated network timeout for " + source);
        });
    }

    /**
     * Generate empty response simulation
     */
    public <T> CompletableFuture<List<T>> simulateEmptyResponse(String source) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("📭 Simulating empty response from source: {}", source);
            simulateNetworkDelay();
            return Collections.emptyList();
        });
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Simulate network delay
     */
    private void simulateNetworkDelay() {
        try {
            int delay = ThreadLocalRandom.current().nextInt(responseDelayMin, responseDelayMax + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateHexident() {
        return String.format("%06X", random.nextInt(0xFFFFFF));
    }

    private String generateCallsign() {
        String[] airlines = { "VN", "VJ", "QH", "AA", "DL", "UA", "BA", "LH" };
        return airlines[random.nextInt(airlines.length)] + String.format("%04d", random.nextInt(9999));
    }

    private Double generateVietnamLatitude() {
        return 8.0 + (random.nextDouble() * 15.5); // Vietnam area
    }

    private Double generateVietnamLongitude() {
        return 102.0 + (random.nextDouble() * 7.5); // Vietnam area
    }

    // ============================================================================
    // SIMULATOR CONFIGURATION AND STATUS
    // ============================================================================

    /**
     * Get simulator configuration status
     */
    public Map<String, Object> getSimulatorStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("enabled", simulatorEnabled);
        status.put("aircraftRange", Map.of("min", aircraftCountMin, "max", aircraftCountMax));
        status.put("vesselRange", Map.of("min", vesselCountMin, "max", vesselCountMax));
        status.put("qualityVariation", enableQualityVariation);
        status.put("responseDelay", Map.of("min", responseDelayMin, "max", responseDelayMax));

        status.put("supportedSources", Map.of(
                "aircraft", List.of("flightradar24", "adsbexchange"),
                "vessel", List.of("marinetraffic", "vesselfinder", "chinaports", "marinetrafficv2")));

        return status;
    }

    /**
     * Generate comprehensive test dataset for all sources
     */
    public Map<String, Object> generateComprehensiveTestData() {
        Map<String, Object> testData = new HashMap<>();

        log.info("🎯 Generating comprehensive test data for all sources...");

        try {
            // Aircraft data
            Map<String, List<AircraftTrackingRequest>> aircraftData = new HashMap<>();
            aircraftData.put("flightradar24", simulateFlightRadar24Data().get());
            aircraftData.put("adsbexchange", simulateAdsbExchangeData().get());

            // Vessel data
            Map<String, List<VesselTrackingRequest>> vesselData = new HashMap<>();
            vesselData.put("marinetraffic", simulateMarineTrafficData().get());
            vesselData.put("vesselfinder", simulateVesselFinderData().get());
            vesselData.put("chinaports", simulateChinaportsData().get());
            vesselData.put("marinetrafficv2", simulateMarineTrafficV2Data().get());

            testData.put("aircraft", aircraftData);
            testData.put("vessel", vesselData);
            testData.put("generatedAt", LocalDateTime.now());
            testData.put("totalRecords",
                    aircraftData.values().stream().mapToInt(List::size).sum() +
                            vesselData.values().stream().mapToInt(List::size).sum());

            log.info("✅ Generated comprehensive test data with {} total records", testData.get("totalRecords"));

        } catch (Exception e) {
            log.error("❌ Error generating comprehensive test data: {}", e.getMessage());
            testData.put("error", e.getMessage());
        }

        return testData;
    }
}
