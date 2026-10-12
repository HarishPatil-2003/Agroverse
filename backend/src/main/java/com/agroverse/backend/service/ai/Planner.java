package com.agroverse.backend.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class Planner {
    private static final Logger logger = LoggerFactory.getLogger(Planner.class);

    public List<String> planExecution(String query, IntentClassifier.Intent intent) {
        logger.info("Decomposing query into structured reasoning steps...");
        List<String> plan;

        if (intent == IntentClassifier.Intent.TASK) {
            plan = Arrays.asList("Extract task details (action, timeline)", "Validate feasibility", "Schedule and execute task");
        } else if (intent == IntentClassifier.Intent.COMMAND) {
            plan = Arrays.asList("Identify command target", "Verify system permissions", "Execute command externally");
        } else {
            plan = Arrays.asList("Analyze query context", "Retrieve factual information from long-term memory", "Synthesize user-friendly response via LLM");
        }

        logger.info("Generated execution plan: {}", plan);
        return plan;
    }
}
