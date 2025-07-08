package com.phamnam.tracking_vessel_flight.models.enums;

public enum EntityType {
    AIRCRAFT("Aircraft"),
    VESSEL("Vessel"),
    SHIP("Ship"),
    FLIGHT("Flight"),
    VEHICLE("Vehicle"),
    UNKNOWN("Unknown");

    private final String displayName;

    EntityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAirborne() {
        return this == AIRCRAFT || this == FLIGHT;
    }

    public boolean isMarine() {
        return this == VESSEL || this == SHIP;
    }
}