package com.agroverse.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MarketRateService {

    private final RestTemplate restTemplate;

    @Value("${gov.data.api.key:}")
    private String apiKey;

    private static final String RESOURCE_ID = "9ef273bd-a4ef-4b31-bc59-0b1a08327ecc";
    private static final String BASE_URL = "https://api.data.gov.in/resource/";

    public MarketRateService() {
        this.restTemplate = new RestTemplate();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getLiveRates() {
        log.info("Fetching live market rates from Agmarknet...");
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gov Data API Key is missing. Using fallback mock data.");
            return getFallbackRates();
        }

        try {
            String url = String.format("%s%s?api-key=%s&format=json&limit=50", BASE_URL, RESOURCE_ID, apiKey);
            
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> response = responseEntity.getBody();

            if (response == null || !response.containsKey("records")) {
                log.warn("No records found in Agmarknet response. Using fallback.");
                return getFallbackRates();
            }

            List<Map<String, Object>> records = (List<Map<String, Object>>) response.get("records");
            
            if (records == null || records.isEmpty()) {
                return getFallbackRates();
            }

            return records.stream()
                    .map(r -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("commodity", r.getOrDefault("commodity", "N/A"));
                        map.put("market", r.getOrDefault("market", "N/A"));
                        map.put("state", r.getOrDefault("state", "N/A"));
                        map.put("district", r.getOrDefault("district", "N/A"));
                        map.put("minPrice", r.getOrDefault("min_price", "0"));
                        map.put("maxPrice", r.getOrDefault("max_price", "0"));
                        map.put("modalPrice", r.getOrDefault("modal_price", "0"));
                        map.put("date", r.getOrDefault("arrival_date", "N/A"));
                        return map;
                    })
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to fetch market rates: {}. Using fallback.", e.getMessage());
            return getFallbackRates();
        }
    }

    private List<Map<String, Object>> getFallbackRates() {
        return List.of(
            Map.of("commodity", "Wheat", "market", "Nashik", "state", "Maharashtra", "district", "Nashik", "minPrice", "2100", "maxPrice", "2400", "modalPrice", "2250", "date", "13/05/2026"),
            Map.of("commodity", "Rice", "market", "Karnal", "state", "Haryana", "district", "Karnal", "minPrice", "3500", "maxPrice", "4200", "modalPrice", "3800", "date", "13/05/2026"),
            Map.of("commodity", "Tomato", "market", "Pimpalgaon", "state", "Maharashtra", "district", "Nashik", "minPrice", "800", "maxPrice", "1500", "modalPrice", "1200", "date", "13/05/2026"),
            Map.of("commodity", "Cotton", "market", "Amravati", "state", "Maharashtra", "district", "Amravati", "minPrice", "6500", "maxPrice", "7500", "modalPrice", "7000", "date", "13/05/2026"),
            Map.of("commodity", "Maize", "market", "Davangere", "state", "Karnataka", "district", "Davangere", "minPrice", "1800", "maxPrice", "2100", "modalPrice", "1950", "date", "13/05/2026")
        );
    }
}
