package com.phamnam.tracking_vessel_flight.models.enums;

public enum AlertStatus {
    ACTIVE("Active", "Alert is currently active"),
    ACKNOWLEDGED("Acknowledged", "Alert has been acknowledged"),
    RESOLVED("Resolved", "Alert has been resolved"),
    DISMISSED("Dismissed", "Alert has been dismissed"),
    ESCALATED("Escalated", "Alert has been escalated"),
    EXPIRED("Expired", "Alert has expired"),
    SUPPRESSED("Suppressed", "Alert has been suppressed");

    private final String displayName;
    private final String description;

    AlertStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == ACTIVE || this == ACKNOWLEDGED || this == ESCALATED;
    }

    public boolean isClosed() {
        return this == RESOLVED || this == DISMISSED || this == EXPIRED;
    }

    public String getColor() {
        switch (this) {
            case ACTIVE:
                return "#dc3545"; // Red
            case ACKNOWLEDGED:
                return "#ffc107"; // Yellow
            case RESOLVED:
                return "#28a745"; // Green
            case DISMISSED:
                return "#6c757d"; // Gray
            case ESCALATED:
                return "#6f42c1"; // Purple
            case EXPIRED:
                return "#17a2b8"; // Cyan
            case SUPPRESSED:
                return "#343a40"; // Dark
            default:
                return "#6c757d"; // Gray
        }
    }
}