package com.agroverse.backend.controller;

import com.agroverse.backend.model.GovernmentScheme;
import com.agroverse.backend.model.SchemeApplication;
import com.agroverse.backend.service.GovernmentSchemeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schemes")
public class GovernmentSchemeController {

    private final GovernmentSchemeService service;

    public GovernmentSchemeController(GovernmentSchemeService service) {
        this.service = service;
    }

    @GetMapping("/deadlines")
    public List<GovernmentScheme> getUpcomingDeadlines() {
        return service.getUpcomingDeadlines();
    }

    @PostMapping("/{id}/apply-click")
    public ResponseEntity<?> applyClick(
            @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Authentication required to apply for schemes."));
        }
        Long targetUserId = (Long) authentication.getPrincipal();

        try {
            SchemeApplication app = service.applyForScheme(id, targetUserId);
            GovernmentScheme scheme = service.getSchemeById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("officialUrl", scheme != null ? scheme.getOfficialWebsite() : "");
            response.put("currentStatus", app != null ? app.getStatus() : "APPLIED");
            response.put("message", "Redirecting to official government website...");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/applications/me")
    public ResponseEntity<?> getMyApplications(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Authentication required to view scheme applications."));
        }
        Long targetUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(service.getUserApplications(targetUserId));
    }

    @GetMapping
    public List<GovernmentScheme> getAllSchemes() {
        return service.getAllSchemes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GovernmentScheme> getSchemeById(@PathVariable Long id) {
        GovernmentScheme scheme = service.getSchemeById(id);
        return scheme != null ? ResponseEntity.ok(scheme) : ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public List<GovernmentScheme> searchSchemes(@RequestParam("keyword") String keyword) {
        return service.searchSchemes(keyword);
    }

    @GetMapping("/filter")
    public List<GovernmentScheme> filterSchemes(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String cropType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String schemeType
    ) {
        return service.filterSchemes(state, cropType, category, schemeType);
    }

    @PostMapping
    public ResponseEntity<?> createScheme(@RequestBody GovernmentScheme scheme, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Administrative authentication required."));
        }
        GovernmentScheme created = service.createScheme(scheme);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateScheme(
            @PathVariable Long id,
            @RequestBody GovernmentScheme schemeDetails,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Administrative authentication required."));
        }
        GovernmentScheme updated = service.updateScheme(id, schemeDetails);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteScheme(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Administrative authentication required."));
        }
        service.deleteScheme(id);
        return ResponseEntity.noContent().build();
    }
}
