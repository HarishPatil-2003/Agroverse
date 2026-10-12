package com.agroverse.backend.controller;

import com.agroverse.backend.service.CropDetectionService;
import com.agroverse.backend.service.GroqService;
import com.agroverse.backend.service.WeatherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final CropDetectionService cropDetectionService;
    private final GroqService groqService;
    private final WeatherService weatherService;

    @Value("${server.port:8080}")
    private int serverPort;

    public HealthController(
            CropDetectionService cropDetectionService,
            GroqService groqService,
            WeatherService weatherService
    ) {
        this.cropDetectionService = cropDetectionService;
        this.groqService = groqService;
        this.weatherService = weatherService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "AgroVerse Backend");
        response.put("port", serverPort);
        response.put("timestamp", Instant.now().toString());

        Map<String, Object> integrations = new HashMap<>();
        integrations.put("onnxEngine", cropDetectionService.isModelReady() ? "READY" : "OFFLINE");
        integrations.put("groqAssistant", groqService.isConfigured() ? "CONFIGURED" : "PENDING_CONFIGURATION");
        integrations.put("weatherProvider", weatherService.isConfigured() ? "CONFIGURED" : "PENDING_CONFIGURATION");

        response.put("integrations", integrations);
        return response;
    }
}
