package com.phamnam.tracking_vessel_flight.models.enums;

public enum SourceStatus {
    HEALTHY("Healthy", "Source is operating normally"),
    WARNING("Warning", "Source has minor issues"),
    ERROR("Error", "Source has errors"),
    TIMEOUT("Timeout", "Source response timeout"),
    RATE_LIMITED("Rate Limited", "Source is rate limiting requests"),
    UNAUTHORIZED("Unauthorized", "Authentication failed"),
    FORBIDDEN("Forbidden", "Access denied"),
    NOT_FOUND("Not Found", "Source endpoint not found"),
    MAINTENANCE("Maintenance", "Source is under maintenance"),
    DISABLED("Disabled", "Source is disabled"),
    UNKNOWN("Unknown", "Source status unknown");

    private final String displayName;
    private final String description;

    SourceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHealthy() {
        return this == HEALTHY;
    }

    public boolean isError() {
        return this == ERROR || this == TIMEOUT || this == UNAUTHORIZED ||
                this == FORBIDDEN || this == NOT_FOUND;
    }

    public boolean isOperational() {
        return this == HEALTHY || this == WARNING;
    }

    public int getSeverityLevel() {
        switch (this) {
            case HEALTHY:
                return 0;
            case WARNING:
                return 1;
            case RATE_LIMITED:
                return 2;
            case TIMEOUT:
                return 3;
            case MAINTENANCE:
                return 4;
            case DISABLED:
                return 5;
            case NOT_FOUND:
                return 6;
            case FORBIDDEN:
                return 7;
            case UNAUTHORIZED:
                return 8;
            case ERROR:
                return 9;
            case UNKNOWN:
                return 10;
            default:
                return 10;
        }
    }
}