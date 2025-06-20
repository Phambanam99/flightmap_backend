package com.phamnam.tracking_vessel_flight.repository;

import com.phamnam.tracking_vessel_flight.models.AlertEvent;
import com.phamnam.tracking_vessel_flight.models.AlertRule;
import com.phamnam.tracking_vessel_flight.models.enums.AlertPriority;
import com.phamnam.tracking_vessel_flight.models.enums.AlertStatus;
import com.phamnam.tracking_vessel_flight.models.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    List<AlertEvent> findByStatusOrderByEventTimeDesc(AlertStatus status);

    List<AlertEvent> findByEntityTypeAndEntityIdOrderByEventTimeDesc(EntityType entityType, String entityId);

    List<AlertEvent> findByEventTimeAfter(LocalDateTime eventTime);

    List<AlertEvent> findByPriorityAndStatusOrderByEventTimeDesc(AlertPriority priority, AlertStatus status);

    List<AlertEvent> findByStatusAndEventTimeBetween(AlertStatus status, LocalDateTime start, LocalDateTime end);

    int countByStatus(AlertStatus status);

    int countByStatusAndPriority(AlertStatus status, AlertPriority priority);

    int countByEventTimeAfter(LocalDateTime eventTime);

    boolean existsByAlertRuleAndEntityTypeAndEntityIdAndEventTimeAfterAndStatus(
            AlertRule alertRule, EntityType entityType, String entityId,
            LocalDateTime eventTime, AlertStatus status);

    @Query("SELECT ae FROM AlertEvent ae WHERE ae.status = :status AND ae.eventTime >= :since ORDER BY ae.eventTime DESC")
    List<AlertEvent> findActiveAlertsSince(@Param("status") AlertStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT ae FROM AlertEvent ae WHERE ae.entityType = :entityType AND ae.status = :status ORDER BY ae.eventTime DESC")
    List<AlertEvent> findByEntityTypeAndStatus(@Param("entityType") EntityType entityType,
            @Param("status") AlertStatus status);

    @Query("SELECT COUNT(ae) FROM AlertEvent ae WHERE ae.priority = :priority AND ae.eventTime >= :since")
    long countByPriorityAndEventTimeAfter(@Param("priority") AlertPriority priority,
            @Param("since") LocalDateTime since);

    @Query("SELECT ae FROM AlertEvent ae WHERE ae.latitude BETWEEN :minLat AND :maxLat " +
            "AND ae.longitude BETWEEN :minLon AND :maxLon AND ae.status = :status")
    List<AlertEvent> findAlertsInArea(@Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon, @Param("maxLon") Double maxLon,
            @Param("status") AlertStatus status);

    void deleteByEventTimeBefore(LocalDateTime cutoff);
}