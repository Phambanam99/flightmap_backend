package com.phamnam.tracking_vessel_flight.models.enums;

public enum RuleType {
    EMERGENCY_SQUAWK("Emergency Squawk", "Detects emergency squawk codes (7500, 7600, 7700)"),
    ALTITUDE_VIOLATION("Altitude Violation", "Detects altitude violations"),
    SPEED_VIOLATION("Speed Violation", "Detects speed violations"),
    GEOGRAPHIC_BOUNDARY("Geographic Boundary", "Detects boundary violations"),
    SUDDEN_ALTITUDE_CHANGE("Sudden Altitude Change", "Detects rapid altitude changes"),
    LOST_CONTACT("Lost Contact", "Detects when entity stops reporting"),
    PROXIMITY_ALERT("Proximity Alert", "Detects when entities are too close"),
    DEVIATION_FROM_ROUTE("Route Deviation", "Detects route deviations"),
    SECURITY_ALERT("Security Alert", "Security-related alerts"),
    DANGEROUS_CARGO("Dangerous Cargo", "Dangerous cargo alerts"),
    WEATHER_ALERT("Weather Alert", "Weather-related alerts"),
    MAINTENANCE_ALERT("Maintenance Alert", "Maintenance-related alerts"),
    FUEL_ALERT("Fuel Alert", "Fuel-related alerts"),
    COMMUNICATION_FAILURE("Communication Failure", "Communication failures"),
    SYSTEM_MALFUNCTION("System Malfunction", "System malfunctions"),
    CUSTOM("Custom", "Custom rule type");

    private final String displayName;
    private final String description;

    RuleType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAircraftApplicable() {
        return this == EMERGENCY_SQUAWK || this == ALTITUDE_VIOLATION ||
                this == SPEED_VIOLATION || this == GEOGRAPHIC_BOUNDARY ||
                this == SUDDEN_ALTITUDE_CHANGE || this == LOST_CONTACT ||
                this == PROXIMITY_ALERT || this == WEATHER_ALERT ||
                this == FUEL_ALERT || this == COMMUNICATION_FAILURE ||
                this == SYSTEM_MALFUNCTION || this == CUSTOM;
    }

    public boolean isVesselApplicable() {
        return this == SPEED_VIOLATION || this == GEOGRAPHIC_BOUNDARY ||
                this == LOST_CONTACT || this == PROXIMITY_ALERT ||
                this == DEVIATION_FROM_ROUTE || this == SECURITY_ALERT ||
                this == DANGEROUS_CARGO || this == WEATHER_ALERT ||
                this == MAINTENANCE_ALERT || this == COMMUNICATION_FAILURE ||
                this == SYSTEM_MALFUNCTION || this == CUSTOM;
    }
}