package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
import com.phamnam.tracking_vessel_flight.dto.request.FlightTrackingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AircraftNotificationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final TrackingCacheService trackingCacheService;

    /**
     * Gửi cập nhật máy bay đến clients đã đăng ký
     */
    public void sendAircraftUpdate(FlightTrackingRequestDTO tracking) {
        if (tracking.getId() == null)
            return;
        // 1. Gửi đến clients đã đăng ký máy bay cụ thể này
        messagingTemplate.convertAndSend("/topic/aircraft/" + tracking.getId(), tracking);
        // 2. Xác định khu vực chứa máy bay này
        if (tracking.getLatitude() == null || tracking.getLongitude() == null)
            return;
        // Lấy danh sách các khu vực đang được đăng ký
        Set<Object> activeAreas = redisTemplate.opsForSet().members("active:area:subscriptions");
        if (activeAreas == null || activeAreas.isEmpty())
            return;

        // Kiểm tra máy bay có nằm trong khu vực nào không
        for (Object areaObj : activeAreas) {
            String areaKey = (String) areaObj;

            // Parse area bounds từ key
            String[] parts = areaKey.substring(5).split("_"); // Loại bỏ "area_" prefix
            if (parts.length != 4)
                continue;

            try {
                double minLat = Double.parseDouble(parts[0]);
                double maxLat = Double.parseDouble(parts[1]);
                double minLon = Double.parseDouble(parts[2]);
                double maxLon = Double.parseDouble(parts[3]);

                // Kiểm tra máy bay có nằm trong khu vực này không
                if (tracking.getLatitude() >= minLat && tracking.getLatitude() <= maxLat &&
                        tracking.getLongitude() >= minLon && tracking.getLongitude() <= maxLon) {

                    // Gửi cập nhật đến topic của khu vực
                    messagingTemplate.convertAndSend("/topic/area/" + areaKey, tracking);
                }
            } catch (NumberFormatException e) {
                log.error("Lỗi khi parse area bounds: {}", areaKey, e);
            }
        }
    }

    /**
     * Gửi batch updates đến khu vực
     */
    public void sendBatchUpdate(String areaKey, List<FlightTrackingRequest> updates) {
        Map<String, Object> batchData = new HashMap<>();
        batchData.put("timestamp", new Date());
        batchData.put("updates", updates);

        messagingTemplate.convertAndSend("/topic/area/" + areaKey + "/batch", batchData);
    }

    public void sendBatchUpdate(String areaKey) {
        // Kiểm tra xem có client nào đăng ký khu vực này không
        Long clientCount = redisTemplate.opsForSet().size("area:" + areaKey + ":clients");

        if (clientCount == null || clientCount == 0) {
            return; // Không có client nào đăng ký
        }
        log.info("number of clients {}", clientCount);

        // Parse area bounds từ key
        String[] parts = areaKey.substring(5).split("_"); // Loại bỏ "area_" prefix
        log.info("areaKey {}", areaKey);
        if (parts.length != 4) {
            log.error("Invalid area key format: {}", areaKey);
            return;
        }

        try {
            List<FlightTrackingRequestDTO> updates = getFlightTrackingRequestDTOS(parts);
            log.info("updates {}", updates.size());
            // Nếu có dữ liệu, gửi cập nhật hàng loạt
            if (!updates.isEmpty()) {
                Map<String, Object> batchData = new HashMap<>();
                batchData.put("timestamp", new Date());
                batchData.put("updates", updates);
                batchData.put("count", updates.size());
                messagingTemplate.convertAndSend("/topic/area/" + areaKey + "/batch", batchData);
                log.debug("Sent batch update with {} aircraft to area {}", updates.size(), areaKey);
            }
        } catch (NumberFormatException e) {
            log.error("Error parsing area bounds: {}", areaKey, e);
        }
    }

    private List<FlightTrackingRequestDTO> getFlightTrackingRequestDTOS(String[] parts) {
        double minLat = Double.parseDouble(parts[0]);
        double maxLat = Double.parseDouble(parts[1]);
        double minLon = Double.parseDouble(parts[2]);
        double maxLon = Double.parseDouble(parts[3]);

        // Lấy tất cả các chuyến bay đang hoạt động (đã là FlightTrackingRequest
        // objects)
        Set<Object> activeFlights = trackingCacheService.getActiveFlights();
        // log.info("active flights {}", activeFlights);
        List<FlightTrackingRequestDTO> updates = new ArrayList<>();

        // Lọc các chuyến bay trong khu vực
        for (Object flightObj : activeFlights) {
            FlightTrackingRequestDTO flightData = (FlightTrackingRequestDTO) flightObj;

            // Kiểm tra máy bay có nằm trong khu vực không
            if (flightData != null && flightData.getLatitude() != null && flightData.getLongitude() != null) {
                if (flightData.getLatitude() >= minLat && flightData.getLatitude() <= maxLat &&
                        flightData.getLongitude() >= minLon && flightData.getLongitude() <= maxLon) {
                    updates.add(flightData);
                }
            }
        }
        return updates;
    }

    public void sendBatchUpdatesToAllAreas() {
        Set<Object> activeAreas = redisTemplate.opsForSet().members("active:area:subscriptions");
        if (activeAreas == null || activeAreas.isEmpty()) {
            return;
        }

        for (Object areaObj : activeAreas) {
            String areaKey = (String) areaObj;
            sendBatchUpdate(areaKey);
        }
    }

    public void processNewAreaRequest(float minLat, float maxLat, float minLon, float maxLon, String areaKey) {

        sendBatchUpdate(areaKey);
    }

}