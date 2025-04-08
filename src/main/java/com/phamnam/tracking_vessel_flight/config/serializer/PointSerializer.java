package com.phamnam.tracking_vessel_flight.config.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.locationtech.jts.geom.Point;

import java.io.IOException;

/**
 * Custom serializer for JTS Point objects to prevent infinite recursion
 */
public class PointSerializer extends JsonSerializer<Point> {

    @Override
    public void serialize(Point point, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        if (point == null) {
            jsonGenerator.writeNull();
            return;
        }

        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField("longitude", point.getX());
        jsonGenerator.writeNumberField("latitude", point.getY());
        jsonGenerator.writeNumberField("srid", point.getSRID());
        jsonGenerator.writeEndObject();
    }
}
