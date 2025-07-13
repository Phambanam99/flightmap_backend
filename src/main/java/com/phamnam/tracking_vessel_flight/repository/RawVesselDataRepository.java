package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.RawVesselData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RawVesselDataRepository extends JpaRepository<RawVesselData, Long> {

    // Find by data source
    List<RawVesselData> findByDataSourceAndReceivedAtBetween(
            String dataSource, LocalDateTime start, LocalDateTime end);

    // Find by MMSI across all sources
    List<RawVesselData> findByMmsiAndReceivedAtBetween(
            String mmsi, LocalDateTime start, LocalDateTime end);

    // Find by source and MMSI for deduplication
    List<RawVesselData> findByDataSourceAndMmsiAndReceivedAtAfter(
            String dataSource, String mmsi, LocalDateTime after);

    // Data quality analysis by source
    @Query("SELECT r.dataSource, AVG(r.dataQuality), COUNT(r) " +
            "FROM RawVesselData r " +
            "WHERE r.receivedAt BETWEEN :start AND :end " +
            "GROUP BY r.dataSource")
    List<Object[]> getDataQualityBySource(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // API response time analysis
    @Query("SELECT r.dataSource, AVG(r.apiResponseTime), MIN(r.apiResponseTime), MAX(r.apiResponseTime) " +
            "FROM RawVesselData r " +
            "WHERE r.receivedAt BETWEEN :start AND :end " +
            "GROUP BY r.dataSource")
    List<Object[]> getResponseTimeStatsBySource(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Find duplicate vessel records
    @Query("SELECT r FROM RawVesselData r " +
            "WHERE r.mmsi = :mmsi " +
            "AND r.receivedAt BETWEEN :start AND :end " +
            "ORDER BY r.receivedAt DESC")
    List<RawVesselData> findDuplicatesForVessel(@Param("mmsi") String mmsi,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Find vessels by type and source
    List<RawVesselData> findByDataSourceAndVesselTypeAndReceivedAtBetween(
            String dataSource, String vesselType, LocalDateTime start, LocalDateTime end);

    // Find dangerous cargo vessels
    List<RawVesselData> findByDangerousCargoTrueAndReceivedAtBetween(
            LocalDateTime start, LocalDateTime end);

    // Find vessels with security alerts
    List<RawVesselData> findBySecurityAlertTrueAndReceivedAtBetween(
            LocalDateTime start, LocalDateTime end);

    // Find invalid data for cleanup
    List<RawVesselData> findByIsValidFalseAndReceivedAtBefore(LocalDateTime before);

    // Find data for retention cleanup
    List<RawVesselData> findByReceivedAtBefore(LocalDateTime before);

    // Count records by source
    @Query("SELECT r.dataSource, COUNT(r) FROM RawVesselData r " +
            "WHERE r.receivedAt BETWEEN :start AND :end " +
            "GROUP BY r.dataSource")
    List<Object[]> countRecordsBySource(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Vessel type distribution by source
    @Query("SELECT r.dataSource, r.vesselType, COUNT(r) " +
            "FROM RawVesselData r " +
            "WHERE r.receivedAt BETWEEN :start AND :end " +
            "GROUP BY r.dataSource, r.vesselType")
    List<Object[]> getVesselTypeDistributionBySource(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Find vessels by destination
    List<RawVesselData> findByDestinationContainingIgnoreCaseAndReceivedAtBetween(
            String destination, LocalDateTime start, LocalDateTime end);

    // Find vessels by flag state
    List<RawVesselData> findByFlagAndDataSourceAndReceivedAtBetween(
            String flag, String dataSource, LocalDateTime start, LocalDateTime end);

    // Find recent data for specific source
    Page<RawVesselData> findByDataSourceOrderByReceivedAtDesc(String dataSource, Pageable pageable);

    // Health check: recent data availability by source
    @Query("SELECT r.dataSource, MAX(r.receivedAt) " +
            "FROM RawVesselData r " +
            "GROUP BY r.dataSource")
    List<Object[]> getLastDataReceiptBySource();

    // Coverage analysis by geographic bounds
    @Query("SELECT r.dataSource, COUNT(r) " +
            "FROM RawVesselData r " +
            "WHERE r.latitude BETWEEN :minLat AND :maxLat " +
            "AND r.longitude BETWEEN :minLon AND :maxLon " +
            "AND r.receivedAt BETWEEN :start AND :end " +
            "GROUP BY r.dataSource")
    List<Object[]> getCoverageByGeographicBounds(@Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}