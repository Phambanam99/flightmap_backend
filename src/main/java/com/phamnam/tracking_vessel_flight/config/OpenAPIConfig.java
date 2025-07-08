package com.phamnam.tracking_vessel_flight.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI trackingAPI() {
        final String securitySchemeName = "bearerAuth";
        
        // Development Server
        Server devServer = new Server();
        devServer.setUrl("http://localhost:9090");
        devServer.setDescription("Development server");

        // Production Server
        Server prodServer = new Server();
        prodServer.setUrl("https://api.tracking.example.com");
        prodServer.setDescription("Production server");

        Contact contact = new Contact();
        contact.setEmail("phambanam99@gmail.com");
        contact.setName("Flight & Vessel Tracking API Support");
        contact.setUrl("https://github.com/Phambanam99/flightmap_backend");

        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0");

        Info info = new Info()
                .title("Real-time Vessel & Flight Tracking API")  
                .version("2.0.0")
                .contact(contact)
                .description("Comprehensive REST API for real-time tracking of vessels and flights with WebSocket and Kafka integration")
                .license(license);

        ExternalDocumentation externalDocs = new ExternalDocumentation()
                .description("Project GitHub Repository")
                .url("https://github.com/Phambanam99/flightmap_backend");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .externalDocs(externalDocs)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token for API authentication")))
                .addTagsItem(new Tag()
                        .name("Authentication")
                        .description("User authentication and JWT token management"))
                .addTagsItem(new Tag()
                        .name("Flight Tracking")
                        .description("Real-time flight tracking and historical data"))
                .addTagsItem(new Tag()
                        .name("Vessel Tracking")
                        .description("Ship and vessel tracking operations"))
                .addTagsItem(new Tag()
                        .name("Aircraft Management")
                        .description("Aircraft information and monitoring"))
                .addTagsItem(new Tag()
                        .name("Ship Management")
                        .description("Ship and vessel information management"))
                .addTagsItem(new Tag()
                        .name("Monitoring")
                        .description("Real-time monitoring dashboards and analytics"))
                .addTagsItem(new Tag()
                        .name("Archive")
                        .description("Cold storage and historical data management"))
                .addTagsItem(new Tag()
                        .name("WebSocket")
                        .description("Real-time WebSocket connections and subscriptions"))
                .addTagsItem(new Tag()
                        .name("Async Events")
                        .description("Asynchronous event publishing and streaming"));
    }
}
