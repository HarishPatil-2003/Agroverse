package com.agroverse.backend.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class MemoryManager {
    private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);

    // Short-term memory (rolling window of 15 messages)
    private final Map<String, LinkedList<String>> shortTermMemory = new ConcurrentHashMap<>();
    
    // Simulated Vector-Based Long-Term Memory (FAISS-like concept)
    private final Map<String, String> longTermVectorMemory = new ConcurrentHashMap<>();

    public MemoryManager() {
        // Pre-loaded domain knowledge
        longTermVectorMemory.put("wheat fertilization npk schedule", "Apply NPK fertilizer evenly during wheat planting phase.");
        longTermVectorMemory.put("rice brown planthopper pest control", "Use neem oil spray to control brown planthoppers in rice fields.");
        longTermVectorMemory.put("drip irrigation tomato schedule", "Tomato drip irrigation should be 30 minutes twice daily during peak summer.");
    }

    public void addMessage(String sessionId, String role, String message) {
        shortTermMemory.computeIfAbsent(sessionId, k -> new LinkedList<>());
        LinkedList<String> sessionMemory = shortTermMemory.get(sessionId);

        // Maintain conversational flow with rolling window of 15 messages
        sessionMemory.add(role + ": " + message);
        if (sessionMemory.size() > 15) {
            sessionMemory.removeFirst();
        }

        // Filtering mechanism for Long-Term Memory (e.g. store only substantial user inputs)
        if ("User".equalsIgnoreCase(role) && isImportantContext(message)) {
            logger.info("Storing relevant interaction to long-term vector memory: '{}'", message);
            // In a real FAISS setup, we would embed 'message' and store in DB. Here we store it directly.
            longTermVectorMemory.put(message, "Past user context: " + message);
        }
    }

    private boolean isImportantContext(String message) {
        String[] words = message.split("\\s+");
        // Filter out short greetings and simple acknowledgments
        return words.length >= 4 && !message.toLowerCase().matches("^(hi|hello|thanks|ok|yes|no).*");
    }

    public String buildStructuredContext(String sessionId, String currentQuery) {
        logger.info("Retrieving dual-memory context for query...");

        StringBuilder structuredPrompt = new StringBuilder();

        // 1. Long-Term Memory Retrieval (Simulated Semantic Similarity)
        List<String> topMemories = retrieveTopRelevantMemories(currentQuery, 3);
        
        structuredPrompt.append("=== LONG-TERM KNOWLEDGE / CONTEXT ===\n");
        if (!topMemories.isEmpty()) {
            topMemories.forEach(mem -> structuredPrompt.append("- ").append(mem).append("\n"));
            logger.info("Retrieved {} relevant memories from long-term storage.", topMemories.size());
        } else {
            structuredPrompt.append("No specific long-term memory matched.\n");
        }
        structuredPrompt.append("\n");

        // 2. Short-Term Conversation History
        structuredPrompt.append("=== RECENT CONVERSATION HISTORY ===\n");
        LinkedList<String> history = shortTermMemory.getOrDefault(sessionId, new LinkedList<>());
        if (history.isEmpty()) {
            structuredPrompt.append("(No recent conversation)\n");
        } else {
            history.forEach(msg -> structuredPrompt.append(msg).append("\n"));
        }
        structuredPrompt.append("\n");

        return structuredPrompt.toString();
    }

    // Simulates an Embedding + FAISS Cosine Similarity search
    private List<String> retrieveTopRelevantMemories(String query, int topK) {
        Set<String> queryTokens = new HashSet<>(Arrays.asList(query.toLowerCase().split("\\W+")));
        
        // Compute Jaccard Similarity as a lightweight simulation of semantic vectors
        Map<String, Double> scoredMemories = new HashMap<>();
        for (Map.Entry<String, String> entry : longTermVectorMemory.entrySet()) {
            Set<String> memoryTokens = new HashSet<>(Arrays.asList(entry.getKey().toLowerCase().split("\\W+")));
            
            Set<String> intersection = new HashSet<>(queryTokens);
            intersection.retainAll(memoryTokens);
            
            Set<String> union = new HashSet<>(queryTokens);
            union.addAll(memoryTokens);
            
            double score = union.isEmpty() ? 0 : (double) intersection.size() / union.size();
            if (score > 0.05) { // Threshold for relevance
                scoredMemories.put(entry.getValue(), score);
            }
        }

        return scoredMemories.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
