package com.phamnam.tracking_vessel_flight.service.rest.interfaces;

import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;

import java.time.LocalDateTime;
import java.util.List;

public interface ColdStorageService {

    /**
     * Lưu trữ dữ liệu FlightTracking vào cold storage
     */
    void archiveFlightTrackingData(List<FlightTracking> trackingData);

    /**
     * Lưu trữ dữ liệu VesselTracking vào cold storage
     */
    void archiveVesselTrackingData(List<ShipTracking> trackingData);

    /**
     * Truy vấn lịch sử lâu dài của flight
     */
    List<FlightTracking> queryFlightTrackingHistory(Long flightId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Truy vấn lịch sử lâu dài của vessel
     */
    List<ShipTracking> queryVesselTrackingHistory(Long vesselId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Thực hiện ETL job để chuyển dữ liệu từ warm storage sang cold storage
     */
    void performDataArchiving();
}