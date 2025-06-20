package com.phamnam.tracking_vessel_flight.dto.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VesselTrackingRequest {
    private String mmsi;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Integer course;
    private Integer heading;
    private String navigationStatus;
    private String vesselName;
    private String vesselType;
    private String imo;
    private String callsign;
    private String flag;
    private Integer length;
    private Integer width;
    private Double draught;
    private String destination;
    private String eta;
    private LocalDateTime timestamp;
    private Double dataQuality;

    // Additional fields for enhanced tracking
    private String cargoType;
    private Integer deadweight;
    private Integer grossTonnage;
    private String buildYear;
    private String portOfRegistry;
    private String ownerOperator;
    private String vesselClass;
    private Boolean dangerousCargo;
    private Boolean securityAlert;
    private String route;
    private String lastPort;
    private String nextPort;
}