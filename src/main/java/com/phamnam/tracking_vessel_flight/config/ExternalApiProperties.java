package com.phamnam.tracking_vessel_flight.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external.api")
public class ExternalApiProperties {

    private boolean enabled = true;
    private FlightRadar24Config flightradar24 = new FlightRadar24Config();
    private MarineTrafficConfig marinetraffic = new MarineTrafficConfig();
    private BoundsConfig bounds = new BoundsConfig();
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    @Data
    public static class FlightRadar24Config {
        private boolean enabled = true;
        private String baseUrl = "https://data-live.flightradar24.com";
        private String apiKey = "";
        private int pollInterval = 30000;  // 30 seconds
        private int timeout = 10000;       // 10 seconds
        private int retryAttempts = 3;
    }

    @Data
    public static class MarineTrafficConfig {
        private boolean enabled = true;
        private String baseUrl = "https://services.marinetraffic.com";
        private String apiKey = "";
        private int pollInterval = 60000;  // 60 seconds
        private int timeout = 15000;       // 15 seconds
        private int retryAttempts = 3;
    }

    @Data
    public static class BoundsConfig {
        private double minLatitude = 8.5;   // Vietnam bounds
        private double maxLatitude = 23.5;
        private double minLongitude = 102.0;
        private double maxLongitude = 109.5;
    }

    @Data
    public static class CircuitBreakerConfig {
        private int failureThreshold = 5;
        private long timeout = 60000;
    }
} 