package com.phamnam.tracking_vessel_flight.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.config.ExternalApiProperties;
import com.phamnam.tracking_vessel_flight.dto.request.ShipTrackingRequest;
import com.phamnam.tracking_vessel_flight.service.realtime.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalMarineDataService {

    private final ExternalApiProperties apiProperties;
    private final KafkaProducerService kafkaProducerService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelayString = "${external.api.marinetraffic.poll-interval:60000}")
    public void pullMarineTrafficData() {
        if (!apiProperties.isEnabled() || !apiProperties.getMarinetraffic().isEnabled()) {
            return;
        }

        try {
            log.info("🚢 Pulling marine data from MarineTraffic...");
            
            String url = buildMarineTrafficUrl();
            HttpHeaders headers = new HttpHeaders();
            if (!apiProperties.getMarinetraffic().getApiKey().isEmpty()) {
                headers.set("Authorization", "Bearer " + apiProperties.getMarinetraffic().getApiKey());
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<ShipTrackingRequest> ships = parseMarineTrafficResponse(response.getBody());
                publishShipsToKafka(ships);
                log.info("✅ Published {} ships from MarineTraffic to Kafka", ships.size());
            }
            
        } catch (Exception e) {
            log.error("❌ Error pulling MarineTraffic data: {}", e.getMessage());
        }
    }

    private String buildMarineTrafficUrl() {
        ExternalApiProperties.BoundsConfig bounds = apiProperties.getBounds();
        return String.format("%s/api/exportvessels/%s/v:2/MINLAT:%.2f/MAXLAT:%.2f/MINLON:%.2f/MAXLON:%.2f/protocol:jsono",
                apiProperties.getMarinetraffic().getBaseUrl(),
                apiProperties.getMarinetraffic().getApiKey(),
                bounds.getMinLatitude(),
                bounds.getMaxLatitude(),
                bounds.getMinLongitude(),
                bounds.getMaxLongitude());
    }

    private List<ShipTrackingRequest> parseMarineTrafficResponse(String jsonResponse) {
        List<ShipTrackingRequest> ships = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.get("data");
            
            if (data != null && data.isArray()) {
                for (JsonNode shipNode : data) {
                    try {
                        ShipTrackingRequest ship = mapMarineTrafficToDTO(shipNode);
                        if (ship != null && isWithinBounds(ship)) {
                            ships.add(ship);
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing ship data: {}", e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing MarineTraffic response: {}", e.getMessage());
        }
        
        return ships;
    }

    private ShipTrackingRequest mapMarineTrafficToDTO(JsonNode shipNode) {
        try {
            ShipTrackingRequest dto = new ShipTrackingRequest();
            
            // MarineTraffic API response fields
            dto.setVoyageId(shipNode.get("MMSI").asLong());
            dto.setLatitude(shipNode.get("LAT").asDouble());
            dto.setLongitude(shipNode.get("LON").asDouble());
            dto.setSpeed(shipNode.get("SPEED").asDouble());
            dto.setCourse(shipNode.get("COURSE").asDouble());
            dto.setDraught(shipNode.get("DRAUGHT").asDouble());
            dto.setTimestamp(LocalDateTime.now());
            
            return dto;
        } catch (Exception e) {
            log.warn("Error mapping MarineTraffic data: {}", e.getMessage());
            return null;
        }
    }

    private boolean isWithinBounds(ShipTrackingRequest ship) {
        ExternalApiProperties.BoundsConfig bounds = apiProperties.getBounds();
        return ship.getLatitude() >= bounds.getMinLatitude() &&
               ship.getLatitude() <= bounds.getMaxLatitude() &&
               ship.getLongitude() >= bounds.getMinLongitude() &&
               ship.getLongitude() <= bounds.getMaxLongitude();
    }

    private void publishShipsToKafka(List<ShipTrackingRequest> ships) {
        for (ShipTrackingRequest ship : ships) {
            try {
                kafkaProducerService.sendVesselTracking(ship);
            } catch (Exception e) {
                log.error("Error publishing ship {} to Kafka: {}", ship.getVoyageId(), e.getMessage());
            }
        }
    }
} 