package com.agroverse.backend.controller;

import com.agroverse.backend.dto.ChatRequest;
import com.agroverse.backend.service.GroqService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatbotController {

    private final GroqService groqService;

    public ChatbotController(GroqService groqService) {
        this.groqService = groqService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        String msg = (request != null) ? request.getMessage() : null;
        String lang = (request != null) ? request.getLanguage() : "en";
        Map<String, Object> context = (request != null) ? request.getContext() : null;
        Map<String, Object> reply = groqService.generateReply(msg, lang, context);
        return ResponseEntity.ok(reply);
    }

    @GetMapping("/chat/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(groqService.getStatus());
    }
}
