package com.phamnam.tracking_vessel_flight.models.enums;

public enum DataSourceType {
    FLIGHT_RADAR("FlightRadar24"),
    MARINE_TRAFFIC("MarineTraffic"),
    ADS_B("ADS-B Exchange"),
    VESSEL_FINDER("VesselFinder"),
    OPEN_SKY("OpenSky Network"),
    SHIP_TRACKING("ShipTracking"),
    MOCK_AIRCRAFT("Mock Aircraft Generator"),
    MOCK_VESSEL("Mock Vessel Generator"),
    INTERNAL("Internal System"),
    CUSTOM("Custom Source");

    private final String displayName;

    DataSourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isExternalApi() {
        return this != INTERNAL && this != MOCK_AIRCRAFT && this != MOCK_VESSEL;
    }

    public boolean isMockSource() {
        return this == MOCK_AIRCRAFT || this == MOCK_VESSEL;
    }

    public boolean isAircraftSource() {
        return this == FLIGHT_RADAR || this == ADS_B || this == OPEN_SKY || this == MOCK_AIRCRAFT;
    }

    public boolean isVesselSource() {
        return this == MARINE_TRAFFIC || this == VESSEL_FINDER || this == SHIP_TRACKING || this == MOCK_VESSEL;
    }
}