package com.agroverse.backend.controller;

import com.agroverse.backend.service.AgroAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class AgroAIController {

    private static final Logger logger = LoggerFactory.getLogger(AgroAIController.class);

    @Autowired
    private AgroAIService agroAIService;

    @PostMapping("/chat-v1")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        Map<String, String> response = new HashMap<>();

        if (userMessage == null || userMessage.trim().isEmpty()) {
            response.put("error", "Message cannot be empty or null.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            logger.info("Received chat request: {}", userMessage);
            String reply = agroAIService.generateReply(userMessage);
            
            response.put("reply", reply);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Service failure in chat controller", e);
            response.put("error", "AI service failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

