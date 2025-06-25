package com.phamnam.tracking_vessel_flight.service;

import com.phamnam.tracking_vessel_flight.models.FlightTracking;
import com.phamnam.tracking_vessel_flight.models.ShipTracking;
import com.phamnam.tracking_vessel_flight.repository.FlightTrackingRepository;
import com.phamnam.tracking_vessel_flight.service.rest.interfaces.ColdStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ColdStorageServiceImpl implements ColdStorageService {

    private final JdbcTemplate jdbcTemplate;
    private final FlightTrackingRepository flightTrackingRepository;

    @Value("${tracking.data.warm-storage.ttl-days:30}")
    private int warmStorageTtlDays;

    @Value("${tracking.data.hot-storage.ttl-hours:24}")
    private int hotStorageTtlHours;

    @Override
    @Transactional
    public void archiveFlightTrackingData(List<FlightTracking> trackingList) {
        if (trackingList == null || trackingList.isEmpty()) {
            return;
        }

        List<Object[]> batchArgs = new ArrayList<>();

        for (FlightTracking tracking : trackingList) {
            Object[] args = new Object[] {
                    tracking.getTrackingId(),
                    tracking.getFlight() != null ? tracking.getFlight().getId() : null,
                    tracking.getCallsign(),
                    tracking.getAltitude(),
                    tracking.getAltitudeType(),
                    tracking.getTargetAlt(),
                    tracking.getSpeed(),
                    tracking.getSpeedType(),
                    tracking.getVerticalSpeed(),
                    tracking.getSquawk(),
                    tracking.getDistance(),
                    tracking.getBearing(),
                    tracking.getUnixTime(),
                    tracking.getUpdateTime(),
                    tracking.getLocation().getX(),
                    tracking.getLocation().getY(),
                    tracking.getLandingUnixTimes(),
                    tracking.getLandingTimes(),
                    tracking.getCreatedAt(),
                    tracking.getUpdatedAt(),
                    tracking.getLocation()
            };
            batchArgs.add(args);
        }

        String sql = "INSERT INTO flight_tracking_archive (" +
                "tracking_id, flight_id, callsign, altitude, altitude_type, target_alt, " +
                "speed, speed_type, vertical_speed, squawk, distance, bearing, " +
                "unix_time, update_time, longitude, latitude, landing_unix_times, " +
                "landing_times, created_at, updated_at, location) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.info("Archived {} flight tracking records to cold storage", trackingList.size());
    }

    @Override
    @Transactional
    public void archiveVesselTrackingData(List<ShipTracking> trackingList) {
        // Triển khai tương tự như archiveFlightTrackingData
    }

    @Override
    public List<FlightTracking> queryFlightTrackingHistory(Long flightId, LocalDateTime startTime,
            LocalDateTime endTime) {
        String sql = "SELECT * FROM flight_tracking_archive " +
                "WHERE flight_id = ? AND update_time BETWEEN ? AND ? " +
                "ORDER BY update_time ASC";

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    FlightTracking tracking = new FlightTracking();
                    tracking.setTrackingId(rs.getLong("tracking_id"));
                    // Ánh xạ các trường còn lại từ ResultSet sang FlightTracking
                    // ...
                    return tracking;
                },
                flightId, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
    }

    @Override
    public List<ShipTracking> queryVesselTrackingHistory(Long vesselId, LocalDateTime startTime,
            LocalDateTime endTime) {
        // Triển khai tương tự như queryFlightTrackingHistory
        return new ArrayList<>();
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 1 * * ?") // Chạy lúc 1:00 AM mỗi ngày
    public void performDataArchiving() {

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(warmStorageTtlDays);

        log.info("Starting scheduled data archiving process for data older than {}", cutoffDate);

        // 1. Lấy dữ liệu cần chuyển sang cold storage
        List<FlightTracking> oldFlightTrackings = flightTrackingRepository.findByUpdateTimeBefore(cutoffDate);

        if (oldFlightTrackings.isEmpty()) {
            log.info("No flight tracking data to archive");
            return;
        }

        log.info("Found {} flight tracking records to archive", oldFlightTrackings.size());

        try {
            // 2. Lưu vào cold storage
            archiveFlightTrackingData(oldFlightTrackings);

            // 3. Xóa khỏi warm storage (TimescaleDB)
            flightTrackingRepository.deleteByUpdateTimeBefore(cutoffDate);

            log.info("Successfully archived and cleaned up {} flight tracking records", oldFlightTrackings.size());
        } catch (Exception e) {
            log.error("Error archiving flight tracking data: {}", e.getMessage(), e);
        }

        // Làm tương tự cho vessel tracking nếu cần
    }

    /**
     * Move data from hot storage to warm storage based on TTL
     * Hot storage: Fast access (Redis/memory) for recent data
     * Warm storage: Database for older but still accessible data
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 */30 * * * ?") // Every 30 minutes
    public void moveFromHotToWarmStorage() {
        LocalDateTime hotStorageCutoff = LocalDateTime.now().minusHours(hotStorageTtlHours);

        log.info("🔄 Moving data from hot to warm storage for data older than {} hours", hotStorageTtlHours);

        try {
            // Move flight tracking data that's older than hot storage TTL
            List<FlightTracking> hotFlightData = flightTrackingRepository
                    .findByUpdateTimeBefore(hotStorageCutoff);

            if (!hotFlightData.isEmpty()) {
                log.info("📦 Moving {} flight tracking records from hot to warm storage", hotFlightData.size());
                // This could involve moving from Redis to Database or reorganizing storage
                // tiers
                // For now, we just log as the data is already in TimescaleDB
            }

        } catch (Exception e) {
            log.error("❌ Error moving data from hot to warm storage: {}", e.getMessage(), e);
        }
    }

    /**
     * Get hot storage statistics
     */
    public Map<String, Object> getHotStorageStats() {
        LocalDateTime hotStorageCutoff = LocalDateTime.now().minusHours(hotStorageTtlHours);

        long hotDataCount = flightTrackingRepository.countByLastSeenAfter(hotStorageCutoff);
        long warmDataCount = flightTrackingRepository
                .findByUpdateTimeBefore(hotStorageCutoff.minusDays(warmStorageTtlDays)).size();

        return Map.of(
                "hotStorageTtlHours", hotStorageTtlHours,
                "warmStorageTtlDays", warmStorageTtlDays,
                "hotDataCount", hotDataCount,
                "warmDataCount", warmDataCount,
                "cutoffTime", hotStorageCutoff);
    }
}