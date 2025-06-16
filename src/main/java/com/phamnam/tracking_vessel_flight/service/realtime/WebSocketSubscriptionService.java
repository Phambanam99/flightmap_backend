package com.phamnam.tracking_vessel_flight.service.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSubscriptionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final AircraftNotificationService aircraftNotificationService; // Thêm dependency này

    /**
     * Đăng ký client vào khu vực
     */

// Phương thức subscribeToArea cần được cập nhật để đảm bảo gửi response đúng

    public void subscribeToArea(String sessionId, float minLat, float maxLat, float minLon, float maxLon) {
        log.info("Processing area subscription for session {}: lat({} to {}), lon({} to {})",
                sessionId, minLat, maxLat, minLon, maxLon);

        try {
            String areaKey = String.format("area_%f_%f_%f_%f", minLat, maxLat, minLon, maxLon);
            log.debug("Generated areaKey: {}", areaKey);

            // Lưu subscriptions vào Redis
            redisTemplate.opsForSet().add("active:area:subscriptions", areaKey);
            redisTemplate.opsForSet().add("area:" + areaKey + ":clients", sessionId);
            redisTemplate.opsForSet().add("client:" + sessionId + ":subscriptions", areaKey);
            log.debug("Saved subscription info to Redis");

            // Tạo response message
            Map<String, Object> response = new HashMap<>();
            response.put("type", "area");
            response.put("status", "subscribed");
            response.put("areaKey", areaKey);
            response.put("key", areaKey);

            // Gửi message đến client
            log.info("Sending subscription confirmation to {}: {}", sessionId, response);
            messagingTemplate.convertAndSendToUser(
                    sessionId,              // username (sessionId)
                    "/queue/subscriptions", // destination
                    response                // payload
            );
            log.info("Confirmation sent to client");

            // Gửi dữ liệu ban đầu
            aircraftNotificationService.processNewAreaRequest(minLat, maxLat, minLon, maxLon, areaKey);
            log.info("Initial data sent for area {}", areaKey);
        } catch (Exception e) {
            log.error("Error in subscribeToArea: {}", e.getMessage(), e);
            throw e; // Rethrow to propagate to controller
        }
    }

    /**
     * Hủy đăng ký client khỏi khu vực
     */
    public void unsubscribeFromArea(String sessionId, double minLat, double maxLat, double minLon, double maxLon) {
        String areaKey = String.format("area_%f_%f_%f_%f", minLat, maxLat, minLon, maxLon);

        // Xóa subscription khỏi Redis
        redisTemplate.opsForSet().remove("area:" + areaKey + ":clients", sessionId);
        redisTemplate.opsForSet().remove("client:" + sessionId + ":subscriptions", areaKey);

        // Kiểm tra xem còn client nào đăng ký không
        Long clientCount = redisTemplate.opsForSet().size("area:" + areaKey + ":clients");
        if (clientCount != null && clientCount == 0) {
            redisTemplate.opsForSet().remove("active:area:subscriptions", areaKey);
        }

        // Gửi xác nhận hủy đăng ký thành công
        Map<String, Object> response = new HashMap<>();
        response.put("type", "area");
        response.put("status", "unsubscribed");
        response.put("areaKey", areaKey);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/subscriptions", response);
    }

    /**
     * Đăng ký client vào máy bay cụ thể
     */
    public void subscribeToAircraft(String sessionId, String hexIdent) {
        String aircraftKey = "aircraft_" + hexIdent;

        // Lưu subscription vào Redis
        redisTemplate.opsForSet().add("aircraft:" + hexIdent + ":clients", sessionId);
        redisTemplate.opsForSet().add("client:" + sessionId + ":aircraft:subscriptions", hexIdent);

        // Gửi xác nhận đăng ký thành công
        Map<String, Object> response = new HashMap<>();
        response.put("type", "aircraft");
        response.put("status", "subscribed");
        response.put("hexIdent", hexIdent);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/subscriptions", response);
    }

    /**
     * Hủy đăng ký client khỏi máy bay cụ thể
     */
    public void unsubscribeFromAircraft(String sessionId, String hexIdent) {
        // Xóa subscription khỏi Redis
        redisTemplate.opsForSet().remove("aircraft:" + hexIdent + ":clients", sessionId);
        redisTemplate.opsForSet().remove("client:" + sessionId + ":aircraft:subscriptions", hexIdent);

        // Gửi xác nhận hủy đăng ký thành công
        Map<String, Object> response = new HashMap<>();
        response.put("type", "aircraft");
        response.put("status", "unsubscribed");
        response.put("hexIdent", hexIdent);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/subscriptions", response);
    }

    /**
     * Xử lý khi client disconnect
     */
    public void handleDisconnect(String sessionId) {
        log.info("Xử lý disconnect cho client: {}", sessionId);

        // Xóa tất cả area subscriptions
        Set<Object> areaSubscriptions = redisTemplate.opsForSet().members("client:" + sessionId + ":subscriptions");
        if (areaSubscriptions != null) {
            for (Object sub : areaSubscriptions) {
                String areaKey = (String) sub;
                redisTemplate.opsForSet().remove("area:" + areaKey + ":clients", sessionId);

                // Nếu không còn client nào đăng ký area này
                Long clientCount = redisTemplate.opsForSet().size("area:" + areaKey + ":clients");
                if (clientCount != null && clientCount == 0) {
                    redisTemplate.opsForSet().remove("active:area:subscriptions", areaKey);
                }
            }
        }

        // Xóa tất cả aircraft subscriptions
        Set<Object> aircraftSubscriptions = redisTemplate.opsForSet()
                .members("client:" + sessionId + ":aircraft:subscriptions");
        if (aircraftSubscriptions != null) {
            for (Object sub : aircraftSubscriptions) {
                String hexIdent = (String) sub;
                redisTemplate.opsForSet().remove("aircraft:" + hexIdent + ":clients", sessionId);
            }
        }

        // Xóa các key client trong Redis
        redisTemplate.delete("client:" + sessionId + ":subscriptions");
        redisTemplate.delete("client:" + sessionId + ":aircraft:subscriptions");
    }
}