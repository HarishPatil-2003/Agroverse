package com.agroverse;

import com.agroverse.backend.service.GroqService;
import com.agroverse.backend.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ExternalIntegrationsTest {

    @Autowired
    private GroqService groqService;

    @Autowired
    private WeatherService weatherService;

    @Test
    public void testGroqPendingOrActiveStatus() {
        Map<String, Object> status = groqService.getStatus();
        assertNotNull(status);
        assertNotNull(status.get("status"));
        assertTrue(status.containsKey("configured"));
    }

    @Test
    public void testWeatherPendingOrActiveStatus() {
        Map<String, Object> status = weatherService.getStatus();
        assertNotNull(status);
        assertNotNull(status.get("status"));
        assertTrue(status.containsKey("configured"));
    }

    @Test
    public void testWeatherCoordinatesValidation() {
        // Invalid latitude > 90
        Map<String, Object> invalid = weatherService.getWeatherByCoords(999.0, 75.0);
        assertTrue(invalid.containsKey("error") && (Boolean) invalid.get("error"));
    }
}
