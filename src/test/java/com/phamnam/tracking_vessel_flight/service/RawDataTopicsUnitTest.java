package com.phamnam.tracking_vessel_flight.service;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.raw.RawAircraftData;
import com.phamnam.tracking_vessel_flight.models.raw.RawVesselData;
import com.phamnam.tracking_vessel_flight.util.SampleDataGenerator;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Raw Data Topics Unit Test
 * 
 * Unit tests for the new raw data topics architecture components:
 * - Tests sample data generation
 * - Tests raw data model functionality
 * - Tests data quality validation
 * - Tests various data scenarios
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RawDataTopicsUnitTest {

    // ============================================================================
    // SAMPLE DATA GENERATION TESTS
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("Test aircraft tracking request generation")
    void testAircraftTrackingRequestGeneration() {
        // Test single aircraft generation
        AircraftTrackingRequest aircraft = SampleDataGenerator.generateAircraftTrackingRequest();

        assertNotNull(aircraft);
        assertNotNull(aircraft.getHexident());
        assertNotNull(aircraft.getLatitude());
        assertNotNull(aircraft.getLongitude());
        assertNotNull(aircraft.getDataQuality());
        assertEquals("test", aircraft.getSource());

        // Validate coordinate ranges
        assertTrue(aircraft.getLatitude() >= -90 && aircraft.getLatitude() <= 90);
        assertTrue(aircraft.getLongitude() >= -180 && aircraft.getLongitude() <= 180);
        assertTrue(aircraft.getDataQuality() >= 0 && aircraft.getDataQuality() <= 1);
    }

    @Test
    @Order(2)
    @DisplayName("Test vessel tracking request generation")
    void testVesselTrackingRequestGeneration() {
        // Test single vessel generation
        VesselTrackingRequest vessel = SampleDataGenerator.generateVesselTrackingRequest();

        assertNotNull(vessel);
        assertNotNull(vessel.getMmsi());
        assertNotNull(vessel.getLatitude());
        assertNotNull(vessel.getLongitude());
        assertNotNull(vessel.getDataQuality());
        assertEquals("test", vessel.getSource());

        // Validate MMSI format (9 digits)
        assertEquals(9, vessel.getMmsi().length());

        // Validate coordinate ranges
        assertTrue(vessel.getLatitude() >= -90 && vessel.getLatitude() <= 90);
        assertTrue(vessel.getLongitude() >= -180 && vessel.getLongitude() <= 180);
        assertTrue(vessel.getDataQuality() >= 0 && vessel.getDataQuality() <= 1);
    }

    @Test
    @Order(3)
    @DisplayName("Test bulk data generation")
    void testBulkDataGeneration() {
        // Test aircraft bulk generation
        List<AircraftTrackingRequest> aircraftList = SampleDataGenerator.generateAircraftTrackingRequests(10);
        assertEquals(10, aircraftList.size());

        // Verify all aircraft have unique hexidents
        long uniqueHexidents = aircraftList.stream()
                .map(AircraftTrackingRequest::getHexident)
                .distinct()
                .count();
        assertEquals(10, uniqueHexidents);

        // Test vessel bulk generation
        List<VesselTrackingRequest> vesselList = SampleDataGenerator.generateVesselTrackingRequests(5);
        assertEquals(5, vesselList.size());

        // Verify all vessels have unique MMSIs
        long uniqueMMSIs = vesselList.stream()
                .map(VesselTrackingRequest::getMmsi)
                .distinct()
                .count();
        assertEquals(5, uniqueMMSIs);
    }

    @Test
    @Order(4)
    @DisplayName("Test raw data generation by source")
    void testRawDataGenerationBySource() {
        // Test aircraft raw data by source
        Map<String, List<RawAircraftData>> aircraftBySource = SampleDataGenerator.generateRawAircraftDataBySource(3);

        assertEquals(2, aircraftBySource.size()); // flightradar24, adsbexchange
        assertTrue(aircraftBySource.containsKey("flightradar24"));
        assertTrue(aircraftBySource.containsKey("adsbexchange"));
        assertEquals(3, aircraftBySource.get("flightradar24").size());
        assertEquals(3, aircraftBySource.get("adsbexchange").size());

        // Test vessel raw data by source
        Map<String, List<RawVesselData>> vesselBySource = SampleDataGenerator.generateRawVesselDataBySource(2);

        assertEquals(4, vesselBySource.size()); // marinetraffic, vesselfinder, chinaports, marinetrafficv2
        assertTrue(vesselBySource.containsKey("marinetraffic"));
        assertTrue(vesselBySource.containsKey("vesselfinder"));
        assertTrue(vesselBySource.containsKey("chinaports"));
        assertTrue(vesselBySource.containsKey("marinetrafficv2"));

        vesselBySource.values().forEach(list -> assertEquals(2, list.size()));
    }

    // ============================================================================
    // RAW DATA MODEL TESTS
    // ============================================================================

    @Test
    @Order(5)
    @DisplayName("Test RawAircraftData model functionality")
    void testRawAircraftDataModel() {
        // Generate sample aircraft
        AircraftTrackingRequest aircraft = SampleDataGenerator.generateAircraftTrackingRequest();

        // Create raw aircraft data
        RawAircraftData rawData = RawAircraftData.fromSource(
                "flightradar24",
                "/api/aircraft/flightradar24",
                aircraft,
                1500L);

        assertNotNull(rawData);
        assertEquals("flightradar24", rawData.getSource());
        assertEquals("/api/aircraft/flightradar24", rawData.getApiEndpoint());
        assertEquals(1500L, rawData.getResponseTimeMs());
        assertNotNull(rawData.getFetchTime());
                assertNotNull(rawData.getRequestId());
        
        // Test processing status
        assertFalse(rawData.getProcessed());
        rawData.markAsProcessed();
        assertTrue(rawData.getProcessed());
        
        // Test data mapping
        assertEquals(aircraft.getHexident(), rawData.getHexident());
        assertEquals(aircraft.getLatitude(), rawData.getLatitude());
        assertEquals(aircraft.getLongitude(), rawData.getLongitude());
        assertEquals(aircraft.getDataQuality(), rawData.getDataQuality());
    }

    @Test
    @Order(6)
    @DisplayName("Test RawVesselData model functionality")
    void testRawVesselDataModel() {
        // Generate sample vessel
        VesselTrackingRequest vessel = SampleDataGenerator.generateVesselTrackingRequest();

        // Create raw vessel data
        RawVesselData rawData = RawVesselData.fromSource(
                "marinetraffic",
                "/api/vessels/marinetraffic",
                vessel,
                2000L);

        assertNotNull(rawData);
        assertEquals("marinetraffic", rawData.getSource());
        assertEquals("/api/vessels/marinetraffic", rawData.getApiEndpoint());
        assertEquals(2000L, rawData.getResponseTimeMs());
        assertNotNull(rawData.getFetchTime());
        assertNotNull(rawData.getRequestId());

        // Test validation methods
        assertTrue(rawData.hasValidMmsi());
                assertTrue(rawData.hasValidPosition());
        
        // Test processing status
        assertFalse(rawData.getProcessed());
        rawData.markAsProcessed();
        assertTrue(rawData.getProcessed());
        
        // Test data mapping
        assertEquals(vessel.getMmsi(), rawData.getMmsi());
        assertEquals(vessel.getLatitude(), rawData.getLatitude());
        assertEquals(vessel.getLongitude(), rawData.getLongitude());
        assertEquals(vessel.getDataQuality(), rawData.getDataQuality());
    }

    // ============================================================================
    // DATA QUALITY TESTS
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("Test poor quality data generation")
    void testPoorQualityDataGeneration() {
        // Test poor quality aircraft data
        AircraftTrackingRequest poorAircraft = SampleDataGenerator.generatePoorQualityAircraftData();

        assertNotNull(poorAircraft);
        assertNotNull(poorAircraft.getHexident());
        assertEquals("test-poor-quality", poorAircraft.getSource());
        assertTrue(poorAircraft.getDataQuality() < 0.6); // Should be low quality

        // Test poor quality vessel data
        VesselTrackingRequest poorVessel = SampleDataGenerator.generatePoorQualityVesselData();

        assertNotNull(poorVessel);
        assertNotNull(poorVessel.getMmsi());
        assertEquals("test-poor-quality", poorVessel.getSource());
        assertTrue(poorVessel.getDataQuality() < 0.5); // Should be low quality
    }

    @Test
    @Order(8)
    @DisplayName("Test duplicate data generation")
    void testDuplicateDataGeneration() {
        String testHexident = "ABC123";
        List<AircraftTrackingRequest> duplicates = SampleDataGenerator.generateDuplicateAircraftData(testHexident, 5);

        assertEquals(5, duplicates.size());

        // All should have the same hexident
        duplicates.forEach(aircraft -> assertEquals(testHexident, aircraft.getHexident()));

        // But should have slightly different positions and timestamps
        for (int i = 1; i < duplicates.size(); i++) {
            AircraftTrackingRequest prev = duplicates.get(i - 1);
            AircraftTrackingRequest curr = duplicates.get(i);

            // Should have different timestamps
            assertNotEquals(prev.getTimestamp(), curr.getTimestamp());

            // Positions should be close but slightly different
            double latDiff = Math.abs(prev.getLatitude() - curr.getLatitude());
            double lonDiff = Math.abs(prev.getLongitude() - curr.getLongitude());
            assertTrue(latDiff < 0.01); // Small difference
            assertTrue(lonDiff < 0.01); // Small difference
        }
    }

    // ============================================================================
    // TEST SCENARIO TESTS
    // ============================================================================

    @Test
    @Order(9)
    @DisplayName("Test different data scenarios")
    void testDataScenarios() {
        // Test high quality scenario
        Map<String, Object> highQualityScenario = SampleDataGenerator.generateTestScenario("high_quality");

        assertNotNull(highQualityScenario);
        assertEquals("High quality data with all fields populated", highQualityScenario.get("description"));

        @SuppressWarnings("unchecked")
        List<AircraftTrackingRequest> aircraft = (List<AircraftTrackingRequest>) highQualityScenario.get("aircraft");
        assertEquals(10, aircraft.size());

        @SuppressWarnings("unchecked")
        List<VesselTrackingRequest> vessels = (List<VesselTrackingRequest>) highQualityScenario.get("vessels");
        assertEquals(10, vessels.size());

        // Test poor quality scenario
        Map<String, Object> poorQualityScenario = SampleDataGenerator.generateTestScenario("poor_quality");

        assertNotNull(poorQualityScenario);
        assertEquals("Poor quality data with missing fields and low quality scores",
                poorQualityScenario.get("description"));

        @SuppressWarnings("unchecked")
        List<AircraftTrackingRequest> poorAircraft = (List<AircraftTrackingRequest>) poorQualityScenario
                .get("aircraft");
        assertEquals(5, poorAircraft.size());

        // Test mixed quality scenario
        Map<String, Object> mixedScenario = SampleDataGenerator.generateTestScenario("mixed");

        assertNotNull(mixedScenario);
        assertEquals("Mixed quality data - 70% good quality, 30% poor quality", mixedScenario.get("description"));

        @SuppressWarnings("unchecked")
        List<AircraftTrackingRequest> mixedAircraft = (List<AircraftTrackingRequest>) mixedScenario.get("aircraft");
        assertEquals(10, mixedAircraft.size());
    }

    // ============================================================================
    // PERFORMANCE AND BULK TESTS
    // ============================================================================

    @Test
    @Order(10)
    @DisplayName("Test bulk data generation performance")
    void testBulkDataGenerationPerformance() {
        long startTime = System.currentTimeMillis();

        // Generate a moderate amount of data
        List<AircraftTrackingRequest> bulkAircraft = SampleDataGenerator.generateBulkAircraftData(100);
        List<VesselTrackingRequest> bulkVessels = SampleDataGenerator.generateBulkVesselData(100);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertEquals(100, bulkAircraft.size());
        assertEquals(100, bulkVessels.size());

        // Should complete within reasonable time (adjust as needed)
        assertTrue(duration < 5000, "Bulk generation should complete within 5 seconds");

        System.out.println("Generated 200 records in " + duration + "ms");

        // Verify data quality
        long validAircraft = bulkAircraft.stream()
                .filter(a -> a.getHexident() != null && a.getLatitude() != null && a.getLongitude() != null)
                .count();
        assertEquals(100, validAircraft);

        long validVessels = bulkVessels.stream()
                .filter(v -> v.getMmsi() != null && v.getLatitude() != null && v.getLongitude() != null)
                .count();
        assertEquals(100, validVessels);
    }

    // ============================================================================
    // VALIDATION TESTS
    // ============================================================================

    @Test
    @Order(11)
    @DisplayName("Test data validation rules")
    void testDataValidationRules() {
        // Test aircraft with invalid coordinates
        AircraftTrackingRequest invalidAircraft = SampleDataGenerator.generateAircraftTrackingRequest();
        invalidAircraft.setLatitude(100.0); // Invalid latitude

        RawAircraftData invalidRawAircraft = RawAircraftData.fromSource("test", "/api/test", invalidAircraft, 1000L);

        // The raw data should still be created (validation happens in consumers)
        assertNotNull(invalidRawAircraft);
        assertEquals(100.0, invalidRawAircraft.getLatitude());

        // Test vessel with invalid MMSI
        VesselTrackingRequest invalidVessel = SampleDataGenerator.generateVesselTrackingRequest();
        invalidVessel.setMmsi("123"); // Too short MMSI

        RawVesselData invalidRawVessel = RawVesselData.fromSource("test", "/api/test", invalidVessel, 1000L);

        // Should create raw data but validation methods should detect issues
        assertNotNull(invalidRawVessel);
        assertFalse(invalidRawVessel.hasValidMmsi()); // Should detect invalid MMSI
    }

    @Test
    @Order(12)
    @DisplayName("Test edge cases and boundary conditions")
    void testEdgeCasesAndBoundaryConditions() {
        // Test with minimum valid coordinates
        AircraftTrackingRequest minAircraft = SampleDataGenerator.generateAircraftTrackingRequest();
        minAircraft.setLatitude(-90.0);
        minAircraft.setLongitude(-180.0);
        minAircraft.setAltitude(0);
        minAircraft.setGroundSpeed(0);

        RawAircraftData minRawAircraft = RawAircraftData.fromSource("test", "/api/test", minAircraft, 1L);
        assertNotNull(minRawAircraft);
        assertEquals(-90.0, minRawAircraft.getLatitude());
        assertEquals(-180.0, minRawAircraft.getLongitude());

        // Test with maximum valid coordinates
        AircraftTrackingRequest maxAircraft = SampleDataGenerator.generateAircraftTrackingRequest();
        maxAircraft.setLatitude(90.0);
        maxAircraft.setLongitude(180.0);
        maxAircraft.setAltitude(50000);
        maxAircraft.setGroundSpeed(1000);

        RawAircraftData maxRawAircraft = RawAircraftData.fromSource("test", "/api/test", maxAircraft, 10000L);
        assertNotNull(maxRawAircraft);
        assertEquals(90.0, maxRawAircraft.getLatitude());
        assertEquals(180.0, maxRawAircraft.getLongitude());

        // Test vessel with boundary speeds
        VesselTrackingRequest boundaryVessel = SampleDataGenerator.generateVesselTrackingRequest();
        boundaryVessel.setSpeed(0.0);
        boundaryVessel.setCourse(0);
        boundaryVessel.setHeading(359);

        RawVesselData boundaryRawVessel = RawVesselData.fromSource("test", "/api/test", boundaryVessel, 1000L);
        assertNotNull(boundaryRawVessel);
        assertEquals(0.0, boundaryRawVessel.getSpeed());
        assertEquals(0.0, boundaryRawVessel.getCourse());
        assertEquals(359.0, boundaryRawVessel.getHeading());
    }

    @Test
    @Order(13)
    @DisplayName("Test data consistency and uniqueness")
    void testDataConsistencyAndUniqueness() {
        // Generate multiple batches and verify uniqueness
        List<AircraftTrackingRequest> batch1 = SampleDataGenerator.generateAircraftTrackingRequests(50);
        List<AircraftTrackingRequest> batch2 = SampleDataGenerator.generateAircraftTrackingRequests(50);

        // Collect all hexidents
        List<String> allHexidents = batch1.stream()
                .map(AircraftTrackingRequest::getHexident)
                .collect(java.util.stream.Collectors.toList());
        allHexidents.addAll(batch2.stream()
                .map(AircraftTrackingRequest::getHexident)
                .collect(java.util.stream.Collectors.toList()));

        // Should have high uniqueness (at least 95% unique)
        long uniqueCount = allHexidents.stream().distinct().count();
        double uniquenessRate = (double) uniqueCount / allHexidents.size();
        assertTrue(uniquenessRate > 0.95, "Hexident uniqueness rate should be > 95%");

        // Similar test for vessels
        List<VesselTrackingRequest> vesselBatch1 = SampleDataGenerator.generateVesselTrackingRequests(30);
        List<VesselTrackingRequest> vesselBatch2 = SampleDataGenerator.generateVesselTrackingRequests(30);

        List<String> allMMSIs = vesselBatch1.stream()
                .map(VesselTrackingRequest::getMmsi)
                .collect(java.util.stream.Collectors.toList());
        allMMSIs.addAll(vesselBatch2.stream()
                .map(VesselTrackingRequest::getMmsi)
                .collect(java.util.stream.Collectors.toList()));

        long uniqueMMSICount = allMMSIs.stream().distinct().count();
        double mmsiUniquenessRate = (double) uniqueMMSICount / allMMSIs.size();
        assertTrue(mmsiUniquenessRate > 0.95, "MMSI uniqueness rate should be > 95%");
    }
}