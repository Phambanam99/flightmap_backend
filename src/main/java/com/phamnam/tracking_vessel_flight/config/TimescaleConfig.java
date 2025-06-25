package com.phamnam.tracking_vessel_flight.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

@Configuration
@Slf4j
@Getter
public class TimescaleConfig {

    @Value("${timescale.enabled:true}")
    private boolean enabled;

    @Value("${timescale.ship-tracking-table:ship_tracking}")
    private String shipTrackingTable;

    @Value("${timescale.aircraft-tracking-table:aircraft_tracking}")
    private String aircraftTrackingTable;

    @Value("${timescale.time-column:timestamp}")
    private String timeColumn;

    /**
     * Get the configured ship tracking table name
     */
    @Bean("shipTrackingTableName")
    public String getShipTrackingTableName() {
        log.info("🗄️ Using TimescaleDB ship tracking table: {}", shipTrackingTable);
        return shipTrackingTable;
    }

    /**
     * Get the configured aircraft tracking table name
     */
    @Bean("aircraftTrackingTableName")
    public String getAircraftTrackingTableName() {
        log.info("🗄️ Using TimescaleDB aircraft tracking table: {}", aircraftTrackingTable);
        return aircraftTrackingTable;
    }

    /**
     * Get the configured time column name
     */
    @Bean("timescaleTimeColumn")
    public String getTimeColumnName() {
        return timeColumn;
    }

    /**
     * Check if TimescaleDB is enabled
     */
    public boolean isTimescaleEnabled() {
        return enabled;
    }

    /**
     * Get table configuration for logging
     */
    public void logConfiguration() {
        log.info("📊 TimescaleDB Configuration:");
        log.info("  - Enabled: {}", enabled);
        log.info("  - Ship tracking table: {}", shipTrackingTable);
        log.info("  - Aircraft tracking table: {}", aircraftTrackingTable);
        log.info("  - Time column: {}", timeColumn);
    }
}