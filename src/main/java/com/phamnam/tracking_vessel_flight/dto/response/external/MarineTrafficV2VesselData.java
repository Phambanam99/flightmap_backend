package com.phamnam.tracking_vessel_flight.dto.response.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual vessel data from MarineTraffic V2 API
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarineTrafficV2VesselData {

    @JsonProperty("mmsi")
    private String mmsi;

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("lon")
    private Double lon;

    @JsonProperty("speed")
    private Double speed;

    @JsonProperty("course")
    private Integer course;

    @JsonProperty("heading")
    private Integer heading;

    @JsonProperty("status")
    private String status;

    @JsonProperty("shipname")
    private String shipname;

    @JsonProperty("shiptype")
    private String shiptype;

    @JsonProperty("imo")
    private String imo;

    @JsonProperty("callsign")
    private String callsign;

    @JsonProperty("flag")
    private String flag;

    @JsonProperty("length")
    private Integer length;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("draught")
    private Double draught;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("eta")
    private String eta;

    @JsonProperty("lastport")
    private String lastport;

    @JsonProperty("nextport")
    private String nextport;

    @JsonProperty("timestamp")
    private String timestamp;
}
