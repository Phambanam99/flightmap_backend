package com.phamnam.tracking_vessel_flight.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@Tag(name = "AsyncAPI Documentation", description = "Endpoints for AsyncAPI documentation")
public class AsyncApiController {

    private final ResourceLoader resourceLoader;

    @Value("${asyncapi.server.url:ws://localhost:9090/ws}")
    private String serverUrl;

    @GetMapping("/asyncapi-ui")
    @Operation(summary = "AsyncAPI UI", description = "Displays the AsyncAPI documentation UI")
    @ResponseBody
    public void asyncApiUi(jakarta.servlet.http.HttpServletResponse response) throws IOException {
        response.sendRedirect("/asyncapi-ui.html");
    }

    @GetMapping("/asyncapi")
    @ResponseBody
    @Operation(summary = "AsyncAPI Specification", description = "Returns the AsyncAPI specification file")
    public String getAsyncApiSpec() throws IOException {
        try {
            Resource resource = resourceLoader.getResource("classpath:asyncapi.yaml");
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                throw new IOException("AsyncAPI specification file not found");
            }
        } catch (Exception e) {
            throw new IOException("Error reading AsyncAPI specification: " + e.getMessage());
        }
    }
}
