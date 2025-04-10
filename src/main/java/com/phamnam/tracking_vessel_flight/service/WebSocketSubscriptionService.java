package com.phamnam.tracking_vessel_flight.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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

    /**
     * Đăng ký client vào khu vực
     */
    public void subscribeToArea(String sessionId, double minLat, double maxLat, double minLon, double maxLon) {
        String areaKey = String.format("area_%f_%f_%f_%f", minLat, maxLat, minLon, maxLon);

        // Lưu subscriptions vào Redis
        redisTemplate.opsForSet().add("active:area:subscriptions", areaKey);
        redisTemplate.opsForSet().add("area:" + areaKey + ":clients", sessionId);
        redisTemplate.opsForSet().add("client:" + sessionId + ":subscriptions", areaKey);

        // Gửi xác nhận đăng ký thành công
        Map<String, Object> response = new HashMap<>();
        response.put("type", "area");
        response.put("status", "subscribed");
        response.put("key", areaKey);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/subscriptions", response);
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
        response.put("key", areaKey);
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
        Set<Object> aircraftSubscriptions = redisTemplate.opsForSet().members("client:" + sessionId + ":aircraft:subscriptions");
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