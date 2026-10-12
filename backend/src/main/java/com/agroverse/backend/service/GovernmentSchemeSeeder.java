package com.agroverse.backend.service;

import com.agroverse.backend.model.GovernmentScheme;
import com.agroverse.backend.repository.GovernmentSchemeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class GovernmentSchemeSeeder implements CommandLineRunner {

    @Autowired
    private GovernmentSchemeRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            try (InputStream inputStream = TypeReference.class.getResourceAsStream("/schemes.json")) {
                if (inputStream != null) {
                    List<GovernmentScheme> schemes = objectMapper.readValue(inputStream, new TypeReference<List<GovernmentScheme>>() {});
                    repository.saveAll(schemes);
                    System.out.println("Government schemes seeded successfully: " + schemes.size());
                } else {
                    System.err.println("Could not find schemes.json in resources.");
                }
            } catch (Exception e) {
                System.err.println("Error seeding government schemes: " + e.getMessage());
            }
        } else {
            System.out.println("Government schemes already exist. Skipping seeding.");
        }
    }
}
