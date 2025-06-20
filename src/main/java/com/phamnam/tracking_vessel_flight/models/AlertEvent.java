package com.phamnam.tracking_vessel_flight.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.phamnam.tracking_vessel_flight.config.serializer.PointSerializer;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_event", indexes = {
        @Index(name = "idx_alert_event_timestamp", columnList = "event_time"),
        @Index(name = "idx_alert_event_entity", columnList = "entity_type,entity_id"),
        @Index(name = "idx_alert_event_status", columnList = "status"),
        @Index(name = "idx_alert_event_priority", columnList = "priority")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlertEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    @JsonBackReference
    private AlertRule alertRule;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TrackingPoint.EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId; // hexident for aircraft, mmsi for vessel

    @Column(name = "entity_name")
    private String entityName; // callsign or ship name

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private AlertRule.Priority priority;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AlertStatus status = AlertStatus.ACTIVE;

    // Location where alert was triggered
    @Column(columnDefinition = "geometry(Point, 4326)")
    @JsonSerialize(using = PointSerializer.class)
    private Point location;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "altitude")
    private Double altitude;

    // Alert details
    @Column(name = "alert_message", nullable = false)
    @Lob
    private String alertMessage;

    @Column(name = "alert_details", columnDefinition = "TEXT")
    private String alertDetails; // JSON with additional details

    @Column(name = "trigger_value")
    private Double triggerValue; // The value that triggered the alert

    @Column(name = "threshold_value")
    private Double thresholdValue; // The threshold that was exceeded

    // Context information
    @Column(name = "speed")
    private Double speed;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "course")
    private Double course;

    @Column(name = "vertical_speed")
    private Double verticalSpeed;

    @Column(name = "navigation_status")
    private String navigationStatus;

    @Column(name = "flight_phase")
    private String flightPhase;

    @Column(name = "weather_conditions")
    private String weatherConditions;

    // Alert handling
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy; // User ID or system

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolution_notes")
    @Lob
    private String resolutionNotes;

    @Column(name = "false_positive")
    private Boolean falsePositive = false;

    @Column(name = "escalated")
    private Boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalated_to")
    private String escalatedTo;

    // Notification tracking
    @Column(name = "notifications_sent", columnDefinition = "TEXT")
    private String notificationsSent; // JSON array of sent notifications

    @Column(name = "first_notification_sent")
    private LocalDateTime firstNotificationSent;

    @Column(name = "last_notification_sent")
    private LocalDateTime lastNotificationSent;

    @Column(name = "notification_count")
    private Integer notificationCount = 0;

    // Related alerts
    @Column(name = "parent_alert_id")
    private Long parentAlertId; // For grouped or cascaded alerts

    @Column(name = "related_alerts", columnDefinition = "TEXT")
    private String relatedAlerts; // JSON array of related alert IDs

    // Metrics
    @Column(name = "response_time_seconds")
    private Long responseTimeSeconds; // Time from creation to acknowledgment

    @Column(name = "resolution_time_seconds")
    private Long resolutionTimeSeconds; // Time from creation to resolution

    @Column(name = "severity_score")
    private Double severityScore; // Calculated severity score

    // Additional context
    @Column(name = "operator")
    private String operator; // Aircraft operator or ship operator

    @Column(name = "flag_state")
    private String flagState; // For vessels

    @Column(name = "aircraft_type")
    private String aircraftType;

    @Column(name = "vessel_type")
    private String vesselType;

    @Column(name = "data_source")
    private String dataSource;

    public enum AlertStatus {
        ACTIVE,
        ACKNOWLEDGED,
        RESOLVED,
        ESCALATED,
        SUPPRESSED,
        EXPIRED
    }

    @JsonProperty("alertRuleId")
    public Long getAlertRuleId() {
        return alertRule != null ? alertRule.getId() : null;
    }

    @JsonProperty("alertRuleName")
    public String getAlertRuleName() {
        return alertRule != null ? alertRule.getName() : null;
    }

    @PrePersist
    @PreUpdate
    public void updateCoordinates() {
        if (location != null) {
            this.latitude = location.getY();
            this.longitude = location.getX();
        }
    }

    // Helper methods
    public boolean isActive() {
        return AlertStatus.ACTIVE.equals(status);
    }

    public boolean isAcknowledged() {
        return acknowledgedAt != null;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }

    public void acknowledge(String userId) {
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgedBy = userId;
        if (this.status == AlertStatus.ACTIVE) {
            this.status = AlertStatus.ACKNOWLEDGED;
        }
        calculateResponseTime();
    }

    public void resolve(String userId, String notes) {
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = userId;
        this.resolutionNotes = notes;
        this.status = AlertStatus.RESOLVED;
        calculateResolutionTime();
    }

    public void escalate(String escalateTo) {
        this.escalated = true;
        this.escalatedAt = LocalDateTime.now();
        this.escalatedTo = escalateTo;
        this.status = AlertStatus.ESCALATED;
    }

    public void markAsFalsePositive(String userId) {
        this.falsePositive = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = userId;
        this.status = AlertStatus.RESOLVED;
        this.resolutionNotes = "Marked as false positive";
        calculateResolutionTime();
    }

    private void calculateResponseTime() {
        if (acknowledgedAt != null && eventTime != null) {
            this.responseTimeSeconds = java.time.Duration.between(eventTime, acknowledgedAt).getSeconds();
        }
    }

    private void calculateResolutionTime() {
        if (resolvedAt != null && eventTime != null) {
            this.resolutionTimeSeconds = java.time.Duration.between(eventTime, resolvedAt).getSeconds();
        }
    }

    public void incrementNotificationCount() {
        this.notificationCount++;
        this.lastNotificationSent = LocalDateTime.now();
        if (this.firstNotificationSent == null) {
            this.firstNotificationSent = LocalDateTime.now();
        }
    }
}