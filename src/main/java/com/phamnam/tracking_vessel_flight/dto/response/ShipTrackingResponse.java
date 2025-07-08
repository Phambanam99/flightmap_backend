package com.phamnam.tracking_vessel_flight.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipTrackingResponse {
    private Long id;
    
    // Voyage information
    private Long voyageId;
    private String voyageNumber;
    private String shipName;
    private String mmsi;
    private String imo;
    
    // Location data
    private Double longitude;
    private Double latitude;
    private Float speed;
    private Float course;
    private String heading;
    private String destination;
    private Float draught;
    private String eta;
    
    // Technical data
    private Integer navStatus;
    private String statusDescription;
    private Long lastPositionTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    private String updatedByUsername;
} 