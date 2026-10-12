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
    public ResponseEntity<List<CropItem>> getCrops(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(cropItemRepository.findByUserIdOrderByCreatedAtDesc(userId));
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
        crop.setStatus(status != null ? status.trim() : "Healthy");
        crop.setLastChecked("Just now");
        crop.setImage("assets/tomato.png");

        CropItem saved = cropItemRepository.save(crop);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
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
