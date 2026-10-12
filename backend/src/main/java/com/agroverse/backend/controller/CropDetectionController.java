package com.agroverse.backend.controller;

import com.agroverse.backend.service.CropDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/crop-detection")
public class CropDetectionController {

    private final CropDetectionService cropDetectionService;

    public CropDetectionController(CropDetectionService cropDetectionService) {
        this.cropDetectionService = cropDetectionService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeCrop(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "language", defaultValue = "en") String language,
            @RequestParam(value = "farmerId", required = false) String farmerId,
            Authentication authentication
    ) {
        Long userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            userId = (Long) authentication.getPrincipal();
        }

        Map<String, Object> result = cropDetectionService.analyze(image, language, userId, farmerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required to view crop history."));
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(cropDetectionService.getUserHistory(userId));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Map.of(
                "modelReady", cropDetectionService.isModelReady(),
                "engine", "In-Process Java ONNX Runtime 1.18.0",
                "framework", "AgroVerse Canonical Machine Learning Pipeline"
        ));
    }
}
