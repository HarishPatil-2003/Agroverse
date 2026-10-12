package com.agroverse.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AgroChatbotService {
    private static final Logger logger = LoggerFactory.getLogger(AgroChatbotService.class);
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    @Value("${agroverse.ai.groq.api-key:${GROQ_API_KEY:}}")
    private String groqApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AgroChatbotService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String getReply(String message, String language) {
        try {
            logger.info("Generating reply for [{}]: {}", language, message);
            
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", MODEL);
            
            ArrayNode messages = requestBody.putArray("messages");
            
            ObjectNode systemNode = objectMapper.createObjectNode();
            systemNode.put("role", "system");
            
            String prompt = "You are Kisan Sahayak, a helpful farming assistant for the AgroVerse platform. " +
                            "Respond strictly in " + language + " language. " +
                            "Provide short, practical farming advice in simple language. Use the context of Indian agriculture.";
            
            systemNode.put("content", prompt);
            messages.add(systemNode);

            ObjectNode userNode = objectMapper.createObjectNode();
            userNode.put("role", "user");
            userNode.put("content", message);
            messages.add(userNode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("choices").get(0).path("message").path("content").asText().trim();
            }
            return "I'm having trouble thinking right now. Please try again later.";
            
        } catch (Exception e) {
            logger.error("Chatbot Service Error", e);
            return "Error: " + e.getMessage();
        }
    }
}
