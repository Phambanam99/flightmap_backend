package com.phamnam.tracking_vessel_flight.service.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchUpdateScheduler {

    private final AircraftNotificationService notificationService;

    /**
     * Gửi cập nhật hàng loạt định kỳ (mỗi 5 giây) cho tất cả các khu vực
     */
    @Scheduled(fixedRate = 5000)
    public void sendPeriodicBatchUpdates() {
//        notificationService.sendBatchUpdatesToAllAreas();
    }
}