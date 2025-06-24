package com.phamnam.tracking_vessel_flight.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseChecker implements CommandLineRunner {

    @Autowired
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "check-db".equals(args[0])) {
            checkDatabase();
        }
    }

    public void checkDatabase() {
        log.info("🔍 Starting database check...");

        try {
            // 1. Check database connection
            String currentTime = jdbcTemplate.queryForObject("SELECT NOW()", String.class);
            log.info("✅ Database connected successfully. Current time: {}", currentTime);

            // 2. Check if vessel-related tables exist
            checkTablesExist();

            // 3. Check record counts
            checkRecordCounts();

            // 4. Check recent data
            checkRecentData();

            // 5. Check raw data sources
            checkRawDataSources();

        } catch (Exception e) {
            log.error("❌ Database check failed: {}", e.getMessage(), e);
        }
    }

    private void checkTablesExist() {
        try {
            List<String> tables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables " +
                            "WHERE table_schema = 'public' AND (table_name LIKE '%ship%' OR table_name LIKE '%vessel%') "
                            +
                            "ORDER BY table_name",
                    String.class);

            log.info("📋 Vessel-related tables found: {}", tables);
        } catch (Exception e) {
            log.error("❌ Failed to check tables: {}", e.getMessage());
        }
    }

    private void checkRecordCounts() {
        try {
            // Check Ship table
            try {
                Integer shipCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ship", Integer.class);
                log.info("🚢 Ship table count: {}", shipCount);
            } catch (Exception e) {
                log.warn("⚠️ Ship table not accessible: {}", e.getMessage());
            }

            // Check ShipTracking table
            try {
                Integer trackingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ship_tracking",
                        Integer.class);
                log.info("🛤️ ShipTracking table count: {}", trackingCount);
            } catch (Exception e) {
                log.warn("⚠️ ShipTracking table not accessible: {}", e.getMessage());
            }

            // Check RawVesselData table
            try {
                Integer rawCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM raw_vessel_data", Integer.class);
                log.info("📊 RawVesselData table count: {}", rawCount);
            } catch (Exception e) {
                log.warn("⚠️ RawVesselData table not accessible: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Failed to check record counts: {}", e.getMessage());
        }
    }

    private void checkRecentData() {
        try {
            // Check recent ship tracking data (last hour)
            try {
                Integer recentCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM ship_tracking WHERE timestamp > NOW() - INTERVAL '1 hour'",
                        Integer.class);
                log.info("🕐 Recent tracking records (last hour): {}", recentCount);
            } catch (Exception e) {
                log.warn("⚠️ Cannot check recent tracking data: {}", e.getMessage());
            }

            // Check latest ship records
            try {
                List<Map<String, Object>> recentShips = jdbcTemplate.queryForList(
                        "SELECT mmsi, name, last_seen FROM ship ORDER BY created_at DESC LIMIT 5");
                log.info("🚢 Recent ships: {}", recentShips);
            } catch (Exception e) {
                log.warn("⚠️ Cannot check recent ships: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Failed to check recent data: {}", e.getMessage());
        }
    }

    private void checkRawDataSources() {
        try {
            List<Map<String, Object>> sources = jdbcTemplate.queryForList(
                    "SELECT data_source, COUNT(*) as count, MAX(received_at) as latest_data " +
                            "FROM raw_vessel_data GROUP BY data_source ORDER BY latest_data DESC");

            log.info("📡 Raw data sources: {}", sources);

        } catch (Exception e) {
            log.warn("⚠️ Cannot check raw data sources (table may not exist): {}", e.getMessage());
        }
    }

    // Manual method to call from controller or service
    public Map<String, Object> getDatabaseStatus() {
        try {
            Integer shipCount = safeCount("ship");
            Integer trackingCount = safeCount("ship_tracking");
            Integer rawCount = safeCount("raw_vessel_data");
            Integer recentCount = safeCount("ship_tracking", "WHERE timestamp > NOW() - INTERVAL '1 hour'");

            return Map.of(
                    "database_connected", true,
                    "ship_count", shipCount,
                    "tracking_count", trackingCount,
                    "raw_data_count", rawCount,
                    "recent_tracking_count", recentCount,
                    "check_time", System.currentTimeMillis());
        } catch (Exception e) {
            return Map.of(
                    "database_connected", false,
                    "error", e.getMessage(),
                    "check_time", System.currentTimeMillis());
        }
    }

    private Integer safeCount(String tableName) {
        return safeCount(tableName, "");
    }

    private Integer safeCount(String tableName, String whereClause) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tableName + " " + whereClause,
                    Integer.class);
        } catch (Exception e) {
            log.debug("Cannot count table {}: {}", tableName, e.getMessage());
            return 0;
        }
    }
}