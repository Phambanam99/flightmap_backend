package com.phamnam.tracking_vessel_flight.models;

import com.phamnam.tracking_vessel_flight.models.enums.DataSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "data_source")
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DataSource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // FlightRadar24, MarineTraffic, ADS-B Exchange, etc.

    @Column(name = "display_name")
    private String displayName; // Human-readable name

    @Column(name = "source_type")
    @Enumerated(EnumType.STRING)
    private DataSourceType sourceType; // AIRCRAFT, VESSEL, BOTH

    @Column(name = "api_url")
    private String apiUrl; // Base API URL

    @Column(name = "api_key")
    private String apiKey; // API key (encrypted)

    @Column(name = "poll_interval")
    private Integer pollInterval; // Polling interval in seconds

    @Column(name = "timeout")
    private Integer timeout; // Request timeout in milliseconds

    @Column(name = "retry_attempts")
    @Builder.Default
    private Integer retryAttempts = 3;

    @Column(name = "circuit_breaker_threshold")
    @Builder.Default
    private Integer circuitBreakerThreshold = 5; // Failure threshold

    @Column(name = "circuit_breaker_timeout")
    @Builder.Default
    private Long circuitBreakerTimeout = 60000L; // Circuit breaker timeout in ms

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Status and monitoring
    @Column(name = "last_poll_time")
    private LocalDateTime lastPollTime;

    @Column(name = "last_success_time")
    private LocalDateTime lastSuccessTime;

    @Column(name = "last_error_time")
    private LocalDateTime lastErrorTime;

    @Column(name = "last_error_message")
    @Lob
    private String lastErrorMessage;

    @Column(name = "consecutive_failures")
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @Column(name = "total_requests")
    @Builder.Default
    private Long totalRequests = 0L;

    @Column(name = "successful_requests")
    @Builder.Default
    private Long successfulRequests = 0L;

    @Column(name = "failed_requests")
    @Builder.Default
    private Long failedRequests = 0L;

    @Column(name = "average_response_time")
    @Builder.Default
    private Double averageResponseTime = 0.0; // in milliseconds

    // Data quality metrics
    @Column(name = "data_points_received")
    @Builder.Default
    private Long dataPointsReceived = 0L;

    @Column(name = "duplicate_data_points")
    @Builder.Default
    private Long duplicateDataPoints = 0L;

    @Column(name = "invalid_data_points")
    @Builder.Default
    private Long invalidDataPoints = 0L;

    @Column(name = "data_freshness_threshold")
    @Builder.Default
    private Integer dataFreshnessThreshold = 300; // seconds

    // Geographic coverage
    @Column(name = "coverage_area")
    private String coverageArea; // Geographic area description

    @Column(name = "min_latitude")
    private Double minLatitude;

    @Column(name = "max_latitude")
    private Double maxLatitude;

    @Column(name = "min_longitude")
    private Double minLongitude;

    @Column(name = "max_longitude")
    private Double maxLongitude;

    // Rate limiting
    @Column(name = "rate_limit_requests_per_minute")
    private Integer rateLimitRequestsPerMinute;

    @Column(name = "rate_limit_requests_per_hour")
    private Integer rateLimitRequestsPerHour;

    @Column(name = "rate_limit_requests_per_day")
    private Integer rateLimitRequestsPerDay;

    // Cost and billing
    @Column(name = "cost_per_request")
    @Builder.Default
    private Double costPerRequest = 0.0;

    @Column(name = "monthly_quota")
    @Builder.Default
    private Long monthlyQuota = 0L;

    @Column(name = "current_month_usage")
    @Builder.Default
    private Long currentMonthUsage = 0L;

    // Configuration
    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration; // JSON configuration

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers; // JSON headers for requests

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 1; // Source priority (1 = highest)

    @Column(name = "weight")
    @Builder.Default
    private Double weight = 1.0; // Weight for data aggregation

    @OneToMany(mappedBy = "dataSource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DataSourceStatus> statusHistory;

    // Helper methods
    public void incrementTotalRequests() {
        this.totalRequests++;
    }

    public void incrementSuccessfulRequests() {
        this.successfulRequests++;
        this.consecutiveFailures = 0;
        this.lastSuccessTime = LocalDateTime.now();
    }

    public void incrementFailedRequests() {
        this.failedRequests++;
        this.consecutiveFailures++;
        this.lastErrorTime = LocalDateTime.now();
    }

    public void updateResponseTime(double responseTime) {
        if (this.totalRequests > 0) {
            this.averageResponseTime = ((this.averageResponseTime * (this.totalRequests - 1)) + responseTime)
                    / this.totalRequests;
        } else {
            this.averageResponseTime = responseTime;
        }
    }

    public double getSuccessRate() {
        if (totalRequests == 0)
            return 0.0;
        return (double) successfulRequests / totalRequests * 100;
    }

    public boolean isHealthy() {
        return isEnabled && isActive && consecutiveFailures < circuitBreakerThreshold;
    }

    public boolean isWithinRateLimit() {
        // Implementation would check current usage against rate limits
        return true; // Simplified for now
    }
}