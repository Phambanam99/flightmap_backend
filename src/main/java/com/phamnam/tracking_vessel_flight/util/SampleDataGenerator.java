package com.phamnam.tracking_vessel_flight.util;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.raw.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.raw.RawVesselData;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sample Data Generator for Testing
 * 
 * Generates realistic sample data for aircraft and vessels for testing
 * purposes.
 * Includes various data quality scenarios and edge cases.
 */
public class SampleDataGenerator {

    private static final Random random = new Random();

    // Sample aircraft data
    private static final String[] AIRCRAFT_TYPES = {
            "B737", "A320", "B777", "A330", "B787", "A350", "B747", "A380", "CRJ9", "E190"
    };

    private static final String[] AIRLINES = {
            "VN", "VJ", "QH", "AA", "DL", "UA", "BA", "LH", "AF", "KL"
    };

    private static final String[] AIRCRAFT_REGISTRATIONS = {
            "VN-A123", "VN-B456", "N123AA", "N456DL", "G-BAAA", "D-ABCD", "F-GKXY", "PH-ABC"
    };

    // Sample vessel data
    private static final String[] VESSEL_TYPES = {
            "Container Ship", "Bulk Carrier", "Tanker", "Cargo Ship", "Passenger Ship",
            "Fishing Vessel", "Tug", "Supply Vessel", "Offshore Vessel", "Naval Ship"
    };

    private static final String[] VESSEL_NAMES = {
            "EVER GIVEN", "MAERSK ALABAMA", "MSC OSCAR", "CMA CGM MARCO POLO",
            "OOCL HONG KONG", "MOL TRIUMPH", "APL CHINA", "COSCO SHIPPING UNIVERSE",
            "HAPAG LLOYD BERLIN", "ONE COLUMBO"
    };

    private static final String[] FLAGS = {
            "VN", "SG", "MY", "TH", "PH", "CN", "JP", "KR", "US", "GB", "DE", "FR", "NL", "DK"
    };

    private static final String[] DESTINATIONS = {
            "SINGAPORE", "HO CHI MINH CITY", "HONG KONG", "SHANGHAI", "BUSAN",
            "YOKOHAMA", "BANGKOK", "MANILA", "JAKARTA", "KUALA LUMPUR"
    };

    // Geographic bounds for realistic coordinates
    private static final double VIETNAM_LAT_MIN = 8.0;
    private static final double VIETNAM_LAT_MAX = 23.5;
    private static final double VIETNAM_LON_MIN = 102.0;
    private static final double VIETNAM_LON_MAX = 109.5;

    private static final double ASIA_LAT_MIN = -10.0;
    private static final double ASIA_LAT_MAX = 50.0;
    private static final double ASIA_LON_MIN = 90.0;
    private static final double ASIA_LON_MAX = 140.0;

    // ============================================================================
    // AIRCRAFT DATA GENERATION
    // ============================================================================

    /**
     * Generate a single sample aircraft tracking request
     */
    public static AircraftTrackingRequest generateAircraftTrackingRequest() {
        return generateAircraftTrackingRequest(null);
    }

    /**
     * Generate a sample aircraft tracking request with specific hexident
     */
    public static AircraftTrackingRequest generateAircraftTrackingRequest(String hexident) {
        String actualHexident = hexident != null ? hexident : generateHexident();

        return AircraftTrackingRequest.builder()
                .hexident(actualHexident)
                .callsign(generateCallsign())
                .latitude(generateLatitude(false))
                .longitude(generateLongitude(false))
                .altitude(generateAltitude())
                .groundSpeed(generateGroundSpeed())
                .track(generateTrack())
                .verticalRate(generateVerticalRate())
                .squawk(generateSquawk())
                .aircraftType(getRandomElement(AIRCRAFT_TYPES))
                .registration(getRandomElement(AIRCRAFT_REGISTRATIONS))
                .onGround(random.nextBoolean())
                .emergency(random.nextDouble() < 0.01) // 1% chance of emergency
                .timestamp(LocalDateTime.now())
                .dataQuality(generateDataQuality())
                .source("test")
                .build();
    }

