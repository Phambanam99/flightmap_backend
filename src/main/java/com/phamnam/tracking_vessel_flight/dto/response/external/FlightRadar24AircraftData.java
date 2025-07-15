package com.phamnam.tracking_vessel_flight.dto.response.external;

import lombok.Data;

/**
 * FlightRadar24 aircraft data array structure
 * Array format: [hexident, lat, lon, heading, alt, speed, squawk, radar,
 * aircraftType,
 * registration, timestamp, origin, destination, flightNumber, onGround,
 * verticalRate, callsign, source]
 * 
 * This class provides typed access to the FlightRadar24 array-based aircraft
 * data
 */
@Data
public class FlightRadar24AircraftData {

    // Array indices based on FlightRadar24 API documentation
    public static final int INDEX_HEX_IDENT = 0;
    public static final int INDEX_LATITUDE = 1;
    public static final int INDEX_LONGITUDE = 2;
    public static final int INDEX_HEADING = 3;
    public static final int INDEX_ALTITUDE = 4;
    public static final int INDEX_SPEED = 5;
    public static final int INDEX_SQUAWK = 6;
    public static final int INDEX_RADAR = 7;
    public static final int INDEX_AIRCRAFT_TYPE = 8;
    public static final int INDEX_REGISTRATION = 9;
    public static final int INDEX_TIMESTAMP = 10;
    public static final int INDEX_ORIGIN = 11;
    public static final int INDEX_DESTINATION = 12;
    public static final int INDEX_FLIGHT_NUMBER = 13;
    public static final int INDEX_ON_GROUND = 14;
    public static final int INDEX_VERTICAL_RATE = 15;
    public static final int INDEX_CALLSIGN = 16;
    public static final int INDEX_SOURCE = 17;

    private Object[] data;

    public FlightRadar24AircraftData(Object[] data) {
        this.data = data;
    }

    // Safe getter methods with null checks and type conversion

    public String getHexIdent() {
        return getStringValue(INDEX_HEX_IDENT);
    }

    public Double getLatitude() {
        return getDoubleValue(INDEX_LATITUDE);
    }

    public Double getLongitude() {
        return getDoubleValue(INDEX_LONGITUDE);
    }

    public Integer getHeading() {
        return getIntegerValue(INDEX_HEADING);
    }

    public Integer getAltitude() {
        return getIntegerValue(INDEX_ALTITUDE);
    }

    public Integer getSpeed() {
        return getIntegerValue(INDEX_SPEED);
    }

    public String getSquawk() {
        return getStringValue(INDEX_SQUAWK);
    }

    public String getRadar() {
        return getStringValue(INDEX_RADAR);
    }

    public String getAircraftType() {
        return getStringValue(INDEX_AIRCRAFT_TYPE);
    }

    public String getRegistration() {
        return getStringValue(INDEX_REGISTRATION);
    }

    public Long getTimestamp() {
        return getLongValue(INDEX_TIMESTAMP);
    }

    public String getOrigin() {
        return getStringValue(INDEX_ORIGIN);
    }

    public String getDestination() {
        return getStringValue(INDEX_DESTINATION);
    }

    public String getFlightNumber() {
        return getStringValue(INDEX_FLIGHT_NUMBER);
    }

    public Boolean getOnGround() {
        return getBooleanValue(INDEX_ON_GROUND);
    }

    public Integer getVerticalRate() {
        return getIntegerValue(INDEX_VERTICAL_RATE);
    }

    public String getCallsign() {
        return getStringValue(INDEX_CALLSIGN);
    }

    public String getSource() {
        return getStringValue(INDEX_SOURCE);
    }

    // Helper methods for safe type conversion

    private String getStringValue(int index) {
        if (data == null || index >= data.length || data[index] == null) {
            return null;
        }
        return data[index].toString().trim();
    }

    private Double getDoubleValue(int index) {
        if (data == null || index >= data.length || data[index] == null) {
            return null;
        }
        try {
            if (data[index] instanceof Number) {
                return ((Number) data[index]).doubleValue();
            }
            return Double.valueOf(data[index].toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerValue(int index) {
        if (data == null || index >= data.length || data[index] == null) {
            return null;
        }
        try {
            if (data[index] instanceof Number) {
                return ((Number) data[index]).intValue();
            }
            return Integer.valueOf(data[index].toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getLongValue(int index) {
        if (data == null || index >= data.length || data[index] == null) {
            return null;
        }
        try {
            if (data[index] instanceof Number) {
                return ((Number) data[index]).longValue();
            }
            return Long.valueOf(data[index].toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBooleanValue(int index) {
        if (data == null || index >= data.length || data[index] == null) {
            return null;
        }
        if (data[index] instanceof Boolean) {
            return (Boolean) data[index];
        }
        if (data[index] instanceof Number) {
            return ((Number) data[index]).intValue() != 0;
        }
        return Boolean.valueOf(data[index].toString());
    }
}
