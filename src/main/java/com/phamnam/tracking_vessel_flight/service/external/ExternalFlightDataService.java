package com.phamnam.tracking_vessel_flight.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phamnam.tracking_vessel_flight.config.ExternalApiProperties;
import com.phamnam.tracking_vessel_flight.dto.FlightTrackingRequestDTO;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalFlightDataService {

    private final ExternalApiProperties apiProperties;
    private final KafkaProducerService kafkaProducerService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelayString = "${external.api.flightradar24.poll-interval:30000}")
    public void pullFlightRadar24Data() {
        if (!apiProperties.isEnabled() || !apiProperties.getFlightradar24().isEnabled()) {
            return;
        }

        try {
            log.info("🛫 Pulling flight data from FlightRadar24...");
            
            String url = buildFlightRadar24Url();
            HttpHeaders headers = new HttpHeaders();
            if (!apiProperties.getFlightradar24().getApiKey().isEmpty()) {
                headers.set("Authorization", "Bearer " + apiProperties.getFlightradar24().getApiKey());
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<FlightTrackingRequestDTO> flights = parseFlightRadar24Response(response.getBody());
                publishFlightsToKafka(flights);
                log.info("✅ Published {} flights from FlightRadar24 to Kafka", flights.size());
            }
            
        } catch (Exception e) {
            log.error("❌ Error pulling FlightRadar24 data: {}", e.getMessage());
        }
    }

    private String buildFlightRadar24Url() {
        ExternalApiProperties.BoundsConfig bounds = apiProperties.getBounds();
        return String.format("%s/zones/fcgi/feed.js?bounds=%.2f,%.2f,%.2f,%.2f&faa=1&satellite=1&mlat=1&flarm=1&adsb=1&gnd=1&air=1&vehicles=1&estimated=1&maxage=14400&gliders=1&stats=1",
                apiProperties.getFlightradar24().getBaseUrl(),
                bounds.getMaxLatitude(),
                bounds.getMinLatitude(), 
                bounds.getMinLongitude(),
                bounds.getMaxLongitude());
    }

    private List<FlightTrackingRequestDTO> parseFlightRadar24Response(String jsonResponse) {
        List<FlightTrackingRequestDTO> flights = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            root.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode flightData = entry.getValue();
                
                // Skip metadata fields
                if (key.equals("full_count") || key.equals("version") || !flightData.isArray()) {
                    return;
                }
                
                try {
                    FlightTrackingRequestDTO flight = mapFlightRadar24ToDTO(key, flightData);
                    if (flight != null && isWithinBounds(flight)) {
                        flights.add(flight);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing flight data for key {}: {}", key, e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Error parsing FlightRadar24 response: {}", e.getMessage());
        }
        
        return flights;
    }

    private FlightTrackingRequestDTO mapFlightRadar24ToDTO(String hexident, JsonNode flightArray) {
        if (flightArray.size() < 16) {
            return null; // Insufficient data
        }
        
        try {
            FlightTrackingRequestDTO dto = new FlightTrackingRequestDTO();
            dto.setHexident(hexident);
            dto.setLatitude(flightArray.get(1).asDouble());
            dto.setLongitude(flightArray.get(2).asDouble());
            dto.setHeading(flightArray.get(3).asInt());
            dto.setAltitude(flightArray.get(4).asInt());
            dto.setSpeed(flightArray.get(5).asInt());
            dto.setSquawk(flightArray.get(6).asText());
            // FlightRadar24 specific: index 8 is aircraft type, 9 is registration, 11 is origin, 12 is destination
            dto.setType(flightArray.get(8).asText());
            dto.setCallsign(flightArray.get(16).asText()); // Callsign is at index 16
            dto.setVerticalSpeed(flightArray.get(15).asInt());
            
            // Set current time as update time
            dto.setUpdateTime(LocalDateTime.now());
            dto.setUnixTime(System.currentTimeMillis() / 1000);
            
            // Generate unique ID based on hexident and timestamp
            dto.setId(Math.abs((hexident + System.currentTimeMillis()).hashCode()) % 999999999L);
            
            return dto;
        } catch (Exception e) {
            log.warn("Error mapping FlightRadar24 data: {}", e.getMessage());
            return null;
        }
    }

    private boolean isWithinBounds(FlightTrackingRequestDTO flight) {
        ExternalApiProperties.BoundsConfig bounds = apiProperties.getBounds();
        return flight.getLatitude() >= bounds.getMinLatitude() &&
               flight.getLatitude() <= bounds.getMaxLatitude() &&
               flight.getLongitude() >= bounds.getMinLongitude() &&
               flight.getLongitude() <= bounds.getMaxLongitude();
    }

    private void publishFlightsToKafka(List<FlightTrackingRequestDTO> flights) {
        for (FlightTrackingRequestDTO flight : flights) {
            try {
                kafkaProducerService.sendFlightTracking(flight);
            } catch (Exception e) {
                log.error("Error publishing flight {} to Kafka: {}", flight.getCallsign(), e.getMessage());
            }
        }
    }
} 