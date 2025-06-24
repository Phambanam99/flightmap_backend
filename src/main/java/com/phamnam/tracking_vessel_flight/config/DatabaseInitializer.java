package com.phamnam.tracking_vessel_flight.config;

import com.phamnam.tracking_vessel_flight.models.User;
import com.phamnam.tracking_vessel_flight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Value("${admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${timescale.enabled:true}")
    private boolean timescaleEnabled;

    @Override
    public void run(String... args) throws Exception {
        // Check if admin user already exists
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            // Create admin user
            User adminUser = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(adminEmail)
                    .role("ADMIN")
                    .build();

            userRepository.save(adminUser);

//            System.out.println("Admin user created successfully!");
        } else {
//            System.out.println("Admin user already exists, skipping initialization.");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDatabase() {
        if (!timescaleEnabled) {
            log.info("TimescaleDB initialization is disabled");
            return;
        }

        log.info("Starting TimescaleDB database initialization...");

        try {
            // Check if TimescaleDB is available
            if (!isTimescaleDbAvailable()) {
                log.warn("TimescaleDB extension is not available, skipping TimescaleDB setup");
                return;
            }

            // Execute initialization scripts in order
            List<String> scripts = Arrays.asList(
                    "db/01-init-extensions.sql",
                    "db/02-timescale-optimization.sql",
                    "db/03-create-hypertables.sql",
                    "db/04-utility-functions.sql");

            for (String scriptPath : scripts) {
                executeScript(scriptPath);
            }

            // Verify setup
            verifyTimescaleSetup();

            log.info("TimescaleDB database initialization completed successfully");

        } catch (Exception e) {
            log.error("Failed to initialize TimescaleDB database", e);
            // Don't throw exception to prevent application startup failure
            // TimescaleDB features will just be unavailable
        }
    }

    private boolean isTimescaleDbAvailable() {
        try {
            String result = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_extension WHERE extname = 'timescaledb'",
                    String.class);
            return "1".equals(result);
        } catch (Exception e) {
            log.warn("Could not check TimescaleDB availability", e);
            return false;
        }
    }

    private void executeScript(String scriptPath) {
        try {
            log.info("Executing script: {}", scriptPath);

            ClassPathResource resource = new ClassPathResource(scriptPath);
            if (!resource.exists()) {
                log.warn("Script not found: {}", scriptPath);
                return;
            }

            String script = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Execute the entire script as one statement to handle PostgreSQL functions
            // properly
            try {
                jdbcTemplate.execute(script);
                log.info("Successfully executed script: {}", scriptPath);
            } catch (Exception e) {
                log.warn("Failed to execute script {}: {}", scriptPath, e.getMessage());

                // Fallback: try to split and execute individual statements
                // but handle PostgreSQL function definitions properly
                executeScriptWithStatementSplitting(script, scriptPath);
            }

        } catch (IOException e) {
            log.error("Failed to read script file: {}", scriptPath, e);
        } catch (Exception e) {
            log.error("Failed to execute script: {}", scriptPath, e);
        }
    }

    private void executeScriptWithStatementSplitting(String script, String scriptPath) {
        // Remove comments and normalize whitespace
        String cleanedScript = script
                .replaceAll("--[^\r\n]*", "") // Remove line comments
                .replaceAll("/\\*[\\s\\S]*?\\*/", "") // Remove block comments
                .replaceAll("\\s+", " ") // Normalize whitespace
                .trim();

        StringBuilder currentStatement = new StringBuilder();
        boolean inFunction = false;
        boolean inDoBlock = false;
        int dollarTagDepth = 0;

        String[] lines = cleanedScript.split(";");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                continue;
            }

            currentStatement.append(line);

            // Check for function/procedure/do block start
            if (line.toUpperCase().contains("CREATE OR REPLACE FUNCTION") ||
                    line.toUpperCase().contains("CREATE FUNCTION") ||
                    line.toUpperCase().contains("CREATE OR REPLACE PROCEDURE") ||
                    line.toUpperCase().contains("CREATE PROCEDURE")) {
                inFunction = true;
            }

            if (line.toUpperCase().startsWith("DO $$") || line.toUpperCase().contains("DO $$")) {
                inDoBlock = true;
                dollarTagDepth = 1;
            }

            // Count dollar tags for nested blocks
            if (inFunction || inDoBlock) {
                dollarTagDepth += countOccurrences(line, "$$");
            }

            // Check for end of function/procedure/do block
            if ((inFunction && line.toUpperCase().contains("$$ LANGUAGE")) ||
                    (inDoBlock && line.endsWith("$$") && dollarTagDepth % 2 == 0)) {
                inFunction = false;
                inDoBlock = false;
                dollarTagDepth = 0;

                // Execute the complete function/do block
                executeStatement(currentStatement.toString(), scriptPath);
                currentStatement.setLength(0);
            } else if (!inFunction && !inDoBlock) {
                // Execute regular statement
                executeStatement(currentStatement.toString(), scriptPath);
                currentStatement.setLength(0);
            } else {
                // Continue building the function/do block
                currentStatement.append(";");
            }
        }

        // Execute any remaining statement
        if (currentStatement.length() > 0) {
            executeStatement(currentStatement.toString(), scriptPath);
        }
    }

    private void executeStatement(String statement, String scriptPath) {
        String trimmedStatement = statement.trim();
        if (!trimmedStatement.isEmpty() && !trimmedStatement.startsWith("--")) {
            try {
                jdbcTemplate.execute(trimmedStatement);
            } catch (Exception e) {
                log.warn("Failed to execute statement in {}: {} - Statement: {}",
                        scriptPath, e.getMessage(),
                        trimmedStatement.substring(0, Math.min(100, trimmedStatement.length())));
            }
        }
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    private void verifyTimescaleSetup() {
        try {
            // Check if hypertables were created
            List<String> hypertables = jdbcTemplate.queryForList(
                    "SELECT hypertable_name FROM timescaledb_information.hypertables WHERE hypertable_schema = 'public'",
                    String.class);

            if (!hypertables.isEmpty()) {
                log.info("TimescaleDB hypertables created: {}", hypertables);

                // Check compression and retention policies
                for (String hypertable : hypertables) {
                    checkTablePolicies(hypertable);
                }
            } else {
                log.warn("No TimescaleDB hypertables found - tables may not exist yet");
            }

            // Check if utility functions were created
            checkUtilityFunctions();

            // Check continuous aggregates
            checkContinuousAggregates();

        } catch (Exception e) {
            log.warn("Could not verify TimescaleDB setup completely", e);
        }
    }

    private void checkTablePolicies(String tableName) {
        try {
            // Check compression policy
            String compressionPolicy = jdbcTemplate.queryForObject(
                    "SELECT COALESCE((SELECT 'Enabled' FROM timescaledb_information.jobs j " +
                            "JOIN timescaledb_information.job_stats js ON j.job_id = js.job_id " +
                            "WHERE j.proc_name = 'policy_compression' AND j.config->>'hypertable_name' = ?), 'Not set')",
                    String.class, tableName);

            // Check retention policy
            String retentionPolicy = jdbcTemplate.queryForObject(
                    "SELECT COALESCE((SELECT 'Enabled' FROM timescaledb_information.jobs j " +
                            "WHERE j.proc_name = 'policy_retention' AND j.config->>'hypertable_name' = ?), 'Not set')",
                    String.class, tableName);

            log.info("Table {} - Compression: {}, Retention: {}", tableName, compressionPolicy, retentionPolicy);

        } catch (Exception e) {
            log.debug("Could not check policies for table {}: {}", tableName, e.getMessage());
        }
    }

    private void checkUtilityFunctions() {
        try {
            List<String> functions = jdbcTemplate.queryForList(
                    "SELECT routine_name FROM information_schema.routines " +
                            "WHERE routine_schema = 'public' AND routine_type = 'FUNCTION' " +
                            "AND routine_name IN ('get_aircraft_trail', 'get_vessel_trail', 'get_performance_metrics', 'check_system_health')",
                    String.class);

            if (!functions.isEmpty()) {
                log.info("Utility functions available: {}", functions);
            } else {
                log.warn("No utility functions found");
            }

        } catch (Exception e) {
            log.debug("Could not check utility functions: {}", e.getMessage());
        }
    }

    private void checkContinuousAggregates() {
        try {
            List<String> aggregates = jdbcTemplate.queryForList(
                    "SELECT view_name FROM timescaledb_information.continuous_aggregates",
                    String.class);

            if (!aggregates.isEmpty()) {
                log.info("Continuous aggregates created: {}", aggregates);
            } else {
                log.info("No continuous aggregates found (will be created when data is available)");
            }

        } catch (Exception e) {
            log.debug("Could not check continuous aggregates: {}", e.getMessage());
        }
    }

    // Manual setup method for development/testing
    public void manualSetup() {
        log.info("Starting manual TimescaleDB setup...");
        initializeDatabase();
    }

    // Method to check system health
    public void checkHealth() {
        try {
            List<String> healthResults = jdbcTemplate.queryForList(
                    "SELECT component || ': ' || status || ' - ' || details FROM check_system_health()",
                    String.class);

            log.info("System Health Check:");
            healthResults.forEach(result -> log.info("  {}", result));

        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }

    // Method to get performance metrics
    public void showPerformanceMetrics() {
        try {
            log.info("Performance Metrics:");

            jdbcTemplate.query(
                    "SELECT table_name, total_size, total_rows, recent_rows, compressed_chunks, total_chunks " +
                            "FROM get_performance_metrics()",
                    rs -> {
                        log.info("  Table: {} | Size: {} | Rows: {} | Recent: {} | Chunks: {}/{} compressed",
                                rs.getString("table_name"),
                                rs.getString("total_size"),
                                rs.getLong("total_rows"),
                                rs.getLong("recent_rows"),
                                rs.getLong("compressed_chunks"),
                                rs.getLong("total_chunks"));
                    });

        } catch (Exception e) {
            log.error("Failed to get performance metrics", e);
        }
    }
}