package com.phamnam.tracking_vessel_flight.dto.response.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual aircraft data from ADS-B Exchange API
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdsbExchangeAircraftData {

    @JsonProperty("hex")
    private String hex; // ICAO identifier

    @JsonProperty("flight")
    private String flight; // Flight number/callsign

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("lon")
    private Double lon;

    @JsonProperty("alt_baro")
    private Integer altBaro; // Barometric altitude

    @JsonProperty("alt_geom")
    private Integer altGeom; // Geometric altitude

    @JsonProperty("gs")
    private Double gs; // Ground speed

    @JsonProperty("ias")
    private Double ias; // Indicated airspeed

    @JsonProperty("tas")
    private Double tas; // True airspeed

    @JsonProperty("mach")
    private Double mach; // Mach number

    @JsonProperty("track")
    private Double track; // Track/heading

    @JsonProperty("track_rate")
    private Double trackRate;

    @JsonProperty("roll")
    private Double roll;

    @JsonProperty("mag_heading")
    private Double magHeading;

    @JsonProperty("true_heading")
    private Double trueHeading;

    @JsonProperty("baro_rate")
    private Integer baroRate; // Rate of climb/descent

    @JsonProperty("geom_rate")
    private Integer geomRate;

    @JsonProperty("squawk")
    private String squawk; // Transponder code

    @JsonProperty("emergency")
    private String emergency;

    @JsonProperty("category")
    private String category; // Aircraft category

    @JsonProperty("nav_qnh")
    private Double navQnh;

    @JsonProperty("nav_altitude_mcp")
    private Integer navAltitudeMcp;

    @JsonProperty("nav_heading")
    private Double navHeading;

    @JsonProperty("nav_modes")
    private String[] navModes;

    @JsonProperty("seen")
    private Double seen; // Seconds since last message

    @JsonProperty("seen_pos")
    private Double seenPos; // Seconds since last position

    @JsonProperty("rssi")
    private Double rssi; // Signal strength

    @JsonProperty("alert")
    private Boolean alert;

    @JsonProperty("spi")
    private Boolean spi; // Special position identification

    @JsonProperty("nic")
    private Integer nic; // Navigation integrity category

    @JsonProperty("rc")
    private Integer rc; // Radius of containment

    @JsonProperty("version")
    private Integer version;

    @JsonProperty("nic_baro")
    private Integer nicBaro;

    @JsonProperty("nac_p")
    private Integer nacP; // Navigation accuracy category - position

    @JsonProperty("nac_v")
    private Integer nacV; // Navigation accuracy category - velocity

    @JsonProperty("sil")
    private Integer sil; // Source integrity level

    @JsonProperty("sil_type")
    private String silType;

    @JsonProperty("gva")
    private Integer gva; // Geometric vertical accuracy

    @JsonProperty("sda")
    private Integer sda; // System design assurance

    @JsonProperty("mlat")
    private String[] mlat; // Multilateration fields

    @JsonProperty("tisb")
    private String[] tisb; // TIS-B fields

    @JsonProperty("messages")
    private Long messages; // Number of messages received

    @JsonProperty("type")
    private String type; // Aircraft type

    @JsonProperty("dbFlags")
    private Long dbFlags;
}
