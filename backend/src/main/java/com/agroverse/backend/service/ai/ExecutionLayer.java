package com.agroverse.backend.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ExecutionLayer {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionLayer.class);
    private final LLMClient llmClient;

    public ExecutionLayer(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    public String execute(String query, IntentClassifier.Intent intent, List<String> plan, String structuredMemoryPrompt) {
        logger.info("Executing plan: {} step(s)", plan.size());
        
        if (intent == IntentClassifier.Intent.TASK) {
            logger.info("Tool Use Activated: Simulated Database Scheduling API Call");
            return "Task scheduled successfully: " + query;
        } else if (intent == IntentClassifier.Intent.COMMAND) {
            logger.info("Tool Use Activated: Simulated System Command Execution API");
            return "System command interpreted and executed safely.";
        }

        // Default: LLM knowledge synthesis for Queries
        logger.info("Tool Use Activated: LLM Knowledge Synthesis Engine");
        
        String systemPrompt = "You are AgroVerse AI, an intelligent farming assistant.\n"
                            + "Strictly use the LONG-TERM KNOWLEDGE and RECENT CONVERSATION HISTORY provided below to inform your answer. "
                            + "Be factual and concise.\n\n"
                            + structuredMemoryPrompt;
        
        logger.info("Executing LLM pipeline request...");
        String response = llmClient.generate(systemPrompt, query);
        
        if (response == null || response.isBlank()) {
            logger.error("Execution layer failed to get response from LLM module.");
            return "I'm having trouble processing that request right now.";
        }
        return response;
    }
}
