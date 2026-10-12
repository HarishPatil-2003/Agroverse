package com.agroverse.backend.service;

import com.agroverse.backend.model.GovernmentScheme;
import com.agroverse.backend.repository.GovernmentSchemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GovernmentSchemeService {

    @Autowired
    private GovernmentSchemeRepository repository;

    @Autowired
    private com.agroverse.backend.repository.SchemeApplicationRepository applicationRepository;

    @Autowired
    private com.agroverse.backend.repository.UserRepository userRepository;

    public List<GovernmentScheme> getAllSchemes() {
        return repository.findAll();
    }

    public List<GovernmentScheme> getUpcomingDeadlines() {
        return repository.findUpcomingDeadlines();
    }

    public GovernmentScheme getSchemeById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<GovernmentScheme> searchSchemes(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSchemes();
        }
        return repository.searchByKeyword(keyword.trim());
    }

    public List<GovernmentScheme> filterSchemes(String state, String cropType, String category, String schemeType) {
        List<GovernmentScheme> allSchemes = repository.findAll();

        return allSchemes.stream()
                .filter(s -> state == null || state.isEmpty() || 
                             s.getApplicableStates().contains("ALL") || 
                             s.getApplicableStates().contains(state))
                .filter(s -> cropType == null || cropType.isEmpty() || 
                             s.getApplicableCropTypes().contains("ALL") || 
                             s.getApplicableCropTypes().contains(cropType))
                .filter(s -> category == null || category.isEmpty() || 
                             category.equalsIgnoreCase(s.getCategory()))
                .filter(s -> schemeType == null || schemeType.isEmpty() || 
                             schemeType.equalsIgnoreCase(s.getSchemeType()))
                .collect(Collectors.toList());
    }

    public com.agroverse.backend.model.SchemeApplication applyForScheme(Long schemeId, Long userId) {
        com.agroverse.backend.model.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        GovernmentScheme scheme = repository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("Scheme not found"));

        com.agroverse.backend.model.SchemeApplication application = applicationRepository.findByUserAndScheme(user, scheme)
                .orElse(new com.agroverse.backend.model.SchemeApplication());

        application.setUser(user);
        application.setScheme(scheme);
        if (application.getStatus() == null) {
            application.setStatus(com.agroverse.backend.model.SchemeApplicationStatus.APPLIED);
        }
        return applicationRepository.save(application);
    }

    public List<com.agroverse.backend.model.SchemeApplication> getUserApplications(Long userId) {
        com.agroverse.backend.model.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return applicationRepository.findByUser(user);
    }

    public GovernmentScheme createScheme(GovernmentScheme scheme) {
        return repository.save(scheme);
    }

    public GovernmentScheme updateScheme(Long id, GovernmentScheme schemeDetails) {
        GovernmentScheme scheme = repository.findById(id).orElse(null);
        if (scheme != null) {
            scheme.setTitle(schemeDetails.getTitle());
            scheme.setShortDescription(schemeDetails.getShortDescription());
            scheme.setFullDescription(schemeDetails.getFullDescription());
            scheme.setSchemeType(schemeDetails.getSchemeType());
            scheme.setCategory(schemeDetails.getCategory());
            scheme.setApplicableStates(schemeDetails.getApplicableStates());
            scheme.setApplicableCropTypes(schemeDetails.getApplicableCropTypes());
            scheme.setEligibility(schemeDetails.getEligibility());
            scheme.setBenefits(schemeDetails.getBenefits());
            scheme.setDocumentsRequired(schemeDetails.getDocumentsRequired());
            scheme.setApplicationProcess(schemeDetails.getApplicationProcess());
            scheme.setOfficialWebsite(schemeDetails.getOfficialWebsite());
            scheme.setImageUrl(schemeDetails.getImageUrl());
            scheme.setStatus(schemeDetails.getStatus());
            scheme.setDeadlineDate(schemeDetails.getDeadlineDate());
            scheme.setSubsidyAmount(schemeDetails.getSubsidyAmount());
            return repository.save(scheme);
        }
        return null;
    }

    public void deleteScheme(Long id) {
        repository.deleteById(id);
    }
}
