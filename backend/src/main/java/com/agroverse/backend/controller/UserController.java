package com.agroverse.backend.controller;

import com.agroverse.backend.dto.AuthResponse;
import com.agroverse.backend.dto.LoginRequest;
import com.agroverse.backend.dto.RegisterRequest;
import com.agroverse.backend.model.User;
import com.agroverse.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.Collections;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping({"/auth/register", "/users/register"})
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping({"/auth/login", "/users/login"})
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Invalid email or password"));
        }
    }

    @GetMapping("/profile/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized: Valid session required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        try {
            User user = userService.getProfile(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "User profile not found"));
        }
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfileById(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }
        Long currentUserId = (Long) authentication.getPrincipal();

        // Enforce strict owner-only access: Farmer A cannot view Farmer B's private profile
        if (!currentUserId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("error", "Access denied: You cannot view another farmer's private profile."));
        }

        try {
            User user = userService.getProfile(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "User not found"));
        }
    }

    @PutMapping("/profile/me")
    public ResponseEntity<?> updateProfile(Authentication authentication, @RequestBody User updatedData) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }
        Long userId = (Long) authentication.getPrincipal();
        try {
            User user = userService.updateProfile(userId, updatedData);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/profile/me/photo")
    public ResponseEntity<?> uploadPhoto(@RequestParam("photo") MultipartFile file, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error", "Unauthorized"));
        }
        Long userId = (Long) authentication.getPrincipal();

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "File is empty"));
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Collections.singletonMap("error", "File size exceeds 10MB limit"));
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Only JPEG, PNG, or WEBP images are supported."));
        }

        try {
            User user = userService.getProfile(userId);

            String uploadDir = "uploads/profile/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String extension = ".jpg";
            if (contentType.equals("image/png")) extension = ".png";
            else if (contentType.equals("image/webp")) extension = ".webp";

            // Safe random filename preventing path traversal
            String fileName = "farmer-" + user.getFarmerId().replaceAll("[^a-zA-Z0-9-]", "") + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path filePath = uploadPath.resolve(fileName).normalize();

            // Ensure destination path remains inside uploadDir
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid file destination"));
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String photoUrl = "/api/uploads/profile/" + fileName;
            userService.updateProfilePhoto(userId, photoUrl);

            return ResponseEntity.ok(Collections.singletonMap("profileImageUrl", photoUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Could not upload image."));
        }
    }
}
