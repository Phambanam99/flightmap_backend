package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import com.phamnam.tracking_vessel_flight.service.rest.FlightTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    @KafkaListener(
            topics = "flight-tracking",
            containerFactory = "flightKafkaListenerContainerFactory"
    )
    public void consumeFlightTracking(FlightTrackingRequest tracking) {
        log.info("Received flight tracking data: {}", tracking);

        // 1. Lưu vào cache (Redis) - Hot Storage
        trackingCacheService.cacheFlightTracking(tracking);

        // 2. Lưu vào TimescaleDB - Warm Storage
//       flightTrackingService.save(tracking, null);
       // 3. Gửi cập nhật qua WebSocket 
        aircraftNotificationService.sendAircraftUpdate(tracking);
        // 4. Kiểm tra xem có nên gửi batch update hay không
        checkAndTriggerBatchUpdate();
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

    // Cấu hình tương tự cho Vessel nếu cần
}