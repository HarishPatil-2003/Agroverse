package com.agroverse.backend.controller;

import com.agroverse.backend.service.CachedAgroDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class AgroDataController {
    private static final Logger logger = LoggerFactory.getLogger(AgroDataController.class);
    private final CachedAgroDataService cachedAgroDataService;

    public AgroDataController(CachedAgroDataService cachedAgroDataService) {
        this.cachedAgroDataService = cachedAgroDataService;
    }

    @GetMapping("/market")
    public ResponseEntity<Map<String, Object>> getMarketData() {
        logger.info("Serving market data from cache (0 external API calls)");
        return ResponseEntity.ok(cachedAgroDataService.getMarketData());
    }

    @GetMapping("/weather")
    public ResponseEntity<Map<String, Object>> getWeatherData() {
        logger.info("Serving weather data from cache (0 external API calls)");
        return ResponseEntity.ok(cachedAgroDataService.getWeatherData());
    }
}
