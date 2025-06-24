package com.phamnam.tracking_vessel_flight.controller;

import com.phamnam.tracking_vessel_flight.util.DatabaseChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DatabaseStatusController {

    private final DatabaseChecker databaseChecker;

    @GetMapping("/database-status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        log.info("🔍 Checking database status via API...");

        try {
            // Run the database check
            databaseChecker.checkDatabase();

            // Return status
            Map<String, Object> status = databaseChecker.getDatabaseStatus();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Database status check completed",
                    "data", status,
                    "timestamp", System.currentTimeMillis()));

        } catch (Exception e) {
            log.error("❌ Database status check failed: {}", e.getMessage(), e);

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Database status check failed",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()));
        }
    }

    @GetMapping("/vessel-processing-config")
    public ResponseEntity<Map<String, Object>> getVesselProcessingConfig() {
        try {
            // Check current configuration values
            Map<String, Object> config = Map.of(
                    "external_api_enabled", getConfigValue("external.api.enabled", "unknown"),
                    "marinetraffic_enabled", getConfigValue("external.api.marinetraffic.enabled", "unknown"),
                    "vesselfinder_enabled", getConfigValue("external.api.vesselfinder.enabled", "unknown"),
                    "chinaports_enabled", getConfigValue("external.api.chinaports.enabled", "unknown"),
                    "data_fusion_enabled", getConfigValue("data.fusion.enabled", "unknown"),
                    "raw_storage_enabled", getConfigValue("raw.data.storage.enabled", "unknown"),
                    "data_processing_note", "enable-persistence config uses default value 'true' if not specified");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vessel processing configuration",
                    "config", config,
                    "timestamp", System.currentTimeMillis()));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()));
        }
    }

    private String getConfigValue(String key, String defaultValue) {
        try {
            // This is a simple implementation - in real apps you'd inject Environment
            return System.getProperty(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}