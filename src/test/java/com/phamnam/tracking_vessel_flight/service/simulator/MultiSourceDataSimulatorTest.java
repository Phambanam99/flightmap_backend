package com.phamnam.tracking_vessel_flight.service.simulator;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-Source Data Simulator Test
 * 
 * Comprehensive tests for the MultiSourceDataSimulator to ensure:
 * 1. Proper data generation for all sources
 * 2. Source-specific data characteristics
 * 3. Data quality variations
 * 4. Error simulation capabilities
 * 5. Performance under load
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MultiSourceDataSimulatorTest {

    @Autowired
    private MultiSourceDataSimulator dataSimulator;

    @BeforeAll
    static void setUpClass() {
        System.out.println("🧪 Starting Multi-Source Data Simulator Tests");
    }

    @BeforeEach
    void setUp() {
        System.out.println("⚡ Setting up test environment...");
    }

    // ============================================================================
    // AIRCRAFT DATA SIMULATION TESTS
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("Test FlightRadar24 Data Simulation")
    void testFlightRadar24DataSimulation() throws Exception {
        System.out.println("🛩️ Testing FlightRadar24 data simulation...");

        CompletableFuture<List<AircraftTrackingRequest>> future = dataSimulator.simulateFlightRadar24Data();
        List<AircraftTrackingRequest> aircraft = future.get();

        assertNotNull(aircraft, "Aircraft data should not be null");
        assertFalse(aircraft.isEmpty(), "Aircraft data should not be empty");

        // Verify FlightRadar24 characteristics (high quality)
        aircraft.forEach(ac -> {
            assertNotNull(ac.getHexident(), "Hexident should not be null");
            assertNotNull(ac.getLatitude(), "Latitude should not be null");
            assertNotNull(ac.getLongitude(), "Longitude should not be null");
            assertEquals("flightradar24", ac.getSource(), "Source should be flightradar24");
            assertTrue(ac.getDataQuality() >= 0.85,
                    "FlightRadar24 should have high quality data (>= 0.85)");
        });

        System.out.println("✅ FlightRadar24 simulation test passed: " + aircraft.size() + " aircraft");
    }

    @Test
    @Order(2)
    @DisplayName("Test ADS-B Exchange Data Simulation")
    void testAdsbExchangeDataSimulation() throws Exception {
        System.out.println("📡 Testing ADS-B Exchange data simulation...");

        CompletableFuture<List<AircraftTrackingRequest>> future = dataSimulator.simulateAdsbExchangeData();
        List<AircraftTrackingRequest> aircraft = future.get();

        assertNotNull(aircraft, "Aircraft data should not be null");
        assertFalse(aircraft.isEmpty(), "Aircraft data should not be empty");

        // Verify ADS-B Exchange characteristics (technical focus)
        aircraft.forEach(ac -> {
            assertNotNull(ac.getHexident(), "Hexident should not be null");
            assertEquals("adsbexchange", ac.getSource(), "Source should be adsbexchange");
            assertTrue(ac.getDataQuality() >= 0.75,
                    "ADS-B Exchange should have good quality data (>= 0.75)");
            // Technical data should include vertical rate and squawk
            if (ac.getVerticalRate() != null) {
                assertTrue(Math.abs(ac.getVerticalRate()) <= 3000,
                        "Vertical rate should be realistic");
            }
        });

        System.out.println("✅ ADS-B Exchange simulation test passed: " + aircraft.size() + " aircraft");
    }

    // ============================================================================
    // VESSEL DATA SIMULATION TESTS
    // ============================================================================

    @Test
    @Order(3)
    @DisplayName("Test MarineTraffic Data Simulation")
    void testMarineTrafficDataSimulation() throws Exception {
        System.out.println("🚢 Testing MarineTraffic data simulation...");

        CompletableFuture<List<VesselTrackingRequest>> future = dataSimulator.simulateMarineTrafficData();
        List<VesselTrackingRequest> vessels = future.get();

        assertNotNull(vessels, "Vessel data should not be null");
        assertFalse(vessels.isEmpty(), "Vessel data should not be empty");

        // Verify MarineTraffic characteristics (commercial vessels)
        vessels.forEach(vessel -> {
            assertNotNull(vessel.getMmsi(), "MMSI should not be null");
            assertNotNull(vessel.getLatitude(), "Latitude should not be null");
            assertNotNull(vessel.getLongitude(), "Longitude should not be null");
            assertEquals("marinetraffic", vessel.getSource(), "Source should be marinetraffic");
            assertTrue(vessel.getDataQuality() >= 0.80,
                    "MarineTraffic should have high quality data (>= 0.80)");

            // Should focus on commercial vessel types
            if (vessel.getVesselType() != null) {
                assertTrue(vessel.getVesselType().contains("Ship") ||
                        vessel.getVesselType().contains("Carrier") ||
                        vessel.getVesselType().contains("Tanker"),
                        "Should generate commercial vessel types");
            }
        });

        System.out.println("✅ MarineTraffic simulation test passed: " + vessels.size() + " vessels");
    }

    @Test
    @Order(4)
    @DisplayName("Test VesselFinder Data Simulation")
    void testVesselFinderDataSimulation() throws Exception {
        System.out.println("🔍 Testing VesselFinder data simulation...");

        CompletableFuture<List<VesselTrackingRequest>> future = dataSimulator.simulateVesselFinderData();
        List<VesselTrackingRequest> vessels = future.get();

        assertNotNull(vessels, "Vessel data should not be null");

        // VesselFinder may have varied quality, some missing data
        vessels.forEach(vessel -> {
            assertNotNull(vessel.getMmsi(), "MMSI should not be null");
            assertEquals("vesselfinder", vessel.getSource(), "Source should be vesselfinder");
            assertTrue(vessel.getDataQuality() >= 0.60,
                    "VesselFinder should have at least medium quality data (>= 0.60)");
        });

        System.out.println("✅ VesselFinder simulation test passed: " + vessels.size() + " vessels");
    }

    @Test
    @Order(5)
    @DisplayName("Test Chinaports Data Simulation")
    void testChinaportsDataSimulation() throws Exception {
        System.out.println("🇨🇳 Testing Chinaports data simulation...");

        CompletableFuture<List<VesselTrackingRequest>> future = dataSimulator.simulateChinaportsData();
        List<VesselTrackingRequest> vessels = future.get();

        assertNotNull(vessels, "Vessel data should not be null");

        // Verify Chinese regional characteristics
        vessels.forEach(vessel -> {
            assertNotNull(vessel.getMmsi(), "MMSI should not be null");
            assertEquals("chinaports", vessel.getSource(), "Source should be chinaports");
            assertTrue(vessel.getDataQuality() >= 0.70,
                    "Chinaports should have good quality data (>= 0.70)");

            // Should have Chinese region coordinates
            if (vessel.getLatitude() != null) {
                assertTrue(vessel.getLatitude() >= 18.0 && vessel.getLatitude() <= 41.0,
                        "Should be in Chinese region latitude range");
            }
            if (vessel.getLongitude() != null) {
                assertTrue(vessel.getLongitude() >= 108.0 && vessel.getLongitude() <= 126.0,
                        "Should be in Chinese region longitude range");
            }
        });

        System.out.println("✅ Chinaports simulation test passed: " + vessels.size() + " vessels");
    }

    @Test
    @Order(6)
    @DisplayName("Test MarineTrafficV2 Data Simulation")
    void testMarineTrafficV2DataSimulation() throws Exception {
        System.out.println("🚢🔄 Testing MarineTrafficV2 data simulation...");

        CompletableFuture<List<VesselTrackingRequest>> future = dataSimulator.simulateMarineTrafficV2Data();
        List<VesselTrackingRequest> vessels = future.get();

        assertNotNull(vessels, "Vessel data should not be null");
        assertFalse(vessels.isEmpty(), "Vessel data should not be empty");

        // Verify enhanced API characteristics (highest quality)
        vessels.forEach(vessel -> {
            assertNotNull(vessel.getMmsi(), "MMSI should not be null");
            assertEquals("marinetrafficv2", vessel.getSource(), "Source should be marinetrafficv2");
            assertTrue(vessel.getDataQuality() >= 0.90,
                    "MarineTrafficV2 should have very high quality data (>= 0.90)");
        });

        System.out.println("✅ MarineTrafficV2 simulation test passed: " + vessels.size() + " vessels");
    }

    // ============================================================================
    // ERROR SIMULATION TESTS
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("Test Network Error Simulation")
    void testNetworkErrorSimulation() {
        System.out.println("🚨 Testing network error simulation...");

        // Test aircraft network error
        CompletableFuture<List<AircraftTrackingRequest>> aircraftErrorFuture = dataSimulator
                .simulateAircraftNetworkError("test-source");

        assertThrows(Exception.class, aircraftErrorFuture::join,
                "Should throw exception for aircraft network error");

        // Test vessel network error
        CompletableFuture<List<VesselTrackingRequest>> vesselErrorFuture = dataSimulator
                .simulateVesselNetworkError("test-source");

        assertThrows(Exception.class, vesselErrorFuture::join,
                "Should throw exception for vessel network error");

        System.out.println("✅ Network error simulation test passed");
    }

    @Test
    @Order(8)
    @DisplayName("Test Empty Response Simulation")
    void testEmptyResponseSimulation() throws Exception {
        System.out.println("📭 Testing empty response simulation...");

        CompletableFuture<List<Object>> emptyFuture = dataSimulator.simulateEmptyResponse("test-source");
        List<Object> emptyResult = emptyFuture.get();

        assertNotNull(emptyResult, "Empty response should not be null");
        assertTrue(emptyResult.isEmpty(), "Response should be empty");

        System.out.println("✅ Empty response simulation test passed");
    }

    // ============================================================================
    // CONFIGURATION AND STATUS TESTS
    // ============================================================================

    @Test
    @Order(9)
    @DisplayName("Test Simulator Status")
    void testSimulatorStatus() {
        System.out.println("📊 Testing simulator status...");

        Map<String, Object> status = dataSimulator.getSimulatorStatus();

        assertNotNull(status, "Status should not be null");
        assertTrue(status.containsKey("enabled"), "Status should contain 'enabled' field");
        assertTrue(status.containsKey("aircraftRange"), "Status should contain aircraft range");
        assertTrue(status.containsKey("vesselRange"), "Status should contain vessel range");
        assertTrue(status.containsKey("supportedSources"), "Status should contain supported sources");

        @SuppressWarnings("unchecked")
        Map<String, Object> supportedSources = (Map<String, Object>) status.get("supportedSources");
        assertNotNull(supportedSources, "Supported sources should not be null");
        assertTrue(supportedSources.containsKey("aircraft"), "Should support aircraft sources");
        assertTrue(supportedSources.containsKey("vessel"), "Should support vessel sources");

        System.out.println("✅ Simulator status test passed");
    }

    @Test
    @Order(10)
    @DisplayName("Test Comprehensive Test Data Generation")
    void testComprehensiveTestDataGeneration() {
        System.out.println("🎯 Testing comprehensive test data generation...");

        Map<String, Object> testData = dataSimulator.generateComprehensiveTestData();

        assertNotNull(testData, "Test data should not be null");
        assertFalse(testData.containsKey("error"), "Should not contain error");
        assertTrue(testData.containsKey("aircraft"), "Should contain aircraft data");
        assertTrue(testData.containsKey("vessel"), "Should contain vessel data");
        assertTrue(testData.containsKey("totalRecords"), "Should contain total records count");

        @SuppressWarnings("unchecked")
        Map<String, Object> aircraftData = (Map<String, Object>) testData.get("aircraft");
        @SuppressWarnings("unchecked")
        Map<String, Object> vesselData = (Map<String, Object>) testData.get("vessel");

        assertTrue(aircraftData.containsKey("flightradar24"), "Should contain FlightRadar24 data");
        assertTrue(aircraftData.containsKey("adsbexchange"), "Should contain ADS-B Exchange data");

        assertTrue(vesselData.containsKey("marinetraffic"), "Should contain MarineTraffic data");
        assertTrue(vesselData.containsKey("vesselfinder"), "Should contain VesselFinder data");
        assertTrue(vesselData.containsKey("chinaports"), "Should contain Chinaports data");
        assertTrue(vesselData.containsKey("marinetrafficv2"), "Should contain MarineTrafficV2 data");

        System.out.println("✅ Comprehensive test data generation passed: " +
                testData.get("totalRecords") + " total records");
    }

    // ============================================================================
    // PERFORMANCE TESTS
    // ============================================================================

    @Test
    @Order(11)
    @DisplayName("Test Concurrent Data Generation")
    void testConcurrentDataGeneration() throws Exception {
        System.out.println("⚡ Testing concurrent data generation...");

        long startTime = System.currentTimeMillis();

        // Generate data from all sources concurrently
        CompletableFuture<List<AircraftTrackingRequest>> flightRadar24Future = dataSimulator
                .simulateFlightRadar24Data();
        CompletableFuture<List<AircraftTrackingRequest>> adsbExchangeFuture = dataSimulator.simulateAdsbExchangeData();
        CompletableFuture<List<VesselTrackingRequest>> marineTrafficFuture = dataSimulator.simulateMarineTrafficData();
        CompletableFuture<List<VesselTrackingRequest>> vesselFinderFuture = dataSimulator.simulateVesselFinderData();
        CompletableFuture<List<VesselTrackingRequest>> chinaportsFuture = dataSimulator.simulateChinaportsData();
        CompletableFuture<List<VesselTrackingRequest>> marineTrafficV2Future = dataSimulator
                .simulateMarineTrafficV2Data();

        // Wait for all to complete
        CompletableFuture.allOf(
                flightRadar24Future, adsbExchangeFuture, marineTrafficFuture,
                vesselFinderFuture, chinaportsFuture, marineTrafficV2Future).get();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Verify all completed successfully
        List<AircraftTrackingRequest> flightRadar24Data = flightRadar24Future.get();
        List<AircraftTrackingRequest> adsbExchangeData = adsbExchangeFuture.get();
        List<VesselTrackingRequest> marineTrafficData = marineTrafficFuture.get();
        List<VesselTrackingRequest> vesselFinderData = vesselFinderFuture.get();
        List<VesselTrackingRequest> chinaportsData = chinaportsFuture.get();
        List<VesselTrackingRequest> marineTrafficV2Data = marineTrafficV2Future.get();

        int totalRecords = flightRadar24Data.size() + adsbExchangeData.size() +
                marineTrafficData.size() + vesselFinderData.size() +
                chinaportsData.size() + marineTrafficV2Data.size();

        assertTrue(totalRecords > 0, "Should generate some records");
        assertTrue(duration < 10000, "Should complete within 10 seconds");

        System.out.println("✅ Concurrent generation test passed: " + totalRecords +
                " records in " + duration + "ms");
    }

    @AfterEach
    void tearDown() {
        System.out.println("🧹 Cleaning up test environment...");
    }

    @AfterAll
    static void tearDownClass() {
        System.out.println("🏁 Multi-Source Data Simulator Tests Completed");
        System.out.println("=".repeat(50));
    }
}
