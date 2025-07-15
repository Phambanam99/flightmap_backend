package com.phamnam.tracking_vessel_flight.dto.response.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual vessel data from VesselFinder API
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VesselFinderVesselData {

    @JsonProperty("mmsi")
    private String mmsi;

    @JsonProperty("MMSI")
    private String mmsiUpper; // VesselFinder sometimes uses uppercase

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("lng")
    private Double lng;

    @JsonProperty("lon")
    private Double lon;

    @JsonProperty("speed")
    private Double speed;

    @JsonProperty("sog")
    private Double sog; // Speed over ground

    @JsonProperty("course")
    private Integer course;

    @JsonProperty("cog")
    private Integer cog; // Course over ground

    @JsonProperty("heading")
    private Integer heading;

    @JsonProperty("hdg")
    private Integer hdg;

    @JsonProperty("navStatus")
    private String navStatus;

    @JsonProperty("navstat")
    private String navstat;

    @JsonProperty("vesselName")
    private String vesselName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("vesselType")
    private String vesselType;

    @JsonProperty("type")
    private String type;

    @JsonProperty("imo")
    private String imo;

    @JsonProperty("IMO")
    private String imoUpper;

    @JsonProperty("callsign")
    private String callsign;

    @JsonProperty("call")
    private String call;

    @JsonProperty("flag")
    private String flag;

    @JsonProperty("country")
    private String country;

    @JsonProperty("length")
    private Integer length;

    @JsonProperty("loa")
    private Integer loa; // Length overall

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("beam")
    private Integer beam;

    @JsonProperty("draught")
    private Double draught;

    @JsonProperty("draft")
    private Double draft;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("dest")
    private String dest;

    @JsonProperty("eta")
    private String eta;

    @JsonProperty("ETA")
    private String etaUpper;

    @JsonProperty("lastPort")
    private String lastPort;

    @JsonProperty("nextPort")
    private String nextPort;

    // Helper methods to get the correct value from multiple possible fields
    public String getActualMmsi() {
        return mmsi != null ? mmsi : mmsiUpper;
    }

    public Double getActualLatitude() {
        return latitude != null ? latitude : lat;
    }

    public Double getActualLongitude() {
        return longitude != null ? longitude : (lng != null ? lng : lon);
    }

    public Double getActualSpeed() {
        return speed != null ? speed : sog;
    }

    public Integer getActualCourse() {
        return course != null ? course : cog;
    }

    public Integer getActualHeading() {
        return heading != null ? heading : hdg;
    }

    public String getActualNavStatus() {
        return navStatus != null ? navStatus : navstat;
    }

    public String getActualVesselName() {
        return vesselName != null ? vesselName : name;
    }

    public String getActualVesselType() {
        return vesselType != null ? vesselType : type;
    }

    public String getActualImo() {
        return imo != null ? imo : imoUpper;
    }

    public String getActualCallsign() {
        return callsign != null ? callsign : call;
    }

    public String getActualFlag() {
        return flag != null ? flag : country;
    }

    public Integer getActualLength() {
        return length != null ? length : loa;
    }

    public Integer getActualWidth() {
        return width != null ? width : beam;
    }

    public Double getActualDraught() {
        return draught != null ? draught : draft;
    }

    public String getActualDestination() {
        return destination != null ? destination : dest;
    }

    public String getActualEta() {
        return eta != null ? eta : etaUpper;
    }
}
