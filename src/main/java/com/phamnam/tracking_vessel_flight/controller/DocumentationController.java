package com.phamnam.tracking_vessel_flight.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class DocumentationController {

    @GetMapping("/asyncapi")
    public ResponseEntity<String> getAsyncApiSpec() {
        try {
            ClassPathResource resource = new ClassPathResource("asyncapi.yaml");
            if (resource.exists()) {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.valueOf("application/yaml"))
                        .body(content);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("AsyncAPI specification file not found");
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading AsyncAPI specification: " + e.getMessage());
        }
    }

    @GetMapping("/asyncapi-ui")
    public ResponseEntity<String> getAsyncApiUi() {
        try {
            ClassPathResource resource = new ClassPathResource("static/asyncapi-ui.html");
            if (resource.exists()) {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(content);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("AsyncAPI UI not found");
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error loading AsyncAPI UI: " + e.getMessage());
        }
    }

    @GetMapping("/docs")
    public ResponseEntity<String> getDocsInfo() {
        String docsInfo = """
            {
              "title": "Flight & Vessel Tracking API Documentation",
              "version": "2.0.0",
              "endpoints": {
                "rest_api": "http://localhost:9090/swagger-ui.html",
                "rest_spec": "http://localhost:9090/v3/api-docs", 
                "async_api": "http://localhost:9090/asyncapi-ui",
                "async_spec": "http://localhost:9090/asyncapi"
              },
              "description": "Comprehensive API documentation for real-time flight and vessel tracking",
              "features": [
                "REST API with OpenAPI 3.0",
                "WebSocket real-time updates",
                "Kafka event streaming",
                "Geospatial queries",
                "JWT authentication"
              ]
            }
            """;
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(docsInfo);
    }
} 