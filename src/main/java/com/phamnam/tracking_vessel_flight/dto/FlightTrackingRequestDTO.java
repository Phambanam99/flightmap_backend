package com.phamnam.tracking_vessel_flight.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightTrackingRequestDTO {

    @JsonProperty("Id")
    private Long id;
    
    @JsonProperty("AircraftId")
    private Long aircraftId;
    
    @JsonProperty("SecsOfTrack")
    private Integer secsOfTrack;
    
    @JsonProperty("ReceverSourceId")
    private Integer receverSourceId;
    
    @JsonProperty("Hexident")
    private String hexident;
    
    @JsonProperty("Register")
    private String register;
    
    @JsonProperty("Altitude")
    private Float altitude;
    
    @JsonProperty("AltitudeType")
    private String altitudeType;
    
    @JsonProperty("TargetAlt")
    private Float targetAlt;
    
    @JsonProperty("Callsign")
    private String callsign;
    
    @JsonProperty("IsTisb")
    private Boolean isTisb;
    
    @JsonProperty("Speed")
    private Float speed;
    
    @JsonProperty("SpeedType")
    private String speedType;
    
    @JsonProperty("VerticalSpeed")
    private Float verticalSpeed;
    
    @JsonProperty("Type")
    private String type;
    
    @JsonProperty("Manufacture")
    private String manufacture;
    
    @JsonProperty("ContructorNumber")
    private String contructorNumber;
    
    @JsonProperty("FromPort")
    private String fromPort;
    
    @JsonProperty("ToPort")
    private String toPort;
    
    @JsonProperty("Operator")
    private String operator;
    
    @JsonProperty("OperatorCode")
    private String operatorCode;
    
    @JsonProperty("Squawk")
    private Integer squawk;
    
    @JsonProperty("Distance")
    private Float distance;
    
    @JsonProperty("Bearing")
    private Float bearing;
    
    @JsonProperty("Engines")
    private String engines;
    
    @JsonProperty("EngineType")
    private String engineType;
    
    @JsonProperty("IsMilitary")
    private Boolean isMilitary;
    
    @JsonProperty("Country")
    private String country;
    
    @JsonProperty("TransponderType")
    private String transponderType;
    
    @JsonProperty("Year")
    private Integer year;
    
    @JsonProperty("UnixTime")
    private Long unixTime;
    
    @JsonProperty("UpdateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateTime;
    
    @JsonProperty("Longitude")
    private Float longitude;
    
    @JsonProperty("Latitude")
    private Float latitude;
    
    @JsonProperty("Source")
    private String source;
    
    @JsonProperty("Flight")
    private String flight;
    
    @JsonProperty("FlightType")
    private String flightType;
    
    @JsonProperty("LandingUnixTimes")
    private Long landingUnixTimes;
    
    @JsonProperty("LandingTimes")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime landingTimes;
    
    @JsonProperty("ItemType")
    private Integer itemType;
}