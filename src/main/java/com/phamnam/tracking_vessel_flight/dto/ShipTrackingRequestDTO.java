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
public class ShipTrackingRequestDTO {

    @JsonProperty("Id")
    private Long id;

    @JsonProperty("VoyageId")
    private Long voyageId;

    @JsonProperty("ShipId")
    private Long shipId;

    @JsonProperty("MMSI")
    private String mmsi;

    @JsonProperty("IMO")
    private String imo;

    @JsonProperty("CallSign")
    private String callSign;

    @JsonProperty("VesselName")
    private String vesselName;

    @JsonProperty("VesselType")
    private String vesselType;

    @JsonProperty("Flag")
    private String flag;

    @JsonProperty("Length")
    private Float length;

    @JsonProperty("Width")
    private Float width;

    @JsonProperty("Draught")
    private Float draught;

    @JsonProperty("Speed")
    private Float speed;

    @JsonProperty("Course")
    private Float course;

    @JsonProperty("Heading")
    private Float heading;

    @JsonProperty("NavigationStatus")
    private String navigationStatus;

    @JsonProperty("Latitude")
    private Float latitude;

    @JsonProperty("Longitude")
    private Float longitude;

    @JsonProperty("Destination")
    private String destination;

    @JsonProperty("ETA")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eta;

    @JsonProperty("LastPort")
    private String lastPort;

    @JsonProperty("NextPort")
    private String nextPort;

    @JsonProperty("CargoType")
    private String cargoType;

    @JsonProperty("GrossTonnage")
    private Integer grossTonnage;

    @JsonProperty("DeadWeight")
    private Integer deadWeight;

    @JsonProperty("YearBuilt")
    private Integer yearBuilt;

    @JsonProperty("UnixTime")
    private Long unixTime;

    @JsonProperty("UpdateTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateTime;

    @JsonProperty("Source")
    private String source;

    @JsonProperty("DataSourceId")
    private Integer dataSourceId;

    @JsonProperty("DataQuality")
    private Double dataQuality;

    @JsonProperty("ReceiverStationId")
    private String receiverStationId;

    @JsonProperty("CrewCount")
    private Integer crewCount;

    @JsonProperty("PassengerCount")
    private Integer passengerCount;

    @JsonProperty("OwnerName")
    private String ownerName;

    @JsonProperty("OperatorName")
    private String operatorName;

    @JsonProperty("ManagementCompany")
    private String managementCompany;

    @JsonProperty("ClassificationSociety")
    private String classificationSociety;

    @JsonProperty("IsActive")
    private Boolean isActive;

    @JsonProperty("LastSeenTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSeenTime;

    @JsonProperty("AISTransponderType")
    private String aisTransponderType;

    @JsonProperty("SpecialIndicators")
    private String specialIndicators;

    @JsonProperty("WeatherConditions")
    private String weatherConditions;

    @JsonProperty("SeaState")
    private String seaState;
}