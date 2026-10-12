package com.agroverse.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ChatbotService {

    private final GroqService groqService;

    public ChatbotService(GroqService groqService) {
        this.groqService = groqService;
    }

    public String getReply(String message, String language) {
        if (message == null || message.trim().isEmpty()) {
            return "Hello! I am your AgroVerse assistant. How can I help you today?";
        }
        
        Map<String, Object> response = groqService.generateReply(message, language);
        if (response != null && response.containsKey("reply")) {
            return (String) response.get("reply");
        }
        return "I'm having trouble connecting right now. Please try again later.";
    }
}
