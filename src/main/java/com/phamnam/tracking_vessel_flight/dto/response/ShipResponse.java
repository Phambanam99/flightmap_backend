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
public class ShipResponse {
    private Long id;
    private String name;
    private String mmsi;
    private String imo;
    private String callsign;
    private String shipType;
    private String flag;
    private Float length;
    private Float width;
    private Integer buildYear;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String updatedByUsername;
    private Integer activeVoyageCount;
}