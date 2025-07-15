package com.phamnam.tracking_vessel_flight.dto.response.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual vessel data from MarineTraffic Main API (used by
 * ExternalApiService)
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarineTrafficVesselData {

    @JsonProperty("mmsi")
    @JsonAlias({ "MMSI" })
    private String mmsi;

    @JsonProperty("lat")
    @JsonAlias({ "LAT" })
    private Double lat;

    @JsonProperty("lon")
    @JsonAlias({ "LON" })
    private Double lon;

    @JsonProperty("speed")
    @JsonAlias({ "SPEED" })
    private Double speed;

    @JsonProperty("course")
    @JsonAlias({ "COURSE" })
    private Integer course;

    @JsonProperty("heading")
    @JsonAlias({ "HEADING" })
    private Integer heading;

    @JsonProperty("status")
    @JsonAlias({ "STATUS" })
    private String status;

    @JsonProperty("shipname")
    @JsonAlias({ "SHIPNAME" })
    private String shipname;

    @JsonProperty("shiptype")
    @JsonAlias({ "SHIPTYPE" })
    private String shiptype;

    @JsonProperty("imo")
    @JsonAlias({ "IMO" })
    private String imo;

    @JsonProperty("callsign")
    @JsonAlias({ "CALLSIGN" })
    private String callsign;

    @JsonProperty("flag")
    @JsonAlias({ "FLAG" })
    private String flag;

    @JsonProperty("length")
    @JsonAlias({ "LENGTH" })
    private Integer length;

    @JsonProperty("width")
    @JsonAlias({ "WIDTH" })
    private Integer width;

    @JsonProperty("draught")
    @JsonAlias({ "DRAUGHT" })
    private Double draught;

    @JsonProperty("destination")
    @JsonAlias({ "DESTINATION" })
    private String destination;

    @JsonProperty("eta")
    @JsonAlias({ "ETA" })
    private String eta;

    @JsonProperty("lastport")
    private String lastport;

    @JsonProperty("nextport")
    private String nextport;

    @JsonProperty("timestamp")
    private String timestamp;
}
