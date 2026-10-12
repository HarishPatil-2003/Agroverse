package com.agroverse.backend.service;

import com.agroverse.backend.config.JwtTokenProvider;
import com.agroverse.backend.dto.AuthResponse;
import com.agroverse.backend.dto.LoginRequest;
import com.agroverse.backend.dto.RegisterRequest;
import com.agroverse.backend.model.Notification;
import com.agroverse.backend.model.User;
import com.agroverse.backend.repository.NotificationRepository;
import com.agroverse.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FarmerIdGenerator farmerIdGenerator;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            FarmerIdGenerator farmerIdGenerator,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.farmerIdGenerator = farmerIdGenerator;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public User resolveOrCreateSupabaseUser(String supabaseUserId, String email, Map<String, Object> metadata) {
        if (supabaseUserId == null || supabaseUserId.isBlank()) {
            throw new IllegalArgumentException("Supabase User ID is required.");
        }

        // 1. Try finding by Supabase UUID
        Optional<User> existing = userRepository.findBySupabaseUserId(supabaseUserId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Try finding by Email (link existing local user)
        String safeEmail = email != null ? email.trim().toLowerCase() : "";
        if (!safeEmail.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(safeEmail);
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                user.setSupabaseUserId(supabaseUserId);
                return userRepository.save(user);
            }
        }

        // 3. Provision new Farmer profile linked to Supabase User ID
        User user = new User();
        user.setSupabaseUserId(supabaseUserId);
        user.setEmail(!safeEmail.isBlank() ? safeEmail : supabaseUserId + "@farmer.agroverse");

        String farmerId;
        do {
            farmerId = farmerIdGenerator.generateFarmerId();
        } while (userRepository.existsByFarmerId(farmerId));
        user.setFarmerId(farmerId);

        if (metadata != null) {
            String fullName = (String) metadata.getOrDefault("full_name", metadata.get("name"));
            if (fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName.trim());
                String[] parts = fullName.trim().split("\\s+", 2);
                user.setFirstName(parts[0]);
                if (parts.length > 1) user.setLastName(parts[1]);
            }
            user.setPhone((String) metadata.get("phone"));
            user.setAddress((String) metadata.getOrDefault("address", metadata.get("location")));
            user.setFarmType((String) metadata.getOrDefault("farm_type", metadata.get("farmType")));
            Object land = metadata.getOrDefault("land_holding", metadata.get("landHolding"));
            if (land != null) {
                try {
                    String num = land.toString().replaceAll("[^0-9.]", "").trim();
                    if (!num.isEmpty()) {
                        user.setLandHolding(Double.parseDouble(num));
                    }
                } catch (Exception ignored) {}
            }
            user.setPreferredLanguage((String) metadata.getOrDefault("preferred_language", metadata.get("preferredLanguage")));
        }

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(!safeEmail.isBlank() ? safeEmail.split("@")[0] : "Farmer " + farmerId);
        }
        if (user.getFarmType() == null) user.setFarmType("General");
        user.setLandUnit("Acres");
        user.setRole("ROLE_FARMER");
        user.setProfileCompleted(false);
        user.setJoinedAt(java.time.LocalDateTime.now());

        User saved = userRepository.save(user);

        // Add Welcome Notification
        try {
            notificationRepository.save(new Notification(
                    saved,
                    "Welcome to AgroVerse!",
                    "Your official farmer identity #" + saved.getFarmerId() + " is ready. Explore crop diagnostics and farming tools.",
                    "WELCOME"
            ));
        } catch (Exception ignored) {}

        return saved;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (userRepository.findByEmail(request.getEmail().trim().toLowerCase()).isPresent()) {
            throw new IllegalArgumentException("An account with this email address already exists. Please log in.");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        String fullName = ((request.getFirstName() != null ? request.getFirstName() : "") + " " +
                (request.getLastName() != null ? request.getLastName() : "")).trim();
        user.setFullName(fullName.isEmpty() ? (request.getName() != null ? request.getName() : "Farmer") : fullName);
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone());

        // Generate Unique Farmer ID
        String farmerId;
        do {
            farmerId = farmerIdGenerator.generateFarmerId();
        } while (userRepository.existsByFarmerId(farmerId));
        user.setFarmerId(farmerId);
        user.setJoinedAt(java.time.LocalDateTime.now());

        // Hash password with BCrypt
        String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(request.getPassword(), org.mindrot.jbcrypt.BCrypt.gensalt(10));
        user.setPassword(hashedPassword);

        user.setAddress(request.getAddress() != null ? request.getAddress() : request.getLocation());
        user.setFarmType(request.getFarmType() != null ? request.getFarmType() : "General");

        if (request.getLandHolding() != null && !request.getLandHolding().trim().isEmpty()) {
            try {
                String numericOnly = request.getLandHolding().replaceAll("[^0-9.]", "").trim();
                if (!numericOnly.isEmpty()) {
                    user.setLandHolding(Double.parseDouble(numericOnly));
                }
            } catch (NumberFormatException ignored) {}
        }
        user.setLandUnit("Acres");
        user.setPreferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "en");
        user.setRole("ROLE_FARMER");
        user.setProfileCompleted(false);

        User saved = userRepository.save(user);

        // Add Welcome Notification
        try {
            notificationRepository.save(new Notification(
                    saved,
                    "Welcome to AgroVerse!",
                    "Your official farmer identity #" + saved.getFarmerId() + " is ready.",
                    "WELCOME"
            ));
        } catch (Exception ignored) {}

        String token = jwtTokenProvider.generateToken(saved.getId(), saved.getEmail(), saved.getFarmerId(), saved.getRole());
        return new AuthResponse(token, saved);
    }

    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (user.getPassword() == null || !org.mindrot.jbcrypt.BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getFarmerId(), user.getRole());
        return new AuthResponse(token, user);
    }

    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getProfileByFarmerId(String farmerId) {
        return userRepository.findByFarmerId(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));
    }

    @Transactional
    public User updateProfile(Long userId, User updatedData) {
        User user = getProfile(userId);

        if (updatedData.getFirstName() != null) user.setFirstName(updatedData.getFirstName());
        if (updatedData.getLastName() != null) user.setLastName(updatedData.getLastName());
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "")).trim();
        if (!fullName.isBlank()) {
            user.setFullName(fullName);
        }

        if (updatedData.getPhone() != null) user.setPhone(updatedData.getPhone());
        if (updatedData.getDateOfBirth() != null) user.setDateOfBirth(updatedData.getDateOfBirth());
        if (updatedData.getGender() != null) user.setGender(updatedData.getGender());
        if (updatedData.getFarmType() != null) user.setFarmType(updatedData.getFarmType());
        if (updatedData.getLandHolding() != null) user.setLandHolding(updatedData.getLandHolding());
        if (updatedData.getLandUnit() != null) user.setLandUnit(updatedData.getLandUnit());
        if (updatedData.getExperienceYears() != null) user.setExperienceYears(updatedData.getExperienceYears());
        if (updatedData.getAddress() != null) user.setAddress(updatedData.getAddress());
        if (updatedData.getCity() != null) user.setCity(updatedData.getCity());
        if (updatedData.getState() != null) user.setState(updatedData.getState());
        if (updatedData.getPincode() != null) user.setPincode(updatedData.getPincode());
        if (updatedData.getPreferredLanguage() != null) user.setPreferredLanguage(updatedData.getPreferredLanguage());

        if (user.getFarmType() != null && user.getAddress() != null) {
            user.setProfileCompleted(true);
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfilePhoto(Long userId, String photoUrl) {
        User user = getProfile(userId);
        user.setProfileImageUrl(photoUrl);
        return userRepository.save(user);
    }
}
