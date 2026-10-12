package com.agroverse.backend.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IntentClassifier {
    private static final Logger logger = LoggerFactory.getLogger(IntentClassifier.class);

    public enum Intent {
        QUERY, COMMAND, TASK, UNKNOWN
    }

    public Intent classify(String input) {
        logger.info("Classifying intent for input: '{}'", input);
        String lowerInput = input.toLowerCase();

        // Simple heuristic-based intent classification
        if (lowerInput.startsWith("set a reminder") || lowerInput.startsWith("remind me") || lowerInput.startsWith("schedule")) {
            logger.info("Intent classified as TASK");
            return Intent.TASK;
        } else if (lowerInput.startsWith("do ") || lowerInput.startsWith("fetch ") || lowerInput.startsWith("calculate ")) {
            logger.info("Intent classified as COMMAND");
            return Intent.COMMAND;
        } else if (lowerInput.startsWith("what") || lowerInput.startsWith("how") || lowerInput.startsWith("why") || lowerInput.startsWith("can you")) {
            logger.info("Intent classified as QUERY");
            return Intent.QUERY;
        }

        logger.info("Intent classified as QUERY (Default fallback)");
        return Intent.QUERY;
    }
}
