package com.agroverse.backend.controller;

import com.agroverse.backend.model.MarketplaceListing;
import com.agroverse.backend.model.User;
import com.agroverse.backend.repository.MarketplaceListingRepository;
import com.agroverse.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final MarketplaceListingRepository listingRepository;
    private final UserRepository userRepository;

    public MarketplaceController(
            MarketplaceListingRepository listingRepository,
            UserRepository userRepository
    ) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getMarketplaceSummary() {
        Map<String, Object> summary = new HashMap<>();
        long activeCount = listingRepository.count();
        summary.put("totalProducts", activeCount);
        summary.put("activeListings", activeCount);

        List<MarketplaceListing> recent = listingRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
        if (!recent.isEmpty()) {
            summary.put("recentProduct", recent.get(0).getTitle());
        } else {
            summary.put("recentProduct", "No recent equipment");
        }
        return summary;
    }

    @GetMapping("/products")
    public List<MarketplaceListing> getProducts(@RequestParam(required = false) String category) {
        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All")) {
            return listingRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category.trim(), "ACTIVE");
        }
        return listingRepository.findByStatusOrderByCreatedAtDesc("ACTIVE");
    }

    @GetMapping("/listings")
    public List<MarketplaceListing> getListings(@RequestParam(required = false) String category) {
        return getProducts(category);
    }

    @PostMapping("/listings")
    @Transactional
    public ResponseEntity<?> createListing(
            @RequestBody Map<String, Object> payload,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required to post a listing."));
        }
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found."));
        }

        String title = (String) payload.get("title");
        String category = (String) payload.get("category");
        String condition = (String) payload.get("condition");
        String location = (String) payload.get("location");
        Object priceObj = payload.get("price");

        if (title == null || title.trim().isEmpty() || category == null || priceObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title, category, and price are required."));
        }

        double price = 0;
        try {
            price = Double.parseDouble(priceObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid price format."));
        }

        MarketplaceListing listing = new MarketplaceListing();
        listing.setUser(user);
        listing.setSellerName(user.getFullName() != null ? user.getFullName() : "Farmer " + user.getFarmerId());
        listing.setSellerPhone(user.getPhone());
        listing.setTitle(title.trim());
        listing.setCategory(category.trim());
        listing.setCondition(condition != null ? condition.trim() : "Good");
        listing.setPrice(price);
        listing.setLocation(location != null ? location.trim() : (user.getAddress() != null ? user.getAddress() : "Maharashtra"));
        listing.setDescription((String) payload.get("description"));

        String image = (String) payload.get("image");
        if (image == null || image.trim().isEmpty()) {
            image = (String) payload.get("imageUrl");
        }
        if (image == null || image.trim().isEmpty()) {
            Object imgs = payload.get("images");
            if (imgs instanceof List && !((List<?>) imgs).isEmpty()) {
                image = ((List<?>) imgs).get(0).toString();
            }
        }
        if (image == null || image.trim().isEmpty()) {
            image = "assets/farmer.png";
        }
        listing.setImageUrl(image);
        listing.setStatus("ACTIVE");

        MarketplaceListing saved = listingRepository.save(listing);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/listings/{id}")
    @Transactional
    public ResponseEntity<?> deleteListing(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Login required."));
        }
        Long userId = (Long) authentication.getPrincipal();
        MarketplaceListing listing = listingRepository.findById(id).orElse(null);
        if (listing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Listing not found."));
        }

        if (listing.getUser() == null || !listing.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only delete your own listings."));
        }

        listingRepository.delete(listing);
        return ResponseEntity.ok(Map.of("deleted", true, "id", id));
    }
}
