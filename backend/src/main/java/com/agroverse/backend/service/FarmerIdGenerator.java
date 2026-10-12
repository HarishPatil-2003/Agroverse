package com.agroverse.backend.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Year;

@Service
public class FarmerIdGenerator {
    private static final String PREFIX = "AGV";
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateFarmerId() {
        String year = String.valueOf(Year.now().getValue());
        String randomPart = generateRandomPart(6);
        return PREFIX + "-" + year + "-" + randomPart;
    }

    private String generateRandomPart(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(secureRandom.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
