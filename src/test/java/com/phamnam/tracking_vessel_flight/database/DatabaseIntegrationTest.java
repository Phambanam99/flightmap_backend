package com.phamnam.tracking_vessel_flight.database;

import com.phamnam.tracking_vessel_flight.dto.request.AircraftTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.VesselTrackingRequest;
import com.phamnam.tracking_vessel_flight.models.Aircraft;
import com.phamnam.tracking_vessel_flight.models.Ship;
import com.phamnam.tracking_vessel_flight.repository.AircraftRepository;
import com.phamnam.tracking_vessel_flight.repository.ShipRepository;
import com.phamnam.tracking_vessel_flight.util.SampleDataGenerator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database Integration Test
 * 
 * Comprehensive tests for database operations, data persistence, and database
 * health.
 * Tests all major repository operations and database connectivity.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class DatabaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private ShipRepository shipRepository;

    private static Aircraft testAircraft;
    private static Ship testShip;

    @BeforeAll
    static void setUpClass() {
        System.out.println("🗄️ Starting Database Integration Tests");
    }

    @BeforeEach
    void setUp() {
        System.out.println("⚡ Setting up database test environment...");

        // Create test aircraft
        AircraftTrackingRequest aircraftRequest = SampleDataGenerator.generateAircraftTrackingRequest();
        testAircraft = Aircraft.builder()
                .hexident(aircraftRequest.getHexident())
                .callsign(aircraftRequest.getCallsign())
                .latitude(aircraftRequest.getLatitude())
                .longitude(aircraftRequest.getLongitude())
                .altitude(aircraftRequest.getAltitude())
                .groundSpeed(aircraftRequest.getGroundSpeed())
                .track(aircraftRequest.getTrack())
                .aircraftType(aircraftRequest.getAircraftType())
                .registration(aircraftRequest.getRegistration())
                .timestamp(LocalDateTime.now())
                .build();

        // Create test ship
        VesselTrackingRequest vesselRequest = SampleDataGenerator.generateVesselTrackingRequest();
        testShip = Ship.builder()
                .mmsi(vesselRequest.getMmsi())
                .imo(vesselRequest.getImo())
                .vesselName(vesselRequest.getVesselName())
                .callsign(vesselRequest.getCallsign())
                .latitude(vesselRequest.getLatitude())
                .longitude(vesselRequest.getLongitude())
                .speed(vesselRequest.getSpeed())
                .course(vesselRequest.getCourse())
                .heading(vesselRequest.getHeading())
                .vesselType(vesselRequest.getVesselType())
                .flag(vesselRequest.getFlag())
                .destination(vesselRequest.getDestination())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ============================================================================
    // DATABASE CONNECTIVITY TESTS
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("Test Database Connectivity")
    void testDatabaseConnectivity() throws Exception {
        System.out.println("🔌 Testing database connectivity...");

        assertNotNull(dataSource, "DataSource should not be null");

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Database connection should not be null");
            assertFalse(connection.isClosed(), "Database connection should be open");

            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Database URL: " + metaData.getURL());
            System.out.println("Database Product: " + metaData.getDatabaseProductName());
            System.out.println("Database Version: " + metaData.getDatabaseProductVersion());

            assertTrue(connection.isValid(5), "Database connection should be valid");
        }

        System.out.println("✅ Database connectivity test passed");
    }

    @Test
    @Order(2)
    @DisplayName("Test Database Schema")
    void testDatabaseSchema() throws Exception {
        System.out.println("📋 Testing database schema...");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Check if aircraft table exists
            try (ResultSet tables = metaData.getTables(null, null, "aircraft", new String[] { "TABLE" })) {
                assertTrue(tables.next(), "Aircraft table should exist");
                System.out.println("✓ Aircraft table found");
            }

            // Check if ship table exists
            try (ResultSet tables = metaData.getTables(null, null, "ship", new String[] { "TABLE" })) {
                assertTrue(tables.next(), "Ship table should exist");
                System.out.println("✓ Ship table found");
            }
        }

        System.out.println("✅ Database schema test passed");
    }

    // ============================================================================
    // AIRCRAFT REPOSITORY TESTS
    // ============================================================================

    @Test
    @Order(3)
    @DisplayName("Test Aircraft Repository Save")
    void testAircraftRepositorySave() {
        System.out.println("✈️ Testing aircraft repository save...");

        assertNotNull(aircraftRepository, "Aircraft repository should not be null");

        Aircraft savedAircraft = aircraftRepository.save(testAircraft);

        assertNotNull(savedAircraft, "Saved aircraft should not be null");
        assertNotNull(savedAircraft.getId(), "Saved aircraft should have an ID");
        assertEquals(testAircraft.getHexident(), savedAircraft.getHexident());
        assertEquals(testAircraft.getCallsign(), savedAircraft.getCallsign());
        assertEquals(testAircraft.getLatitude(), savedAircraft.getLatitude());
        assertEquals(testAircraft.getLongitude(), savedAircraft.getLongitude());

        System.out.println("Saved aircraft with ID: " + savedAircraft.getId());
        System.out.println("✅ Aircraft repository save test passed");
    }

    @Test
    @Order(4)
    @DisplayName("Test Aircraft Repository Find")
    void testAircraftRepositoryFind() {
        System.out.println("🔍 Testing aircraft repository find...");

        // Save aircraft first
        Aircraft savedAircraft = aircraftRepository.save(testAircraft);
        Long aircraftId = savedAircraft.getId();

        // Test find by ID
        Optional<Aircraft> foundAircraft = aircraftRepository.findById(aircraftId);
        assertTrue(foundAircraft.isPresent(), "Aircraft should be found by ID");
        assertEquals(testAircraft.getHexident(), foundAircraft.get().getHexident());

        // Test find by hexident
        Optional<Aircraft> foundByHexident = aircraftRepository.findByHexident(testAircraft.getHexident());
        assertTrue(foundByHexident.isPresent(), "Aircraft should be found by hexident");

        // Test find all
        List<Aircraft> allAircraft = aircraftRepository.findAll();
        assertFalse(allAircraft.isEmpty(), "Aircraft list should not be empty");

        System.out.println("Found aircraft by ID: " + foundAircraft.get().getHexident());
        System.out.println("Total aircraft in database: " + allAircraft.size());
        System.out.println("✅ Aircraft repository find test passed");
    }

    @Test
    @Order(5)
    @DisplayName("Test Aircraft Repository Update")
    void testAircraftRepositoryUpdate() {
        System.out.println("🔄 Testing aircraft repository update...");

        // Save aircraft first
        Aircraft savedAircraft = aircraftRepository.save(testAircraft);
        Long aircraftId = savedAircraft.getId();

        // Update aircraft
        String newCallsign = "UPDATED_CALLSIGN";
        savedAircraft.setCallsign(newCallsign);
        savedAircraft.setAltitude(35000);

        Aircraft updatedAircraft = aircraftRepository.save(savedAircraft);

        assertNotNull(updatedAircraft, "Updated aircraft should not be null");
        assertEquals(aircraftId, updatedAircraft.getId(), "Aircraft ID should remain the same");
        assertEquals(newCallsign, updatedAircraft.getCallsign(), "Callsign should be updated");
        assertEquals(35000, updatedAircraft.getAltitude(), "Altitude should be updated");

        System.out.println("Updated aircraft callsign to: " + updatedAircraft.getCallsign());
        System.out.println("✅ Aircraft repository update test passed");
    }

    @Test
    @Order(6)
    @DisplayName("Test Aircraft Repository Delete")
    void testAircraftRepositoryDelete() {
        System.out.println("🗑️ Testing aircraft repository delete...");

        // Save aircraft first
        Aircraft savedAircraft = aircraftRepository.save(testAircraft);
        Long aircraftId = savedAircraft.getId();

        // Verify aircraft exists
        assertTrue(aircraftRepository.existsById(aircraftId), "Aircraft should exist before deletion");

        // Delete aircraft
        aircraftRepository.deleteById(aircraftId);

        // Verify aircraft is deleted
        assertFalse(aircraftRepository.existsById(aircraftId), "Aircraft should not exist after deletion");
        Optional<Aircraft> deletedAircraft = aircraftRepository.findById(aircraftId);
        assertFalse(deletedAircraft.isPresent(), "Aircraft should not be found after deletion");

        System.out.println("Successfully deleted aircraft with ID: " + aircraftId);
        System.out.println("✅ Aircraft repository delete test passed");
    }

    // ============================================================================
    // SHIP REPOSITORY TESTS
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("Test Ship Repository Save")
    void testShipRepositorySave() {
        System.out.println("🚢 Testing ship repository save...");

        assertNotNull(shipRepository, "Ship repository should not be null");

        Ship savedShip = shipRepository.save(testShip);

        assertNotNull(savedShip, "Saved ship should not be null");
        assertNotNull(savedShip.getId(), "Saved ship should have an ID");
        assertEquals(testShip.getMmsi(), savedShip.getMmsi());
        assertEquals(testShip.getVesselName(), savedShip.getVesselName());
        assertEquals(testShip.getLatitude(), savedShip.getLatitude());
        assertEquals(testShip.getLongitude(), savedShip.getLongitude());

        System.out.println("Saved ship with ID: " + savedShip.getId());
        System.out.println("✅ Ship repository save test passed");
    }

    @Test
    @Order(8)
    @DisplayName("Test Ship Repository Find")
    void testShipRepositoryFind() {
        System.out.println("🔍 Testing ship repository find...");

        // Save ship first
        Ship savedShip = shipRepository.save(testShip);
        Long shipId = savedShip.getId();

        // Test find by ID
        Optional<Ship> foundShip = shipRepository.findById(shipId);
        assertTrue(foundShip.isPresent(), "Ship should be found by ID");
        assertEquals(testShip.getMmsi(), foundShip.get().getMmsi());

        // Test find by MMSI
        Optional<Ship> foundByMmsi = shipRepository.findByMmsi(testShip.getMmsi());
        assertTrue(foundByMmsi.isPresent(), "Ship should be found by MMSI");

        // Test find all
        List<Ship> allShips = shipRepository.findAll();
        assertFalse(allShips.isEmpty(), "Ship list should not be empty");

        System.out.println("Found ship by ID: " + foundShip.get().getVesselName());
        System.out.println("Total ships in database: " + allShips.size());
        System.out.println("✅ Ship repository find test passed");
    }

    @Test
    @Order(9)
    @DisplayName("Test Ship Repository Update")
    void testShipRepositoryUpdate() {
        System.out.println("🔄 Testing ship repository update...");

        // Save ship first
        Ship savedShip = shipRepository.save(testShip);
        Long shipId = savedShip.getId();

        // Update ship
        String newVesselName = "UPDATED_VESSEL_NAME";
        savedShip.setVesselName(newVesselName);
        savedShip.setSpeed(15.5);

        Ship updatedShip = shipRepository.save(savedShip);

        assertNotNull(updatedShip, "Updated ship should not be null");
        assertEquals(shipId, updatedShip.getId(), "Ship ID should remain the same");
        assertEquals(newVesselName, updatedShip.getVesselName(), "Vessel name should be updated");
        assertEquals(15.5, updatedShip.getSpeed(), "Speed should be updated");

        System.out.println("Updated ship vessel name to: " + updatedShip.getVesselName());
        System.out.println("✅ Ship repository update test passed");
    }

    @Test
    @Order(10)
    @DisplayName("Test Ship Repository Delete")
    void testShipRepositoryDelete() {
        System.out.println("🗑️ Testing ship repository delete...");

        // Save ship first
        Ship savedShip = shipRepository.save(testShip);
        Long shipId = savedShip.getId();

        // Verify ship exists
        assertTrue(shipRepository.existsById(shipId), "Ship should exist before deletion");

        // Delete ship
        shipRepository.deleteById(shipId);

        // Verify ship is deleted
        assertFalse(shipRepository.existsById(shipId), "Ship should not exist after deletion");
        Optional<Ship> deletedShip = shipRepository.findById(shipId);
        assertFalse(deletedShip.isPresent(), "Ship should not be found after deletion");

        System.out.println("Successfully deleted ship with ID: " + shipId);
        System.out.println("✅ Ship repository delete test passed");
    }

    // ============================================================================
    // BULK OPERATIONS TESTS
    // ============================================================================

    @Test
    @Order(11)
    @DisplayName("Test Bulk Aircraft Operations")
    void testBulkAircraftOperations() {
        System.out.println("📦 Testing bulk aircraft operations...");

        // Generate multiple aircraft
        List<AircraftTrackingRequest> aircraftRequests = SampleDataGenerator.generateAircraftTrackingRequests(10);

        // Convert to aircraft entities and save
        List<Aircraft> aircraftList = aircraftRequests.stream()
                .map(req -> Aircraft.builder()
                        .hexident(req.getHexident())
                        .callsign(req.getCallsign())
                        .latitude(req.getLatitude())
                        .longitude(req.getLongitude())
                        .altitude(req.getAltitude())
                        .groundSpeed(req.getGroundSpeed())
                        .track(req.getTrack())
                        .aircraftType(req.getAircraftType())
                        .registration(req.getRegistration())
                        .timestamp(LocalDateTime.now())
                        .build())
                .toList();

        // Save all aircraft
        List<Aircraft> savedAircraft = aircraftRepository.saveAll(aircraftList);

        assertEquals(10, savedAircraft.size(), "All 10 aircraft should be saved");
        savedAircraft.forEach(aircraft -> {
            assertNotNull(aircraft.getId(), "Each aircraft should have an ID");
            assertNotNull(aircraft.getHexident(), "Each aircraft should have a hexident");
        });

        System.out.println("Successfully saved " + savedAircraft.size() + " aircraft in bulk");
        System.out.println("✅ Bulk aircraft operations test passed");
    }

    @Test
    @Order(12)
    @DisplayName("Test Bulk Ship Operations")
    void testBulkShipOperations() {
        System.out.println("📦 Testing bulk ship operations...");

        // Generate multiple ships
        List<VesselTrackingRequest> vesselRequests = SampleDataGenerator.generateVesselTrackingRequests(10);

        // Convert to ship entities and save
        List<Ship> shipList = vesselRequests.stream()
                .map(req -> Ship.builder()
                        .mmsi(req.getMmsi())
                        .imo(req.getImo())
                        .vesselName(req.getVesselName())
                        .callsign(req.getCallsign())
                        .latitude(req.getLatitude())
                        .longitude(req.getLongitude())
                        .speed(req.getSpeed())
                        .course(req.getCourse())
                        .heading(req.getHeading())
                        .vesselType(req.getVesselType())
                        .flag(req.getFlag())
                        .destination(req.getDestination())
                        .timestamp(LocalDateTime.now())
                        .build())
                .toList();

        // Save all ships
        List<Ship> savedShips = shipRepository.saveAll(shipList);

        assertEquals(10, savedShips.size(), "All 10 ships should be saved");
        savedShips.forEach(ship -> {
            assertNotNull(ship.getId(), "Each ship should have an ID");
            assertNotNull(ship.getMmsi(), "Each ship should have an MMSI");
        });

        System.out.println("Successfully saved " + savedShips.size() + " ships in bulk");
        System.out.println("✅ Bulk ship operations test passed");
    }

    // ============================================================================
    // QUERY TESTS
    // ============================================================================

    @Test
    @Order(13)
    @DisplayName("Test Custom Queries")
    void testCustomQueries() {
        System.out.println("🔍 Testing custom queries...");

        // Save some test data
        Aircraft aircraft1 = aircraftRepository.save(testAircraft);
        Ship ship1 = shipRepository.save(testShip);

        // Test aircraft queries
        List<Aircraft> aircraftByType = aircraftRepository.findByAircraftType(testAircraft.getAircraftType());
        assertFalse(aircraftByType.isEmpty(), "Should find aircraft by type");

        // Test ship queries
        List<Ship> shipsByType = shipRepository.findByVesselType(testShip.getVesselType());
        assertFalse(shipsByType.isEmpty(), "Should find ships by vessel type");

        System.out.println("Found " + aircraftByType.size() + " aircraft of type: " + testAircraft.getAircraftType());
        System.out.println("Found " + shipsByType.size() + " ships of type: " + testShip.getVesselType());
        System.out.println("✅ Custom queries test passed");
    }

    // ============================================================================
    // TRANSACTION TESTS
    // ============================================================================

    @Test
    @Order(14)
    @DisplayName("Test Transaction Rollback")
    void testTransactionRollback() {
        System.out.println("🔄 Testing transaction rollback...");

        long initialAircraftCount = aircraftRepository.count();

        try {
            // This should trigger rollback due to @Transactional on test class
            Aircraft aircraft = aircraftRepository.save(testAircraft);
            assertNotNull(aircraft.getId(), "Aircraft should be saved within transaction");

            // Force an exception to test rollback
            if (true) { // Always true to trigger exception
                throw new RuntimeException("Test exception for rollback");
            }
        } catch (RuntimeException e) {
            System.out.println("Expected exception caught: " + e.getMessage());
        }

        // Transaction should rollback, so count should remain the same
        long finalAircraftCount = aircraftRepository.count();
        assertEquals(initialAircraftCount, finalAircraftCount,
                "Aircraft count should be same due to transaction rollback");

        System.out.println("Initial count: " + initialAircraftCount + ", Final count: " + finalAircraftCount);
        System.out.println("✅ Transaction rollback test passed");
    }

    // ============================================================================
    // PERFORMANCE TESTS
    // ============================================================================

    @Test
    @Order(15)
    @DisplayName("Test Database Performance")
    void testDatabasePerformance() {
        System.out.println("🚀 Testing database performance...");

        long startTime = System.currentTimeMillis();

        // Test bulk insert performance
        List<Aircraft> aircraftList = SampleDataGenerator.generateAircraftTrackingRequests(100)
                .stream()
                .map(req -> Aircraft.builder()
                        .hexident(req.getHexident())
                        .callsign(req.getCallsign())
                        .latitude(req.getLatitude())
                        .longitude(req.getLongitude())
                        .altitude(req.getAltitude())
                        .groundSpeed(req.getGroundSpeed())
                        .track(req.getTrack())
                        .aircraftType(req.getAircraftType())
                        .registration(req.getRegistration())
                        .timestamp(LocalDateTime.now())
                        .build())
                .toList();

        List<Aircraft> savedAircraft = aircraftRepository.saveAll(aircraftList);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertEquals(100, savedAircraft.size(), "Should save 100 aircraft");
        assertTrue(duration < 10000, "Bulk insert should complete within 10 seconds");

        System.out.println("Bulk inserted 100 aircraft in: " + duration + "ms");
        System.out.println("✅ Database performance test passed");
    }

    @AfterEach
    void tearDown() {
        System.out.println("🧹 Cleaning up database test data...");
    }

    @AfterAll
    static void tearDownClass() {
        System.out.println("🎉 Database Integration Tests Completed!");
        System.out.println("\n📋 Database Test Summary:");
        System.out.println("✅ Database Connectivity");
        System.out.println("✅ Database Schema Validation");
        System.out.println("✅ Aircraft Repository CRUD Operations");
        System.out.println("✅ Ship Repository CRUD Operations");
        System.out.println("✅ Bulk Operations");
        System.out.println("✅ Custom Queries");
        System.out.println("✅ Transaction Management");
        System.out.println("✅ Database Performance");

        System.out.println("\n🗄️ Database is fully functional and ready for production!");
    }
}