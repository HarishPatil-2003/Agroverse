package com.agroverse.backend.service;

import com.agroverse.backend.service.ai.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgroAIService {

    private static final Logger logger = LoggerFactory.getLogger(AgroAIService.class);

    private final IntentClassifier intentClassifier;
    private final MemoryManager memoryManager;
    private final Planner planner;
    private final ExecutionLayer executionLayer;
    private final ValidationLayer validationLayer;

    public AgroAIService(IntentClassifier intentClassifier, MemoryManager memoryManager, 
                           Planner planner, ExecutionLayer executionLayer, ValidationLayer validationLayer) {
        this.intentClassifier = intentClassifier;
        this.memoryManager = memoryManager;
        this.planner = planner;
        this.executionLayer = executionLayer;
        this.validationLayer = validationLayer;
    }

    public String generateReply(String userMessage) {
        String sessionId = "default-session"; 

        logger.info("--- Starting Intelligent Modular AI Pipeline ---");

        // 1. Intent Detection
        IntentClassifier.Intent intent = intentClassifier.classify(userMessage);

        // 2. Context Retrieval (Dual-Memory System)
        String memoryContext = memoryManager.buildStructuredContext(sessionId, userMessage);

        // 3. Planning
        List<String> plan = planner.planExecution(userMessage, intent);

        // 4. Execution (Tool Selection / LLM generation)
        String rawResponse = executionLayer.execute(userMessage, intent, plan, memoryContext);

        // 5. Validation
        String finalResponse = validationLayer.validate(rawResponse);

        // 6. Update Memory (Retain Interaction with Context Filters)
        memoryManager.addMessage(sessionId, "User", userMessage);
        memoryManager.addMessage(sessionId, "Assistant", finalResponse);

        logger.info("--- Pipeline Execution Complete ---");
        return finalResponse;
    }
}
