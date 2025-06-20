package com.phamnam.tracking_vessel_flight.service.realtime;

import com.phamnam.tracking_vessel_flight.models.*;
import com.phamnam.tracking_vessel_flight.models.enums.AlertPriority;
import com.phamnam.tracking_vessel_flight.models.enums.AlertStatus;
import com.phamnam.tracking_vessel_flight.models.enums.EntityType;
import com.phamnam.tracking_vessel_flight.models.enums.RuleType;
import com.phamnam.tracking_vessel_flight.repository.AlertEventRepository;
import com.phamnam.tracking_vessel_flight.repository.AlertRuleRepository;
import com.phamnam.tracking_vessel_flight.service.kafka.TrackingKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertRuleEngine {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertEventRepository alertEventRepository;
    private final TrackingKafkaProducer kafkaProducer;
    private final WebSocketService webSocketService;

    // Cache for rule evaluation to improve performance
    private final Map<String, Object> ruleCache = new ConcurrentHashMap<>();

    // ============================================================================
    // ALERT MANAGEMENT
    // ============================================================================

    private boolean isDuplicateAlert(AlertRule rule, EntityType entityType, String entityId) {
        LocalDateTime recentThreshold = LocalDateTime.now().minusMinutes(5); // Check last 5 minutes

        return alertEventRepository.existsByAlertRuleAndEntityTypeAndEntityIdAndEventTimeAfterAndStatus(
                rule, entityType, entityId, recentThreshold, AlertStatus.ACTIVE);
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    private List<AlertRule> getActiveRulesForEntityType(EntityType entityType) {
        String cacheKey = "rules_" + entityType.name();

        // Check cache first
        @SuppressWarnings("unchecked")
        List<AlertRule> cachedRules = (List<AlertRule>) ruleCache.get(cacheKey);

        if (cachedRules == null) {
            cachedRules = alertRuleRepository.findByIsEnabledTrueAndEntityTypeOrderByPriorityDesc(entityType);
            ruleCache.put(cacheKey, cachedRules);
        }

        return cachedRules;
    }

    // Clear cache when rules are updated
    public void clearRuleCache() {
        ruleCache.clear();
        log.debug("Alert rule cache cleared");
    }

    // ============================================================================
    // MANUAL ALERT MANAGEMENT
    // ============================================================================

    @Transactional
    public AlertEvent createManualAlert(EntityType entityType, String entityId,
            AlertPriority priority, String message,
            Double latitude, Double longitude) {
        AlertEvent alertEvent = AlertEvent.builder()
                .entityType(entityType)
                .entityId(entityId)
                .priority(priority)
                .alertMessage(message)
                .latitude(latitude)
                .longitude(longitude)
                .eventTime(LocalDateTime.now())
                .status(AlertStatus.ACTIVE)
                .build();

        alertEvent = alertEventRepository.save(alertEvent);

        // Broadcast the manual alert
        kafkaProducer.sendAlert(alertEvent);
        webSocketService.broadcastAlert(alertEvent);

        log.info("Created manual {} priority alert for {} {}: {}",
                priority, entityType, entityId, message);

        return alertEvent;
    }

    @Transactional
    public void resolveAlert(Long alertId, String resolution) {
        alertEventRepository.findById(alertId).ifPresent(alert -> {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolution(resolution);
            alert.setResolvedTime(LocalDateTime.now());
            alertEventRepository.save(alert);

            log.info("Resolved alert {}: {}", alertId, resolution);
        });
    }

    public List<AlertEvent> getActiveAlerts() {
        return alertEventRepository.findByStatusOrderByEventTimeDesc(AlertStatus.ACTIVE);
    }

    public List<AlertEvent> getAlertsByEntity(EntityType entityType, String entityId) {
        return alertEventRepository.findByEntityTypeAndEntityIdOrderByEventTimeDesc(entityType, entityId);
    }
}