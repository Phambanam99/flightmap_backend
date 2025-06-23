package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.RawAircraftData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RawAircraftDataRepository extends JpaRepository<RawAircraftData, Long> {

    // Find by data source
    List<RawAircraftData> findByDataSourceAndReceivedAtBetween(
            String dataSource, LocalDateTime start, LocalDateTime end);

    // Find by hexident across all sources
    List<RawAircraftData> findByHexidentAndReceivedAtBetween(
            String hexident, LocalDateTime start, LocalDateTime end);

    // Find by source and hexident for deduplication
    List<RawAircraftData> findByDataSourceAndHexidentAndReceivedAtAfter(
            String dataSource, String hexident, LocalDateTime after);

    // Data quality analysis by source
    @Query("SELECT r.dataSource, AVG(r.dataQuality), COUNT(r) " +
            "FROM RawAircraftData r " +
            "WHERE r.receivedAt BETWEEN :start AND :end " +
            "GROUP BY r.dataSource")
    List<Object[]> getDataQualityBySource(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // API response time analysis
    @Query("SELECT r.dataSource, AVG(r.apiResponseTime), MIN(r.apiResponseTime), MAX(r.apiResponseTime) " +
            "FROM RawAircraftData r " +
            "WHERE r.receivedAt BETWEEN :start AND :end " +
            "GROUP BY r.dataSource")
    List<Object[]> getResponseTimeStatsBySource(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Find duplicate aircraft records
    @Query("SELECT r FROM RawAircraftData r " +
            "WHERE r.hexident = :hexident " +
            "AND r.receivedAt BETWEEN :start AND :end " +
            "ORDER BY r.receivedAt DESC")
    List<RawAircraftData> findDuplicatesForAircraft(@Param("hexident") String hexident,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Find invalid data for cleanup
    List<RawAircraftData> findByIsValidFalseAndReceivedAtBefore(LocalDateTime before);

    // Find data for retention cleanup
    List<RawAircraftData> findByReceivedAtBefore(LocalDateTime before);

    // Count records by source
    @Query("SELECT r.dataSource, COUNT(r) FROM RawAircraftData r " +
            "WHERE r.receivedAt BETWEEN :start AND :end " +
            "GROUP BY r.dataSource")
    List<Object[]> countRecordsBySource(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Find emergency aircraft across all sources
    List<RawAircraftData> findByEmergencyTrueAndReceivedAtBetween(
            LocalDateTime start, LocalDateTime end);

    // Find recent data for specific source
    Page<RawAircraftData> findByDataSourceOrderByReceivedAtDesc(String dataSource, Pageable pageable);

    // Health check: recent data availability by source
    @Query("SELECT r.dataSource, MAX(r.receivedAt) " +
            "FROM RawAircraftData r " +
            "GROUP BY r.dataSource")
    List<Object[]> getLastDataReceiptBySource();
}