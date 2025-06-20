package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.service.rest.FlightTrackingService;
import com.phamnam.tracking_vessel_flight.service.rest.VoyageService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final FlightTrackingService flightTrackingService;
    private final TrackingCacheService trackingCacheService;
    // private final TrackingPushService trackingPushService;
    private final AircraftNotificationService aircraftNotificationService;

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

    @KafkaListener(topics = "ship-tracking-batch", groupId = "ship-tracking-batch-consumer-group", containerFactory = "batchShipKafkaListenerContainerFactory")
    public void consumeShipTrackingBatch(List<ShipTrackingRequest> trackings) {
        if (trackings == null || trackings.isEmpty()) {
            return;
        }

        log.info("Received batch of {} ship tracking updates", trackings.size());

        // Xử lý từng item trong batch
        for (ShipTrackingRequest tracking : trackings) {
            // Cập nhật cache
            // trackingCacheService.cacheShipTracking(tracking);

            // Lưu vào database
            // voyageTrackingService.save(tracking, null);
        }

        log.info("Processed batch of {} ship tracking updates", trackings.size());
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