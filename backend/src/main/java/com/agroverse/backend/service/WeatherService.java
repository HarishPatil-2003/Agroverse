package com.agroverse.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    @Value("${agroverse.weather.api-key:${openweather.api.key:${OPENWEATHER_API_KEY:}}}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private static final String OWM_BASE = "https://api.openweathermap.org/data/2.5/weather";

    // Memory cache with TTL (15 minutes = 900,000 ms)
    private final Map<String, CacheEntry> weatherCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 15 * 60 * 1000;

    public WeatherService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    private static class CacheEntry {
        final Map<String, Object> data;
        final long timestamp;

        CacheEntry(Map<String, Object> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > CACHE_TTL_MS;
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("your_") && !apiKey.startsWith("<");
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean configured = isConfigured();
        status.put("configured", configured);
        status.put("provider", "OpenWeatherMap");
        status.put("status", configured ? "CONFIGURED" : "PENDING_CONFIGURATION");
        status.put("message", configured
                ? "Weather provider credentials configured."
                : "Weather provider is pending configuration. Set OPENWEATHER_API_KEY to activate live meteorological data.");
        return status;
    }

    public Map<String, Object> getWeatherForCity(String city) {
        String cleanCity = (city != null && !city.trim().isEmpty()) ? city.trim() : "Nashik";
        String cacheKey = "city:" + cleanCity.toLowerCase();

        CacheEntry cached = weatherCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        if (!isConfigured()) {
            Map<String, Object> pending = pendingConfiguration(cleanCity);
            weatherCache.put(cacheKey, new CacheEntry(pending));
            return pending;
        }

        try {
            String url = String.format("%s?q=%s,IN&appid=%s&units=metric", OWM_BASE, cleanCity, apiKey.trim());
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = restTemplate.getForObject(url, Map.class);
            Map<String, Object> normalized = normalize(raw, cleanCity, true);
            weatherCache.put(cacheKey, new CacheEntry(normalized));
            return normalized;
        } catch (HttpClientErrorException.Unauthorized e) {
            logger.warn("OpenWeatherMap API key invalid or pending activation for city '{}'", cleanCity);
            Map<String, Object> pending = pendingActivation(cleanCity);
            weatherCache.put(cacheKey, new CacheEntry(pending));
            return pending;
        } catch (Exception e) {
            logger.warn("Weather API request failed for '{}': {}", cleanCity, e.getMessage());
            if (cached != null) {
                Map<String, Object> stale = new HashMap<>(cached.data);
                stale.put("stale", true);
                stale.put("warning", "Showing cached observation; live provider unreachable.");
                return stale;
            }
            return unavailable(cleanCity, e.getMessage());
        }
    }

    public Map<String, Object> getWeatherByCoords(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", true);
            err.put("message", "Invalid GPS coordinates.");
            return err;
        }

        String cacheKey = String.format(Locale.US, "coords:%.2f:%.2f", lat, lon);
        CacheEntry cached = weatherCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        if (!isConfigured()) {
            return pendingConfiguration("Current GPS Location");
        }

        try {
            String url = String.format(Locale.US, "%s?lat=%.4f&lon=%.4f&appid=%s&units=metric", OWM_BASE, lat, lon, apiKey.trim());
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = restTemplate.getForObject(url, Map.class);
            Map<String, Object> normalized = normalize(raw, null, true);
            weatherCache.put(cacheKey, new CacheEntry(normalized));
            return normalized;
        } catch (Exception e) {
            logger.warn("Weather API request failed for coords {},{}: {}", lat, lon, e.getMessage());
            return unavailable("GPS Location", e.getMessage());
        }
    }

    public Map<String, Object> getAllCitiesWeather() {
        List<String> cities = List.of("Nashik", "Mumbai", "Pune", "Delhi", "Hyderabad", "Chennai", "Kolkata");
        Map<String, Object> result = new HashMap<>();
        for (String city : cities) {
            result.put(city.toLowerCase(), getWeatherForCity(city));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalize(Map<String, Object> raw, String cityHint, boolean isLive) {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("city", raw.getOrDefault("name", cityHint != null ? cityHint : "Unknown"));
            result.put("status", "ACTIVE");
            result.put("isLive", isLive);
            result.put("observationTime", Instant.now().toString());

            Map<String, Object> main = (Map<String, Object>) raw.get("main");
            if (main != null) {
                result.put("temp", Math.round(((Number) main.get("temp")).doubleValue()));
                result.put("feels_like", Math.round(((Number) main.get("feels_like")).doubleValue()));
                result.put("humidity", main.get("humidity"));
                result.put("temp_min", Math.round(((Number) main.get("temp_min")).doubleValue()));
                result.put("temp_max", Math.round(((Number) main.get("temp_max")).doubleValue()));
            }

            Map<String, Object> wind = (Map<String, Object>) raw.get("wind");
            if (wind != null && wind.containsKey("speed")) {
                result.put("wind_speed", Math.round(((Number) wind.get("speed")).doubleValue() * 3.6));
            }

            List<Map<String, Object>> weatherList = (List<Map<String, Object>>) raw.get("weather");
            if (weatherList != null && !weatherList.isEmpty()) {
                result.put("condition", weatherList.get(0).get("main"));
                result.put("description", weatherList.get(0).get("description"));
                result.put("icon", "https://openweathermap.org/img/wn/" + weatherList.get(0).get("icon") + "@2x.png");
            }

            Map<String, Object> sys = (Map<String, Object>) raw.get("sys");
            if (sys != null) {
                result.put("sunrise", sys.get("sunrise"));
                result.put("sunset", sys.get("sunset"));
            }

            if (raw.containsKey("visibility")) {
                result.put("visibility", Math.round(((Number) raw.get("visibility")).doubleValue() / 1000.0));
            }

        } catch (Exception e) {
            logger.error("Error normalizing weather data: {}", e.getMessage());
            return unavailable(cityHint != null ? cityHint : "Unknown", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> pendingConfiguration(String city) {
        Map<String, Object> m = new HashMap<>();
        m.put("city", city);
        m.put("temp", "--");
        m.put("feels_like", "--");
        m.put("humidity", "--");
        m.put("wind_speed", "--");
        m.put("condition", "Pending");
        m.put("description", "Weather integration pending configuration (OPENWEATHER_API_KEY)");
        m.put("status", "pending_configuration");
        m.put("isLive", false);
        m.put("icon", null);
        return m;
    }

    private Map<String, Object> pendingActivation(String city) {
        Map<String, Object> m = new HashMap<>();
        m.put("city", city);
        m.put("temp", "--");
        m.put("feels_like", "--");
        m.put("humidity", "--");
        m.put("wind_speed", "--");
        m.put("condition", "Activating");
        m.put("description", "API key newly generated; activating on OpenWeatherMap (usually up to 2h)");
        m.put("status", "pending_activation");
        m.put("isLive", false);
        m.put("icon", null);
        return m;
    }

    private Map<String, Object> unavailable(String city, String reason) {
        Map<String, Object> m = new HashMap<>();
        m.put("city", city);
        m.put("temp", "--");
        m.put("feels_like", "--");
        m.put("humidity", "--");
        m.put("wind_speed", "--");
        m.put("condition", "Unavailable");
        m.put("description", "Weather observations temporarily unavailable: " + (reason != null ? reason : "Unknown error"));
        m.put("error", true);
        m.put("status", "UNAVAILABLE");
        m.put("isLive", false);
        return m;
    }
}
