package com.agroverse.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CachedAgroDataService {
    private static final Logger logger = LoggerFactory.getLogger(CachedAgroDataService.class);

    @Autowired
    private WeatherService weatherService;
    
    // In-Memory Caches to avoid redundant API calls
    private final Map<String, Object> marketCache = new ConcurrentHashMap<>();
    private final Map<String, Object> weatherCache = new ConcurrentHashMap<>();
    private static final List<String> FARMING_CITIES =
        List.of("Nashik", "Mumbai", "Pune", "Delhi", "Hyderabad", "Chennai", "Kolkata");

    // Fetch every 15 minutes (configured in properties)
    @Scheduled(fixedRateString = "${agroverse.cache.market-refresh-ms}")
    public void refreshMarketData() {
        try {
            logger.info("Scheduled Task: Refreshing Market Data cache...");
            marketCache.put("wheat", Map.of("price", 2250, "trend", "+2.5%"));
            marketCache.put("rice", Map.of("price", 3100, "trend", "-1.0%"));
            marketCache.put("tomato", Map.of("price", 40, "trend", "stable"));
            logger.info("Market data cache refreshed successfully.");
        } catch (Exception e) {
            logger.error("Failed to refresh market data cache", e);
        }
    }

    @Scheduled(fixedRateString = "${agroverse.cache.weather-refresh-ms}", initialDelay = 5000)
    public void refreshWeatherData() {
        try {
            logger.info("Scheduled Task: Refreshing Weather Data cache from OpenWeatherMap...");
            for (String city : FARMING_CITIES) {
                Map<String, Object> data = weatherService.getWeatherForCity(city);
                weatherCache.put(city.toLowerCase(), data);
            }
            logger.info("Weather data cache refreshed successfully for {} cities.", FARMING_CITIES.size());
        } catch (Exception e) {
            logger.error("Failed to refresh weather data cache", e);
        }
    }

    public Map<String, Object> getMarketData() {
        if (marketCache.isEmpty()) refreshMarketData();
        return marketCache;
    }

    public Map<String, Object> getWeatherData() {
        if (weatherCache.isEmpty()) refreshWeatherData();
        return weatherCache;
    }
}
