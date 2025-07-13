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
            String areaKey = String.format("area_%.6f_%.6f_%.6f_%.6f", minLat, maxLat, minLon, maxLon);
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
                    sessionId, // username (sessionId)
                    "/queue/subscriptions", // destination
                    response // payload
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
        String areaKey = String.format("area_%.6f_%.6f_%.6f_%.6f", minLat, maxLat, minLon, maxLon);

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

    // =============== SHIP METHODS FOR INTELLIGENT SERVICES ===============

    /**
     * Subscribe to ship area updates
     */
    public void subscribeToShipArea(String sessionId,
            com.phamnam.tracking_vessel_flight.dto.request.AreaSubscriptionRequest request) {
        log.info("Processing ship area subscription for session {}: lat({} to {}), lon({} to {})",
                sessionId, request.getMinLatitude(), request.getMaxLatitude(),
                request.getMinLongitude(), request.getMaxLongitude());

        try {
            String areaKey = String.format("ship_area_%.6f_%.6f_%.6f_%.6f",
                    request.getMinLatitude(), request.getMaxLatitude(),
                    request.getMinLongitude(), request.getMaxLongitude());

            // Save subscriptions to Redis
            redisTemplate.opsForSet().add("active:ship:area:subscriptions", areaKey);
            redisTemplate.opsForSet().add("ship:area:" + areaKey + ":clients", sessionId);
            redisTemplate.opsForSet().add("client:" + sessionId + ":ship:area:subscriptions", areaKey);

            // Send confirmation
            Map<String, Object> response = new HashMap<>();
            response.put("type", "ship_area");
            response.put("status", "subscribed");
            response.put("areaKey", areaKey);

            messagingTemplate.convertAndSendToUser(sessionId, "/queue/ship/subscriptions", response);
            log.info("Ship area subscription confirmed for session {}", sessionId);

        } catch (Exception e) {
            log.error("Error in subscribeToShipArea: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Unsubscribe from ship area updates
     */
    public void unsubscribeFromShipArea(String sessionId,
            com.phamnam.tracking_vessel_flight.dto.request.AreaSubscriptionRequest request) {
        String areaKey = String.format("ship_area_%.6f_%.6f_%.6f_%.6f",
                request.getMinLatitude(), request.getMaxLatitude(),
                request.getMinLongitude(), request.getMaxLongitude());

        // Remove subscription from Redis
        redisTemplate.opsForSet().remove("ship:area:" + areaKey + ":clients", sessionId);
        redisTemplate.opsForSet().remove("client:" + sessionId + ":ship:area:subscriptions", areaKey);

        // Check if any clients still subscribed
        Long clientCount = redisTemplate.opsForSet().size("ship:area:" + areaKey + ":clients");
        if (clientCount != null && clientCount == 0) {
            redisTemplate.opsForSet().remove("active:ship:area:subscriptions", areaKey);
        }

        // Send confirmation
        Map<String, Object> response = new HashMap<>();
        response.put("type", "ship_area");
        response.put("status", "unsubscribed");
        response.put("areaKey", areaKey);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/ship/subscriptions", response);
    }

    /**
     * Subscribe to specific ship updates
     */
    public void subscribeToShip(String sessionId, String mmsi) {
        // Save subscription to Redis
        redisTemplate.opsForSet().add("ship:" + mmsi + ":clients", sessionId);
        redisTemplate.opsForSet().add("client:" + sessionId + ":ship:subscriptions", mmsi);

        // Send confirmation
        Map<String, Object> response = new HashMap<>();
        response.put("type", "ship");
        response.put("status", "subscribed");
        response.put("mmsi", mmsi);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/ship/subscriptions", response);
    }

    /**
     * Unsubscribe from specific ship updates
     */
    public void unsubscribeFromShip(String sessionId, String mmsi) {
        // Remove subscription from Redis
        redisTemplate.opsForSet().remove("ship:" + mmsi + ":clients", sessionId);
        redisTemplate.opsForSet().remove("client:" + sessionId + ":ship:subscriptions", mmsi);

        // Send confirmation
        Map<String, Object> response = new HashMap<>();
        response.put("type", "ship");
        response.put("status", "unsubscribed");
        response.put("mmsi", mmsi);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/ship/subscriptions", response);
    }

    /**
     * Send ship area update to subscribers
     */
    public void sendShipAreaUpdate(String sessionId,
            java.util.List<com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest> ships) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "ship_area_update");
        message.put("ships", ships);
        message.put("timestamp", java.time.LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/ship/updates", message);
        log.debug("Ship area update sent to session: {}", sessionId);
    }

    /**
     * Send individual ship update
     */
    public void sendShipUpdate(String sessionId,
            com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest shipData) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "ship_update");
        message.put("ship", shipData);
        message.put("timestamp", java.time.LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/ship/updates", message);
        log.debug("Ship update sent to session: {}", sessionId);
    }

    /**
     * Send ship history data
     */
    public void sendShipHistory(String sessionId, String mmsi, Map<String, Object> historyData) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "ship_history");
        message.put("mmsi", mmsi);
        message.put("data", historyData);
        message.put("timestamp", java.time.LocalDateTime.now());

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/ship/history", message);
        log.debug("Ship history sent to session: {} for MMSI: {}", sessionId, mmsi);
    }

    /**
     * Get ship subscribers for MMSI
     */
    public java.util.List<String> getShipSubscribers(String mmsi) {
        Set<Object> subscribers = redisTemplate.opsForSet().members("ship:" + mmsi + ":clients");
        return subscribers != null
                ? subscribers.stream().map(Object::toString).collect(java.util.stream.Collectors.toList())
                : java.util.Collections.emptyList();
    }

    /**
     * Get ship area subscribers
     */
    public java.util.List<String> getShipAreaSubscribers(Double latitude, Double longitude) {
        // Find area subscriptions that contain this lat/lon
        Set<Object> activeAreas = redisTemplate.opsForSet().members("active:ship:area:subscriptions");
        java.util.List<String> allSubscribers = new java.util.ArrayList<>();

        if (activeAreas != null) {
            for (Object areaObj : activeAreas) {
                String areaKey = (String) areaObj;
                // Parse area bounds and check if position is within
                if (isPositionInArea(latitude, longitude, areaKey)) {
                    Set<Object> areaClients = redisTemplate.opsForSet().members("ship:area:" + areaKey + ":clients");
                    if (areaClients != null) {
                        allSubscribers.addAll(areaClients.stream().map(Object::toString)
                                .collect(java.util.stream.Collectors.toList()));
                    }
                }
            }
        }

        return allSubscribers;
    }

    private boolean isPositionInArea(Double latitude, Double longitude, String areaKey) {
        try {
            // Parse area key: "ship_area_minLat_maxLat_minLon_maxLon"
            String[] parts = areaKey.split("_");
            if (parts.length >= 6) {
                double minLat = Double.parseDouble(parts[2]);
                double maxLat = Double.parseDouble(parts[3]);
                double minLon = Double.parseDouble(parts[4]);
                double maxLon = Double.parseDouble(parts[5]);

                return latitude >= minLat && latitude <= maxLat &&
                        longitude >= minLon && longitude <= maxLon;
            }
        } catch (Exception e) {
            log.error("Error parsing area key {}: {}", areaKey, e.getMessage());
        }
        return false;
    }
}