package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.DataSource;
import com.phamnam.tracking_vessel_flight.models.DataSourceStatus;
import com.phamnam.tracking_vessel_flight.models.enums.SourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceStatusRepository extends JpaRepository<DataSourceStatus, Long> {

    List<DataSourceStatus> findByDataSourceOrderByCheckTimeDesc(DataSource dataSource);

    List<DataSourceStatus> findByDataSourceAndCheckTimeBetween(
            DataSource dataSource, LocalDateTime start, LocalDateTime end);

    List<DataSourceStatus> findByStatus(SourceStatus status);

    @Query("SELECT dss FROM DataSourceStatus dss WHERE dss.dataSource.id = :dataSourceId " +
            "ORDER BY dss.checkTime DESC LIMIT 1")
    Optional<DataSourceStatus> findLatestByDataSourceId(@Param("dataSourceId") Long dataSourceId);

    @Query("SELECT dss FROM DataSourceStatus dss WHERE dss.checkTime >= :since " +
            "ORDER BY dss.checkTime DESC")
    List<DataSourceStatus> findRecentStatuses(@Param("since") LocalDateTime since);

    @Query("SELECT dss FROM DataSourceStatus dss WHERE dss.dataSource.id = :dataSourceId " +
            "AND dss.checkTime >= :since ORDER BY dss.checkTime DESC")
    List<DataSourceStatus> findByDataSourceIdAndSince(
            @Param("dataSourceId") Long dataSourceId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(dss.responseTime) FROM DataSourceStatus dss WHERE dss.dataSource.id = :dataSourceId " +
            "AND dss.checkTime >= :since")
    Double getAverageResponseTime(
            @Param("dataSourceId") Long dataSourceId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(dss) FROM DataSourceStatus dss WHERE dss.dataSource.id = :dataSourceId " +
            "AND dss.status = :status AND dss.checkTime >= :since")
    Long countByDataSourceIdAndStatusAndSince(
            @Param("dataSourceId") Long dataSourceId,
            @Param("status") SourceStatus status,
            @Param("since") LocalDateTime since);

    void deleteByCheckTimeBefore(LocalDateTime cutoff);
}