    /**
     * Generate multiple aircraft tracking requests
     */
    public static List<AircraftTrackingRequest> generateAircraftTrackingRequests(int count) {
        List<AircraftTrackingRequest> aircraft = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            aircraft.add(generateAircraftTrackingRequest());
        }
        return aircraft;
    }

    /**
     * Generate raw aircraft data from source
     */
    public static RawAircraftData generateRawAircraftData(String source, String apiEndpoint) {
        AircraftTrackingRequest aircraft = generateAircraftTrackingRequest();
        return RawAircraftData.fromSource(source, apiEndpoint, aircraft, random.nextLong(100, 2000));
    }

    /**
     * Generate multiple raw aircraft data from different sources
     */
    public static Map<String, List<RawAircraftData>> generateRawAircraftDataBySource(int countPerSource) {
        Map<String, List<RawAircraftData>> dataBySource = new HashMap<>();

        String[] sources = { "flightradar24", "adsbexchange" };

        for (String source : sources) {
            List<RawAircraftData> sourceData = new ArrayList<>();
            for (int i = 0; i < countPerSource; i++) {
                sourceData.add(generateRawAircraftData(source, "/api/aircraft/" + source));
            }
            dataBySource.put(source, sourceData);
        }

        return dataBySource;
    }

    // ============================================================================
    // VESSEL DATA GENERATION
    // ============================================================================

    /**
     * Generate a single sample vessel tracking request
     */
    public static VesselTrackingRequest generateVesselTrackingRequest() {
        return generateVesselTrackingRequest(null);
    }

    /**
     * Generate a sample vessel tracking request with specific MMSI
     */
    public static VesselTrackingRequest generateVesselTrackingRequest(String mmsi) {
        String actualMmsi = mmsi != null ? mmsi : generateMmsi();

        return VesselTrackingRequest.builder()
                .mmsi(actualMmsi)
                .imo(generateImo())
                .vesselName(getRandomElement(VESSEL_NAMES))
                .callsign(generateVesselCallsign())
                .latitude(generateLatitude(true))
                .longitude(generateLongitude(true))
                .speed(generateVesselSpeed())
                .course(generateCourse())
                .heading(generateHeading())
                .navigationStatus(generateNavigationStatus())
                .vesselType(getRandomElement(VESSEL_TYPES))
                .length(generateVesselLength())
                .width(generateVesselWidth())
                .draught(generateDraught())
                .flag(getRandomElement(FLAGS))
                .destination(getRandomElement(DESTINATIONS))
                .eta(generateEta())
                .timestamp(LocalDateTime.now())
                .dataQuality(generateDataQuality())
                .source("test")
                .build();
    }

    /**
     * Generate multiple vessel tracking requests
     */
    public static List<VesselTrackingRequest> generateVesselTrackingRequests(int count) {
        List<VesselTrackingRequest> vessels = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            vessels.add(generateVesselTrackingRequest());
        }
        return vessels;
    }

    /**
     * Generate raw vessel data from source
     */
    public static RawVesselData generateRawVesselData(String source, String apiEndpoint) {
        VesselTrackingRequest vessel = generateVesselTrackingRequest();
        return RawVesselData.fromSource(source, apiEndpoint, vessel, random.nextLong(100, 3000));
    }

    /**
     * Generate multiple raw vessel data from different sources
     */
    public static Map<String, List<RawVesselData>> generateRawVesselDataBySource(int countPerSource) {
        Map<String, List<RawVesselData>> dataBySource = new HashMap<>();

        String[] sources = { "marinetraffic", "vesselfinder", "chinaports", "marinetrafficv2" };

        for (String source : sources) {
            List<RawVesselData> sourceData = new ArrayList<>();
            for (int i = 0; i < countPerSource; i++) {
                sourceData.add(generateRawVesselData(source, "/api/vessels/" + source));
            }
            dataBySource.put(source, sourceData);
        }

        return dataBySource;
    }

    // ============================================================================
    // SPECIAL SCENARIOS GENERATION
    // ============================================================================

    /**
     * Generate aircraft data with intentional quality issues for testing
     */
    public static AircraftTrackingRequest generatePoorQualityAircraftData() {
        return AircraftTrackingRequest.builder()
                .hexident(generateHexident())
                .callsign(random.nextBoolean() ? null : generateCallsign()) // 50% missing callsign
                .latitude(random.nextBoolean() ? null : generateLatitude(false)) // 50% missing coordinates
                .longitude(random.nextBoolean() ? null : generateLongitude(false))
                .altitude(random.nextBoolean() ? null : generateAltitude())
                .groundSpeed(random.nextBoolean() ? null : generateGroundSpeed())
                .track(random.nextBoolean() ? null : generateTrack())
                .aircraftType(random.nextBoolean() ? null : getRandomElement(AIRCRAFT_TYPES))
                .timestamp(LocalDateTime.now())
                .dataQuality(random.nextDouble() * 0.5) // Low quality (0-0.5)
                .source("test-poor-quality")
                .build();
    }

    /**
     * Generate vessel data with intentional quality issues for testing
     */
    public static VesselTrackingRequest generatePoorQualityVesselData() {
        return VesselTrackingRequest.builder()
                .mmsi(generateMmsi())
                .vesselName(random.nextBoolean() ? null : getRandomElement(VESSEL_NAMES))
                .latitude(random.nextBoolean() ? null : generateLatitude(true))
                .longitude(random.nextBoolean() ? null : generateLongitude(true))
                .speed(random.nextBoolean() ? null : generateVesselSpeed())
                .course(random.nextBoolean() ? null : generateCourse())
                .vesselType(random.nextBoolean() ? null : getRandomElement(VESSEL_TYPES))
                .timestamp(LocalDateTime.now())
                .dataQuality(random.nextDouble() * 0.4) // Low quality (0-0.4)
                .source("test-poor-quality")
                .build();
    }

    /**
     * Generate duplicate aircraft data for deduplication testing
     */
    public static List<AircraftTrackingRequest> generateDuplicateAircraftData(String hexident, int count) {
        List<AircraftTrackingRequest> duplicates = new ArrayList<>();

        // Generate base data
        AircraftTrackingRequest base = generateAircraftTrackingRequest(hexident);

        for (int i = 0; i < count; i++) {
            // Create slight variations of the same aircraft
            AircraftTrackingRequest duplicate = base.toBuilder()
                    .latitude(base.getLatitude() + (random.nextDouble() - 0.5) * 0.001) // Small position variations
                    .longitude(base.getLongitude() + (random.nextDouble() - 0.5) * 0.001)
                    .altitude(base.getAltitude() + random.nextInt(100) - 50) // Small altitude variations
                    .timestamp(LocalDateTime.now().plusSeconds(i * 5)) // Different timestamps
                    .build();

            duplicates.add(duplicate);
        }

        return duplicates;
    }

    // ============================================================================
    // DATA GENERATION UTILITIES
    // ============================================================================

    private static String generateHexident() {
        return String.format("%06X", random.nextInt(0xFFFFFF));
    }

    private static String generateCallsign() {
        return getRandomElement(AIRLINES) + String.format("%04d", random.nextInt(9999));
    }

    private static String generateMmsi() {
        return String.format("%09d", random.nextInt(100000000, 999999999));
    }

    private static String generateImo() {
        return String.format("%07d", random.nextInt(1000000, 9999999));
    }

    private static String generateVesselCallsign() {
        return String.format("%s%03d", getRandomElement(FLAGS), random.nextInt(999));
    }

    private static Double generateLatitude(boolean isVessel) {
        if (isVessel) {
            return ThreadLocalRandom.current().nextDouble(ASIA_LAT_MIN, ASIA_LAT_MAX);
        } else {
            return ThreadLocalRandom.current().nextDouble(VIETNAM_LAT_MIN, VIETNAM_LAT_MAX);
        }
    }

    private static Double generateLongitude(boolean isVessel) {
        if (isVessel) {
            return ThreadLocalRandom.current().nextDouble(ASIA_LON_MIN, ASIA_LON_MAX);
        } else {
            return ThreadLocalRandom.current().nextDouble(VIETNAM_LON_MIN, VIETNAM_LON_MAX);
        }
    }

    private static Integer generateAltitude() {
        return random.nextInt(1000, 45000); // 1,000 to 45,000 feet
    }

    private static Integer generateGroundSpeed() {
        return random.nextInt(0, 900); // 0 to 900 knots
    }

    private static Integer generateTrack() {
        return random.nextInt(0, 360); // 0 to 359 degrees
    }

    private static Integer generateVerticalRate() {
        return random.nextInt(-3000, 3000); // -3000 to +3000 feet per minute
    }

    private static String generateSquawk() {
        return String.format("%04d", random.nextInt(0, 7777));
    }

    private static Double generateVesselSpeed() {
        return random.nextDouble() * 30; // 0 to 30 knots
    }

    private static Integer generateCourse() {
        return random.nextInt(0, 360); // 0 to 359 degrees
    }

    private static Integer generateHeading() {
        return random.nextInt(0, 360); // 0 to 359 degrees
    }

    private static String generateNavigationStatus() {
        String[] statuses = {
                "Under way using engine", "At anchor", "Not under command", "Restricted manoeuvrability",
                "Constrained by her draught", "Moored", "Aground", "Engaged in fishing"
        };
        return getRandomElement(statuses);
    }

    private static Integer generateVesselLength() {
        return random.nextInt(50, 400); // 50 to 400 meters
    }

    private static Integer generateVesselWidth() {
        return random.nextInt(10, 60); // 10 to 60 meters
    }

    private static Double generateDraught() {
        return random.nextDouble() * 20; // 0 to 20 meters
    }

    private static String generateEta() {
        return LocalDateTime.now().plusDays(random.nextInt(1, 30)).toString();
    }

    private static Double generateDataQuality() {
        // Generate mostly good quality data with some variation
        double base = 0.7 + (random.nextDouble() * 0.3); // 0.7 to 1.0
        return Math.min(1.0, base);
    }

    private static <T> T getRandomElement(T[] array) {
        return array[random.nextInt(array.length)];
    }

    // ============================================================================
    // BULK DATA GENERATION FOR PERFORMANCE TESTING
    // ============================================================================

    /**
     * Generate large volume of aircraft data for performance testing
     */
    public static List<AircraftTrackingRequest> generateBulkAircraftData(int count) {
        List<AircraftTrackingRequest> bulkData = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            bulkData.add(generateAircraftTrackingRequest());

            // Log progress for large datasets
            if (i > 0 && i % 1000 == 0) {
                System.out.println("Generated " + i + " aircraft records...");
            }
        }

        return bulkData;
    }

    /**
     * Generate large volume of vessel data for performance testing
     */
    public static List<VesselTrackingRequest> generateBulkVesselData(int count) {
        List<VesselTrackingRequest> bulkData = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            bulkData.add(generateVesselTrackingRequest());

            // Log progress for large datasets
            if (i > 0 && i % 1000 == 0) {
                System.out.println("Generated " + i + " vessel records...");
            }
        }

        return bulkData;
    }

    /**
     * Generate test scenario data for specific testing needs
     */
    public static Map<String, Object> generateTestScenario(String scenarioType) {
        Map<String, Object> scenario = new HashMap<>();

        switch (scenarioType.toLowerCase()) {
            case "high_quality":
                scenario.put("aircraft", generateAircraftTrackingRequests(10));
                scenario.put("vessels", generateVesselTrackingRequests(10));
                scenario.put("description", "High quality data with all fields populated");
                break;

            case "poor_quality":
                List<AircraftTrackingRequest> poorAircraft = new ArrayList<>();
                List<VesselTrackingRequest> poorVessels = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    poorAircraft.add(generatePoorQualityAircraftData());
                    poorVessels.add(generatePoorQualityVesselData());
                }
                scenario.put("aircraft", poorAircraft);
                scenario.put("vessels", poorVessels);
                scenario.put("description", "Poor quality data with missing fields and low quality scores");
                break;

            case "mixed":
                List<AircraftTrackingRequest> mixedAircraft = new ArrayList<>();
                List<VesselTrackingRequest> mixedVessels = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    if (i < 7) {
                        mixedAircraft.add(generateAircraftTrackingRequest());
                        mixedVessels.add(generateVesselTrackingRequest());
                    } else {
                        mixedAircraft.add(generatePoorQualityAircraftData());
                        mixedVessels.add(generatePoorQualityVesselData());
                    }
                }
                scenario.put("aircraft", mixedAircraft);
                scenario.put("vessels", mixedVessels);
                scenario.put("description", "Mixed quality data - 70% good quality, 30% poor quality");
                break;

            default:
                scenario.put("aircraft", generateAircraftTrackingRequests(5));
                scenario.put("vessels", generateVesselTrackingRequests(5));
                scenario.put("description", "Default test scenario");
        }

        return scenario;
    }
}