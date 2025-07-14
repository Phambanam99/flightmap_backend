package com.phamnam.tracking_vessel_flight.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Custom deserializer for LocalDateTime that can handle array format
 * like [2025, 7, 13, 4, 22] or [2025, 7, 13, 4, 22, 30] or [2025, 7, 13, 4, 22, 30, 123]
 */
public class LocalDateTimeArrayDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        
        // If it's a string, try to parse it as ISO LocalDateTime
        if (node.isTextual()) {
            return LocalDateTime.parse(node.asText());
        }
        
        // If it's an array, parse it as [year, month, day, hour, minute, second, nano]
        if (node.isArray() && node.size() >= 5) {
            int year = node.get(0).asInt();
            int month = node.get(1).asInt();
            int day = node.get(2).asInt();
            int hour = node.get(3).asInt();
            int minute = node.get(4).asInt();
            int second = node.size() > 5 ? node.get(5).asInt() : 0;
            int nano = node.size() > 6 ? node.get(6).asInt() : 0;
            
            return LocalDateTime.of(year, month, day, hour, minute, second, nano);
        }
        
        throw new IOException("Unable to deserialize LocalDateTime from: " + node.toString());
    }
}
