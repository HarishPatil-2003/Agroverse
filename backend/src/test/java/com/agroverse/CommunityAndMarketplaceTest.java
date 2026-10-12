package com.agroverse;

import com.agroverse.backend.controller.CommunityController;
import com.agroverse.backend.controller.MarketplaceController;
import com.agroverse.backend.dto.AuthResponse;
import com.agroverse.backend.dto.RegisterRequest;
import com.agroverse.backend.model.CommunityPost;
import com.agroverse.backend.model.MarketplaceListing;
import com.agroverse.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CommunityAndMarketplaceTest {

    @Autowired
    private CommunityController communityController;

    @Autowired
    private MarketplaceController marketplaceController;

    @Autowired
    private UserService userService;

    @Test
    public void testCommunityPostCreationAndListing() {
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Community");
        reg.setLastName("Tester");
        reg.setEmail("community_" + System.currentTimeMillis() + "@test.com");
        reg.setPassword("secret123");
        AuthResponse auth = userService.register(reg);

        Authentication userAuth = new UsernamePasswordAuthenticationToken(auth.getUser().getId(), null);

        // Create post
        ResponseEntity<?> response = communityController.createPost(
                Map.of("title", "Best Fertilizer for Onions", "content", "Looking for organic recommendations.", "category", "Organic"),
                userAuth
        );
        assertEquals(201, response.getStatusCode().value());
        CommunityPost post = (CommunityPost) response.getBody();
        assertNotNull(post);
        assertEquals("Best Fertilizer for Onions", post.getTitle());

        // List posts
        ResponseEntity<List<CommunityPost>> list = communityController.getPosts(null);
        assertTrue(list.getBody().size() >= 1);
    }

    @Test
    public void testMarketplaceListingCreationAndOwnership() {
        RegisterRequest reg1 = new RegisterRequest();
        reg1.setFirstName("Seller");
        reg1.setLastName("One");
        reg1.setEmail("seller_" + System.currentTimeMillis() + "@test.com");
        reg1.setPassword("secret123");
        AuthResponse seller = userService.register(reg1);

        Authentication sellerAuth = new UsernamePasswordAuthenticationToken(seller.getUser().getId(), null);

        // Create listing
        ResponseEntity<?> resp = marketplaceController.createListing(
                Map.of("title", "Rotavator 6ft", "category", "Implements", "condition", "Good", "price", 65000, "location", "Nashik"),
                sellerAuth
        );
        assertEquals(201, resp.getStatusCode().value());
        MarketplaceListing listing = (MarketplaceListing) resp.getBody();
        assertNotNull(listing);

        // Other user cannot delete
        Authentication otherAuth = new UsernamePasswordAuthenticationToken(9999L, null);
        ResponseEntity<?> delResp = marketplaceController.deleteListing(listing.getId(), otherAuth);
        assertEquals(403, delResp.getStatusCode().value());

        // Owner can delete
        ResponseEntity<?> ownerDel = marketplaceController.deleteListing(listing.getId(), sellerAuth);
        assertEquals(200, ownerDel.getStatusCode().value());
    }
}
