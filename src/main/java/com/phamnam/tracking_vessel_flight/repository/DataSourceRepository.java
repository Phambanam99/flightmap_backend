package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.DataSource;
import com.phamnam.tracking_vessel_flight.models.enums.DataSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

    Optional<DataSource> findByName(String name);

    List<DataSource> findByIsEnabledTrue();

    List<DataSource> findByIsActiveTrue();

    List<DataSource> findBySourceType(DataSourceType sourceType);

    List<DataSource> findByIsEnabledTrueAndIsActiveTrue();

    @Query("SELECT ds FROM DataSource ds WHERE ds.isEnabled = true ORDER BY ds.priority ASC")
    List<DataSource> findEnabledOrderByPriority();

    @Query("SELECT ds FROM DataSource ds WHERE ds.consecutiveFailures >= ds.circuitBreakerThreshold")
    List<DataSource> findFailedDataSources();

    @Query("SELECT ds FROM DataSource ds WHERE ds.lastSuccessTime < :threshold")
    List<DataSource> findStaleDataSources(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT ds FROM DataSource ds WHERE ds.totalRequests > 0 AND (CAST(ds.successfulRequests AS double) / CAST(ds.totalRequests AS double) * 100) < :minRate")
    List<DataSource> findLowPerformanceDataSources(@Param("minRate") Double minRate);

    boolean existsByNameAndSourceType(String name, DataSourceType sourceType);
}