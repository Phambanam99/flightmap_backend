package com.phamnam.tracking_vessel_flight.models;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "alert_rule")
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlertRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    @Lob
    private String description;

    @Column(name = "rule_type")
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;

    @Column(name = "entity_type")
    @Enumerated(EnumType.STRING)
    private EntityType entityType; // AIRCRAFT, VESSEL, BOTH

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    // Geographic constraints
    @Column(columnDefinition = "geometry")
    private Geometry geofence; // Geographic area for the rule

    @Column(name = "min_latitude")
    private Double minLatitude;

    @Column(name = "max_latitude")
    private Double maxLatitude;

    @Column(name = "min_longitude")
    private Double minLongitude;

    @Column(name = "max_longitude")
    private Double maxLongitude;

    // Movement constraints
    @Column(name = "min_speed")
    private Double minSpeed;

    @Column(name = "max_speed")
    private Double maxSpeed;

    @Column(name = "min_altitude")
    private Double minAltitude;

    @Column(name = "max_altitude")
    private Double maxAltitude;

    // Time constraints
    @Column(name = "time_window_start")
    private java.time.LocalTime timeWindowStart;

    @Column(name = "time_window_end")
    private java.time.LocalTime timeWindowEnd;

    @Column(name = "active_days")
    private String activeDays; // JSON array of days [1,2,3,4,5,6,7]

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    // Condition parameters
    @Column(name = "condition_parameters", columnDefinition = "TEXT")
    private String conditionParameters; // JSON configuration

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Column(name = "duration_seconds")
    private Integer durationSeconds; // How long condition must persist

    @Column(name = "cooldown_seconds")
    @Builder.Default
    private Integer cooldownSeconds = 300; // Cooldown between alerts

    // Action configuration
    @Column(name = "notification_channels", columnDefinition = "TEXT")
    private String notificationChannels; // JSON array of channels

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "email_recipients", columnDefinition = "TEXT")
    private String emailRecipients; // JSON array of emails

    @Column(name = "sms_recipients", columnDefinition = "TEXT")
    private String smsRecipients; // JSON array of phone numbers

    // Filtering criteria
    @Column(name = "aircraft_types", columnDefinition = "TEXT")
    private String aircraftTypes; // JSON array of aircraft types

    @Column(name = "vessel_types", columnDefinition = "TEXT")
    private String vesselTypes; // JSON array of vessel types

    @Column(name = "operators", columnDefinition = "TEXT")
    private String operators; // JSON array of operators

    @Column(name = "flags", columnDefinition = "TEXT")
    private String flags; // JSON array of flag states

    @Column(name = "exclude_military")
    @Builder.Default
    private Boolean excludeMilitary = false;

    @Column(name = "exclude_government")
    @Builder.Default
    private Boolean excludeGovernment = false;

    // Statistics
    @Column(name = "triggered_count")
    @Builder.Default
    private Long triggeredCount = 0L;

    @Column(name = "last_triggered")
    private LocalDateTime lastTriggered;

    @Column(name = "false_positive_count")
    @Builder.Default
    private Long falsePositiveCount = 0L;

    @Column(name = "acknowledged_count")
    @Builder.Default
    private Long acknowledgedCount = 0L;

    @OneToMany(mappedBy = "alertRule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AlertEvent> alertEvents;

    public enum RuleType {
        GEOFENCE_ENTRY, // Entity enters geofenced area
        GEOFENCE_EXIT, // Entity exits geofenced area
        SPEED_THRESHOLD, // Speed above/below threshold
        ALTITUDE_THRESHOLD, // Altitude above/below threshold
        EMERGENCY_SQUAWK, // Emergency transponder code
        COURSE_DEVIATION, // Deviation from expected course
        PROXIMITY_ALERT, // Two entities too close
        DATA_LOSS, // Lost contact with entity
        UNUSUAL_PATTERN, // Unusual movement pattern
        RESTRICTED_AREA, // Entry into restricted area
        NAVIGATION_STATUS, // Specific navigation status
        CARGO_ALERT, // Dangerous cargo related
        WEATHER_ALERT, // Weather related constraints
        CUSTOM_CONDITION // Custom rule logic
    }

    public enum EntityType {
        AIRCRAFT,
        VESSEL,
        BOTH
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    // Helper methods
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();

        // Check if rule is enabled
        if (!isEnabled)
            return false;

        // Check validity period
        if (validFrom != null && now.isBefore(validFrom))
            return false;
        if (validUntil != null && now.isAfter(validUntil))
            return false;

        // Check time window (if specified)
        if (timeWindowStart != null && timeWindowEnd != null) {
            java.time.LocalTime currentTime = now.toLocalTime();
            if (timeWindowStart.isAfter(timeWindowEnd)) {
                // Crosses midnight
                return currentTime.isAfter(timeWindowStart) || currentTime.isBefore(timeWindowEnd);
            } else {
                // Same day
                return currentTime.isAfter(timeWindowStart) && currentTime.isBefore(timeWindowEnd);
            }
        }

        return true;
    }

    public boolean isInCooldown() {
        if (lastTriggered == null || cooldownSeconds == null)
            return false;
        return lastTriggered.plusSeconds(cooldownSeconds).isAfter(LocalDateTime.now());
    }

    public void incrementTriggeredCount() {
        this.triggeredCount++;
        this.lastTriggered = LocalDateTime.now();
    }

    public double getFalsePositiveRate() {
        if (triggeredCount == 0)
            return 0.0;
        return (double) falsePositiveCount / triggeredCount * 100;
    }

    public double getAcknowledgmentRate() {
        if (triggeredCount == 0)
            return 0.0;
        return (double) acknowledgedCount / triggeredCount * 100;
    }
}