package com.agroverse.backend.controller;

import com.agroverse.backend.model.CropItem;
import com.agroverse.backend.model.FarmTask;
import com.agroverse.backend.model.Notification;
import com.agroverse.backend.model.User;
import com.agroverse.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class HomeController {

    private final CropItemRepository cropItemRepository;
    private final FarmTaskRepository farmTaskRepository;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final CropAnalysisRepository cropAnalysisRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public HomeController(
            CropItemRepository cropItemRepository,
            FarmTaskRepository farmTaskRepository,
            MarketplaceListingRepository marketplaceListingRepository,
            CropAnalysisRepository cropAnalysisRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.cropItemRepository = cropItemRepository;
        this.farmTaskRepository = farmTaskRepository;
        this.marketplaceListingRepository = marketplaceListingRepository;
        this.cropAnalysisRepository = cropAnalysisRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/home/summary")
    public ResponseEntity<Map<String, Object>> getHomeSummary(Authentication authentication) {
        Map<String, Object> summary = new HashMap<>();

        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            Long userId = (Long) authentication.getPrincipal();
            long cropsCount = cropItemRepository.countByUserId(userId);
            long listingsCount = marketplaceListingRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
            long pendingTasks = farmTaskRepository.countByUserIdAndCompletedFalse(userId);
            long diagnosesCount = cropAnalysisRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
            long unreadNotifications = notificationRepository.countByUserIdAndReadFalse(userId);

            summary.put("activeCrops", cropsCount);
            summary.put("activeListings", listingsCount);
            summary.put("tasksToday", pendingTasks);
            summary.put("cropAnalyses", diagnosesCount);
            summary.put("newNotifications", unreadNotifications);
        } else {
            summary.put("activeCrops", 0);
            summary.put("activeListings", 0);
            summary.put("tasksToday", 0);
            summary.put("cropAnalyses", 0);
            summary.put("newNotifications", 0);
        }
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/crops")
    public ResponseEntity<List<CropItem>> getCrops(
            @RequestParam(value = "includeArchived", defaultValue = "false") boolean includeArchived,
            Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        Long userId = (Long) authentication.getPrincipal();
        if (includeArchived) {
            return ResponseEntity.ok(cropItemRepository.findByUserIdOrderByCreatedAtDesc(userId));
        }
        return ResponseEntity.ok(cropItemRepository.findByUserIdAndArchivedFalseOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/crops/{id}")
    public ResponseEntity<?> getCropDetails(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        CropItem crop = cropItemRepository.findById(id).orElse(null);
        if (crop == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (crop.getUser() == null || !crop.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied to crop."));
        }
        return ResponseEntity.ok(crop);
    }

    @PostMapping("/crops")
    @Transactional
    public ResponseEntity<?> addCrop(@RequestBody Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String name = payload.get("name");
        String field = payload.get("field");
        String status = payload.get("status");

        if (name == null || name.trim().isEmpty() || field == null || field.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Crop name and field location are required."));
        }

        CropItem crop = new CropItem();
        crop.setUser(user);
        crop.setName(name.trim());
        crop.setField(field.trim());
        crop.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : "Healthy");
        crop.setLastChecked("Just now");
        crop.setImage(payload.getOrDefault("image", "assets/tomato.png"));

        // Smart Lifecycle Fields
        crop.setVariety(payload.get("variety"));
        crop.setGrowthStage(payload.getOrDefault("growthStage", "Vegetative"));
        crop.setIrrigationNotes(payload.get("irrigationNotes"));
        crop.setFertilizerNotes(payload.get("fertilizerNotes"));
        crop.setTreatmentNotes(payload.get("treatmentNotes"));

        String sowingStr = payload.get("sowingDate");
        if (sowingStr != null && !sowingStr.trim().isEmpty()) {
            try {
                crop.setSowingDate(java.time.LocalDate.parse(sowingStr.trim()));
            } catch (Exception ignored) {}
        }

        String harvestStr = payload.get("expectedHarvestDate");
        if (harvestStr != null && !harvestStr.trim().isEmpty()) {
            try {
                crop.setExpectedHarvestDate(java.time.LocalDate.parse(harvestStr.trim()));
            } catch (Exception ignored) {}
        }

        CropItem saved = cropItemRepository.save(crop);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @RequestMapping(value = "/crops/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @Transactional
    public ResponseEntity<?> updateCrop(@PathVariable Long id, @RequestBody Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        CropItem crop = cropItemRepository.findById(id).orElse(null);
        if (crop == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (crop.getUser() == null || !crop.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }

        if (payload.containsKey("name") && payload.get("name") != null && !payload.get("name").trim().isEmpty()) {
            crop.setName(payload.get("name").trim());
        }
        if (payload.containsKey("field") && payload.get("field") != null && !payload.get("field").trim().isEmpty()) {
            crop.setField(payload.get("field").trim());
        }
        if (payload.containsKey("status") && payload.get("status") != null) {
            crop.setStatus(payload.get("status").trim());
        }
        if (payload.containsKey("variety")) {
            crop.setVariety(payload.get("variety"));
        }
        if (payload.containsKey("growthStage") && payload.get("growthStage") != null) {
            crop.setGrowthStage(payload.get("growthStage").trim());
        }
        if (payload.containsKey("irrigationNotes")) {
            crop.setIrrigationNotes(payload.get("irrigationNotes"));
        }
        if (payload.containsKey("fertilizerNotes")) {
            crop.setFertilizerNotes(payload.get("fertilizerNotes"));
        }
        if (payload.containsKey("treatmentNotes")) {
            crop.setTreatmentNotes(payload.get("treatmentNotes"));
        }
        if (payload.containsKey("image") && payload.get("image") != null) {
            crop.setImage(payload.get("image"));
        }
        if (payload.containsKey("sowingDate")) {
            String s = payload.get("sowingDate");
            crop.setSowingDate(s != null && !s.trim().isEmpty() ? java.time.LocalDate.parse(s.trim()) : null);
        }
        if (payload.containsKey("expectedHarvestDate")) {
            String h = payload.get("expectedHarvestDate");
            crop.setExpectedHarvestDate(h != null && !h.trim().isEmpty() ? java.time.LocalDate.parse(h.trim()) : null);
        }
        if (payload.containsKey("archived")) {
            crop.setArchived(Boolean.parseBoolean(payload.get("archived")));
        }

        crop.setLastChecked("Just now");
        CropItem saved = cropItemRepository.save(crop);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/crops/{id}/archive")
    @Transactional
    public ResponseEntity<?> toggleArchiveCrop(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        CropItem crop = cropItemRepository.findById(id).orElse(null);
        if (crop == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (crop.getUser() == null || !crop.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }

        crop.setArchived(!crop.isArchived());
        CropItem saved = cropItemRepository.save(crop);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/crops/{id}")
    @Transactional
    public ResponseEntity<?> deleteCrop(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        CropItem crop = cropItemRepository.findById(id).orElse(null);
        if (crop == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        if (crop.getUser() == null || !crop.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }

        cropItemRepository.delete(crop);
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }

    @GetMapping("/tasks/today")
    public ResponseEntity<List<FarmTask>> getTodayTasks(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(farmTaskRepository.findByUserIdOrderByCreatedAtAsc(userId));
    }

    @PostMapping("/tasks")
    @Transactional
    public ResponseEntity<?> addTask(@RequestBody Map<String, String> payload, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String title = payload.get("title");
        String subtitle = payload.get("subtitle");
        String dueTime = payload.get("dueTime");
        String type = payload.get("type");

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Task title is required."));
        }

        FarmTask task = new FarmTask();
        task.setUser(user);
        task.setTitle(title.trim());
        task.setSubtitle(subtitle != null ? subtitle.trim() : "");
        task.setDueTime(dueTime != null ? dueTime.trim() : "Today");
        task.setType(type != null ? type.trim() : "general");
        task.setIcon("fa-clipboard-check");
        task.setColor("#16a34a");
        task.setCompleted(false);

        FarmTask saved = farmTaskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @RequestMapping(value = {"/tasks/{id}/toggle", "/tasks/{id}/complete"}, method = {RequestMethod.POST, RequestMethod.PATCH})
    @Transactional
    public ResponseEntity<?> completeTask(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        FarmTask task = farmTaskRepository.findById(id).orElse(null);
        if (task == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if (task.getUser() == null || !task.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only modify your own tasks."));
        }

        task.setCompleted(!task.isCompleted());
        FarmTask saved = farmTaskRepository.save(task);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getNotifications(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @PostMapping("/notifications/{id}/read")
    @Transactional
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null) return ResponseEntity.notFound().build();

        if (notification.getUser() == null || !notification.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied to notification."));
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(Map.of("read", true, "id", id));
    }
}
