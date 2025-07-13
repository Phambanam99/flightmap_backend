package com.phamnam.tracking_vessel_flight.models.enums;

public enum AlertPriority {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4),
    EMERGENCY("Emergency", 5);

    private final String displayName;
    private final int severityLevel;

    AlertPriority(String displayName, int severityLevel) {
        this.displayName = displayName;
        this.severityLevel = severityLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSeverityLevel() {
        return severityLevel;
    }

    public boolean isHighPriority() {
        return this == HIGH || this == CRITICAL || this == EMERGENCY;
    }

    public boolean isCritical() {
        return this == CRITICAL || this == EMERGENCY;
    }

    public String getColor() {
        switch (this) {
            case LOW:
                return "#28a745"; // Green
            case MEDIUM:
                return "#ffc107"; // Yellow
            case HIGH:
                return "#fd7e14"; // Orange
            case CRITICAL:
                return "#dc3545"; // Red
            case EMERGENCY:
                return "#6f42c1"; // Purple
            default:
                return "#6c757d"; // Gray
        }
    }
}