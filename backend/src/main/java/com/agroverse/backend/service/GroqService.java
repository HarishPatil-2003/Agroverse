package com.agroverse.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GroqService {
    private static final Logger logger = LoggerFactory.getLogger(GroqService.class);

    @Value("${agroverse.ai.groq.api-key:${GROQ_API_KEY:}}")
    private String apiKey;

    @Value("${agroverse.ai.groq.model:llama-3.3-70b-versatile}")
    private String modelName;

    @Value("${agroverse.ai.groq.timeout-ms:10000}")
    private int timeoutMs;

    private final RestTemplate restTemplate;
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public GroqService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(12000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("your_") && !apiKey.startsWith("<");
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean configured = isConfigured();
        status.put("configured", configured);
        status.put("provider", "Groq Cloud");
        status.put("model", modelName);
        status.put("status", configured ? "READY" : "PENDING_CONFIGURATION");
        status.put("message", configured
                ? "Groq Agricultural Assistant is active."
                : "Groq API key is pending configuration in environment variables (GROQ_API_KEY).");
        return status;
    }

    public Map<String, Object> generateReply(String userMessage, String language) {
        Map<String, Object> response = new HashMap<>();

        // Validate input
        if (userMessage == null || userMessage.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Message cannot be empty.");
            return response;
        }

        if (userMessage.length() > 500) {
            userMessage = userMessage.substring(0, 500);
        }

        if (!isConfigured()) {
            response.put("success", false);
            response.put("status", "PENDING_CONFIGURATION");
            response.put("reply", getPendingMessage(language));
            response.put("configured", false);
            return response;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            String systemPrompt = "You are Kisan Sahayak (AgroVerse AI), an agricultural expert for Indian farmers. " +
                    "Provide practical, science-based advice on crops, soil health, fertilizers, weather adaptation, and pest management. " +
                    "Always state clearly that your guidance is advisory and recommend leaf image upload or local KVK consultation for confirmed diagnoses. " +
                    "Respond concisely and respectfully in " + (language != null ? language : "en") + ".";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.6);
            requestBody.put("max_tokens", 450);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(GROQ_URL, entity, Map.class);

            if (apiResponse.getStatusCode() == HttpStatus.OK && apiResponse.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) apiResponse.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) msg.get("content");

                    response.put("success", true);
                    response.put("reply", content);
                    response.put("model", modelName);
                    return response;
                }
            }

            response.put("success", false);
            response.put("reply", "The agricultural assistant is currently unresponsive. Please try again shortly.");
            return response;

        } catch (Exception e) {
            logger.warn("Groq API request failed: {}", e.getMessage());
            response.put("success", false);
            response.put("status", "PROVIDER_ERROR");
            response.put("reply", "AI assistant service unavailable at the moment. Please verify network and credentials.");
            return response;
        }
    }

    private String getPendingMessage(String language) {
        if ("hi".equalsIgnoreCase(language)) {
            return "एआई सहायक वर्तमान में कॉन्फ़िगरेशन की प्रतीक्षा में है (GROQ_API_KEY सेट नहीं है)। कृपया स्थानीय कृषि विशेषज्ञ से संपर्क करें।";
        } else if ("mr".equalsIgnoreCase(language)) {
            return "एआय सहाय्यक सध्या कॉन्फिगरेशनच्या प्रतीक्षेत आहे (GROQ_API_KEY सेट नाही). कृपया स्थानिक कृषी तज्ञांशी संपर्क साधा.";
        }
        return "The Groq AI assistant integration is currently pending configuration (GROQ_API_KEY not set). Please configure the API key in environment settings.";
    }
}
