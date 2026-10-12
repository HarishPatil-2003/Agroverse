package com.agroverse.backend.controller;

import com.agroverse.backend.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWeather(
            @RequestParam(defaultValue = "Nashik") String city) {
        return ResponseEntity.ok(weatherService.getWeatherForCity(city));
    }

    @GetMapping("/coords")
    public ResponseEntity<Map<String, Object>> getWeatherByCoords(
            @RequestParam double lat,
            @RequestParam double lon) {
        return ResponseEntity.ok(weatherService.getWeatherByCoords(lat, lon));
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllCitiesWeather() {
        return ResponseEntity.ok(weatherService.getAllCitiesWeather());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(weatherService.getStatus());
    }
}
