package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.ShipTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.service.rest.FlightTrackingService;
import com.phamnam.tracking_vessel_flight.service.rest.ShipTrackingService;
import com.phamnam.tracking_vessel_flight.repository.ShipRepository;
import com.phamnam.tracking_vessel_flight.models.Ship;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final FlightTrackingService flightTrackingService;
    private final TrackingCacheService trackingCacheService;
    // private final TrackingPushService trackingPushService;
    private final AircraftNotificationService aircraftNotificationService;
    private final ShipTrackingService shipTrackingService;
    private final ShipRepository shipRepository;

    // Theo dõi số cập nhật mới từ lần gửi batch update cuối
    private final AtomicInteger updateCounter = new AtomicInteger(0);
    // Thời gian cập nhật batch cuối cùng
    private final AtomicLong lastBatchUpdateTime = new AtomicLong(System.currentTimeMillis());
    // Ngưỡng số lượng cập nhật để kích hoạt batch update
    private static final int BATCH_THRESHOLD = 50;
    // Thời gian tối thiểu giữa các batch updates (ms)
    private static final long MIN_BATCH_INTERVAL = 5000;

    @KafkaListener(topics = "flight-tracking", groupId = "flight-tracking-consumer-group", containerFactory = "flightKafkaListenerContainerFactory")
    public void consumeFlightTracking(FlightTrackingRequestDTO tracking) {
        try {
            log.info("Received flight tracking data: {}", tracking);
            trackingCacheService.cacheFlightTracking(tracking);
            flightTrackingService.processNewTrackingData(tracking, null);
            aircraftNotificationService.sendAircraftUpdate(tracking);
            checkAndTriggerBatchUpdate();
        } catch (Exception e) {
            log.error("Error processing message", e);
            // Xử lý lỗi tại đây
        }
    }

    // Thêm phương thức mới để xử lý batch
    @KafkaListener(topics = "flight-tracking-batch", groupId = "flight-tracking-batch-consumer-group", containerFactory = "batchFlightKafkaListenerContainerFactory")
    public void consumeFlightTrackingBatch(List<ConsumerRecord<String, List<FlightTrackingRequestDTO>>> records) {
        for (ConsumerRecord<String, List<FlightTrackingRequestDTO>> record : records) {
            List<FlightTrackingRequestDTO> batch = record.value();
            for (FlightTrackingRequestDTO dto : batch) {
                try {
                    // log.info("Processing record: [{}]", dto);
                    trackingCacheService.cacheFlightTracking(dto);
                    flightTrackingService.processNewTrackingData(dto, null);
                    aircraftNotificationService.sendAircraftUpdate(dto);
                } catch (Exception e) {
                    log.error("Error processing record", e);
                }
            }
        }
        aircraftNotificationService.sendBatchUpdatesToAllAreas();
    }

    private void checkAndTriggerBatchUpdate() {
        int currentCount = updateCounter.incrementAndGet();
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastBatchUpdateTime.get();

        // Gửi batch update nếu đủ số lượng cập nhật hoặc đã qua đủ thời gian
        if (currentCount >= BATCH_THRESHOLD || elapsed >= MIN_BATCH_INTERVAL) {
            // Reset counter và thời gian
            updateCounter.set(0);
            lastBatchUpdateTime.set(currentTime);

            // Gửi batch update
            aircraftNotificationService.sendBatchUpdatesToAllAreas();
        }
    }

    @KafkaListener(topics = "ship-tracking", groupId = "ship-tracking-consumer-group")
    public void consumeShipTracking(ShipTrackingRequest tracking) {
        try {
            log.info("Received ship tracking data: {}", tracking);
            // Process single ship tracking request
            processShipTrackingData(tracking);
        } catch (Exception e) {
            log.error("Error processing ship tracking message", e);
        }
    }

    @KafkaListener(topics = "ship-tracking-dto", groupId = "ship-tracking-dto-consumer-group", containerFactory = "shipKafkaListenerContainerFactory")
    public void consumeShipTrackingDTO(ShipTrackingRequestDTO tracking) {
        try {
            log.info("Received ship tracking DTO data: {}", tracking);
            // Process ship tracking DTO from external APIs
            processShipTrackingDTOData(tracking);
        } catch (Exception e) {
            log.error("Error processing ship tracking DTO message", e);
        }
    }

    @KafkaListener(topics = "ship-tracking-batch", groupId = "ship-tracking-batch-consumer-group", containerFactory = "batchShipKafkaListenerContainerFactory")
    public void consumeShipTrackingBatch(List<ShipTrackingRequest> trackings) {
        if (trackings == null || trackings.isEmpty()) {
            return;
        }

        log.info("Received batch of {} ship tracking updates", trackings.size());

        // Xử lý từng item trong batch
        for (ShipTrackingRequest tracking : trackings) {
            try {
                processShipTrackingData(tracking);
            } catch (Exception e) {
                log.error("Error processing ship tracking in batch", e);
            }
        }

        log.info("Processed batch of {} ship tracking updates", trackings.size());
    }

    private void processShipTrackingData(ShipTrackingRequest tracking) {
        try {
            // Cập nhật cache
            // trackingCacheService.cacheShipTracking(tracking);

            // Approach 1: Try to find ship by MMSI and process tracking data
            // This will automatically create voyage if needed
            Long shipId = findOrCreateShipByMmsi(tracking.getMmsi());
            if (shipId != null) {
                shipTrackingService.processNewTrackingData(shipId, tracking, null);
                log.debug("Successfully processed ship tracking for MMSI: {} (Ship ID: {})", tracking.getMmsi(),
                        shipId);
            } else {
                // Approach 2: If ship doesn't exist, create a minimal one and process
                log.warn("Could not find/create ship for MMSI: {}, creating minimal record", tracking.getMmsi());
                // This approach requires additional implementation
                throw new RuntimeException("Ship creation not implemented yet for MMSI: " + tracking.getMmsi());
            }

        } catch (Exception e) {
            log.error("Error processing ship tracking data for MMSI: {}, voyage ID: {}",
                    tracking.getMmsi(), tracking.getVoyageId(), e);
            throw e;
        }
    }

    private void processShipTrackingDTOData(ShipTrackingRequestDTO tracking) {
        try {
            // Convert DTO to internal request format
            ShipTrackingRequest internalRequest = ShipTrackingRequest.builder()
                    .timestamp(tracking.getUpdateTime() != null ? tracking.getUpdateTime() : LocalDateTime.now())
                    .latitude(tracking.getLatitude() != null ? tracking.getLatitude().doubleValue() : null)
                    .longitude(tracking.getLongitude() != null ? tracking.getLongitude().doubleValue() : null)
                    .mmsi(tracking.getMmsi())
                    .heading(tracking.getHeading() != null ? tracking.getHeading().doubleValue() : null)
                    .navStatus(tracking.getNavigationStatus())
                    .speed(tracking.getSpeed() != null ? tracking.getSpeed().doubleValue() : null)
                    .course(tracking.getCourse() != null ? tracking.getCourse().doubleValue() : null)
                    .draught(tracking.getDraught() != null ? tracking.getDraught().doubleValue() : null)
                    .voyageId(tracking.getVoyageId())
                    .build();

            // Process using existing ship tracking logic
            processShipTrackingData(internalRequest);

        } catch (Exception e) {
            log.error("Error processing ship tracking DTO data for MMSI: {}, ID: {}",
                    tracking.getMmsi(), tracking.getId(), e);
            throw e;
        }
    }

    private Long findOrCreateShipByMmsi(String mmsi) {
        try {
            // Try to find existing ship by MMSI
            Ship ship = shipRepository.findAll().stream()
                    .filter(s -> mmsi.equals(s.getMmsi()))
                    .findFirst()
                    .orElse(null);

            if (ship != null) {
                log.debug("Found existing ship with MMSI: {} (ID: {})", mmsi, ship.getId());
                return ship.getId();
            }

            // Create new ship if not found
            log.info("Creating new ship for MMSI: {}", mmsi);
            Ship newShip = Ship.builder()
                    .mmsi(mmsi)
                    .name("Unknown Vessel " + mmsi)
                    .isActive(true)
                    .dataSource("Kafka Consumer")
                    .build();

            Ship savedShip = shipRepository.save(newShip);
            log.info("Created new ship with MMSI: {} (ID: {})", mmsi, savedShip.getId());
            return savedShip.getId();

        } catch (Exception e) {
            log.error("Error finding/creating ship for MMSI: {}", mmsi, e);
            return null;
        }
    }

    // Cấu hình tương tự cho Vessel nếu cần

    // Thêm các method để expose thống kê cho controller
    public Map<String, Object> getConsumerStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("updateCounter", updateCounter.get());
        stats.put("lastBatchUpdateTime", lastBatchUpdateTime.get());
        stats.put("batchThreshold", BATCH_THRESHOLD);
        stats.put("minBatchInterval", MIN_BATCH_INTERVAL);
        stats.put("timeSinceLastBatch", System.currentTimeMillis() - lastBatchUpdateTime.get());
        stats.put("status", "RUNNING");

        return stats;
    }

    public boolean isHealthy() {
        // Kiểm tra xem consumer có hoạt động bình thường không
        long timeSinceLastUpdate = System.currentTimeMillis() - lastBatchUpdateTime.get();
        // Nếu quá 30 giây không có batch update nào thì có thể có vấn đề
        return timeSinceLastUpdate < 30000;
    }

    public void resetCounters() {
        updateCounter.set(0);
        lastBatchUpdateTime.set(System.currentTimeMillis());
        log.info("Consumer counters reset manually");
    }

    public void triggerManualBatchUpdate() {
        try {
            aircraftNotificationService.sendBatchUpdatesToAllAreas();
            lastBatchUpdateTime.set(System.currentTimeMillis());
            log.info("Manual batch update triggered successfully");
        } catch (Exception e) {
            log.error("Error triggering manual batch update", e);
            throw new RuntimeException("Failed to trigger manual batch update", e);
        }
    }
}