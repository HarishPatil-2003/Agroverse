package com.agroverse;

import com.agroverse.backend.controller.CommunityController;
import com.agroverse.backend.controller.HomeController;
import com.agroverse.backend.controller.MarketplaceController;
import com.agroverse.backend.controller.UserController;
import com.agroverse.backend.dto.AuthResponse;
import com.agroverse.backend.dto.RegisterRequest;
import com.agroverse.backend.model.CommunityPost;
import com.agroverse.backend.model.FarmTask;
import com.agroverse.backend.model.MarketplaceListing;
import com.agroverse.backend.model.User;
import com.agroverse.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AuthorizationAndOwnershipTest {

    @Autowired
    private UserController userController;

    @Autowired
    private MarketplaceController marketplaceController;

    @Autowired
    private CommunityController communityController;

    @Autowired
    private HomeController homeController;

    @Autowired
    private UserService userService;

    private User farmerA;
    private User farmerB;
    private Authentication authA;
    private Authentication authB;

    @BeforeEach
    public void setupFarmers() {
        long ts = System.currentTimeMillis();

        RegisterRequest regA = new RegisterRequest();
        regA.setFirstName("FarmerA");
        regA.setLastName("Patil");
        regA.setEmail("farmer_a_" + ts + "@test.com");
        regA.setPassword("secret123");
        regA.setLocation("Nashik");
        regA.setFarmType("Vineyard");
        AuthResponse resA = userService.register(regA);
        this.farmerA = resA.getUser();
        this.authA = new UsernamePasswordAuthenticationToken(farmerA.getId(), null);

        RegisterRequest regB = new RegisterRequest();
        regB.setFirstName("FarmerB");
        regB.setLastName("Kulkarni");
        regB.setEmail("farmer_b_" + ts + "@test.com");
        regB.setPassword("secret123");
        regB.setLocation("Pune");
        regB.setFarmType("Sugarcane");
        AuthResponse resB = userService.register(regB);
        this.farmerB = resB.getUser();
        this.authB = new UsernamePasswordAuthenticationToken(farmerB.getId(), null);
    }

    @Test
    public void testFarmerCannotReadAnotherFarmersPrivateProfile() {
        // Farmer A viewing own profile -> 200 OK
        ResponseEntity<?> ownProfile = userController.getProfileById(farmerA.getId(), authA);
        assertEquals(200, ownProfile.getStatusCode().value());

        // Farmer A attempting to view Farmer B's private profile -> 403 Forbidden
        ResponseEntity<?> bProfile = userController.getProfileById(farmerB.getId(), authA);
        assertEquals(403, bProfile.getStatusCode().value(), "Farmer A must not be allowed to view Farmer B's private profile");

        // Unauthenticated access -> 401 Unauthorized
        ResponseEntity<?> unauthProfile = userController.getMyProfile(null);
        assertEquals(401, unauthProfile.getStatusCode().value());
    }

    @Test
    public void testFarmerCannotDeleteAnotherFarmersMarketplaceListing() {
        // Farmer B creates a tractor listing
        ResponseEntity<?> listingResp = marketplaceController.createListing(
                Map.of("title", "Tractor 45HP", "category", "Machinery", "condition", "Excellent", "price", 350000),
                authB
        );
        assertEquals(201, listingResp.getStatusCode().value());
        MarketplaceListing listing = (MarketplaceListing) listingResp.getBody();
        assertNotNull(listing);

        // Farmer A attempts to delete Farmer B's listing -> 403 Forbidden
        ResponseEntity<?> unauthorizedDel = marketplaceController.deleteListing(listing.getId(), authA);
        assertEquals(403, unauthorizedDel.getStatusCode().value(), "Farmer A must NOT delete Farmer B's marketplace listing");

        // Farmer B (owner) can delete -> 200 OK
        ResponseEntity<?> ownerDel = marketplaceController.deleteListing(listing.getId(), authB);
        assertEquals(200, ownerDel.getStatusCode().value());
    }

    @Test
    public void testFarmerCannotDeleteAnotherFarmersCommunityPost() {
        // Farmer B creates a post
        ResponseEntity<?> postResp = communityController.createPost(
                Map.of("title", "Water harvesting advice", "content", "How to install drip irrigation?", "category", "Irrigation"),
                authB
        );
        assertEquals(201, postResp.getStatusCode().value());
        CommunityPost post = (CommunityPost) postResp.getBody();
        assertNotNull(post);

        // Farmer A attempts to delete Farmer B's post -> 403 Forbidden
        ResponseEntity<?> unauthorizedDel = communityController.deletePost(post.getId(), authA);
        assertEquals(403, unauthorizedDel.getStatusCode().value(), "Farmer A must NOT delete Farmer B's post");

        // Farmer B (owner) can delete -> 200 OK
        ResponseEntity<?> ownerDel = communityController.deletePost(post.getId(), authB);
        assertEquals(200, ownerDel.getStatusCode().value());
    }

    @Test
    public void testFarmerCannotModifyAnotherFarmersFarmTask() {
        // Farmer B creates a task
        ResponseEntity<?> taskResp = homeController.addTask(
                Map.of("title", "Check drip filter", "subtitle", "Vineyard Block 2", "dueTime", "10:00 AM", "type", "irrigation"),
                authB
        );
        assertEquals(201, taskResp.getStatusCode().value());
        FarmTask task = (FarmTask) taskResp.getBody();
        assertNotNull(task);
        assertFalse(task.isCompleted());

        // Farmer A attempts to complete/toggle Farmer B's task -> 403 Forbidden
        ResponseEntity<?> unauthToggle = homeController.completeTask(task.getId(), authA);
        assertEquals(403, unauthToggle.getStatusCode().value(), "Farmer A must NOT toggle Farmer B's task");

        // Farmer B toggles own task -> 200 OK and completed = true
        ResponseEntity<?> ownerToggle = homeController.completeTask(task.getId(), authB);
        assertEquals(200, ownerToggle.getStatusCode().value());
        FarmTask updated = (FarmTask) ownerToggle.getBody();
        assertTrue(updated.isCompleted());
    }
}
