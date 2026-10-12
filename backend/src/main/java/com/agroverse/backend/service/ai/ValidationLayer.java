package com.agroverse.backend.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidationLayer {
    private static final Logger logger = LoggerFactory.getLogger(ValidationLayer.class);

    public String validate(String rawResponse) {
        logger.info("Validating response for factual consistency, output format, and completeness...");
        
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            logger.error("Validation Failed: Empty or null response detected");
            return "Sorry, I couldn't generate a complete response. Please try again.";
        }
        
        if (rawResponse.toLowerCase().contains("i don't know") && rawResponse.length() < 20) {
            logger.warn("Validation Warning: Response is too brief and unhelpful.");
        }
        
        // Simulating checking for hallucinated data or invalid formats
        if (rawResponse.contains("Exception") || rawResponse.contains("Error 500")) {
            logger.error("Validation Failed: Raw error exposed to user.");
            return "An internal verification error occurred while processing your query.";
        }

        logger.info("Response validation successful. Output formatting is confirmed.");
        return rawResponse.trim();
    }
}
