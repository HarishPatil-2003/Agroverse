package com.agroverse;

import com.agroverse.backend.controller.HomeController;
import com.agroverse.backend.controller.UserController;
import com.agroverse.backend.dto.AuthResponse;
import com.agroverse.backend.dto.RegisterRequest;
import com.agroverse.backend.model.Notification;
import com.agroverse.backend.model.User;
import com.agroverse.backend.repository.NotificationRepository;
import com.agroverse.backend.repository.UserRepository;
import com.agroverse.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RegistrationAndPersistenceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserController userController;

    @Autowired
    private HomeController homeController;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    public void testValidRegistrationAndUniqueFarmerId() {
        String email = "farmer_reg_" + System.currentTimeMillis() + "@test.com";

        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Ramesh");
        req.setLastName("Shinde");
        req.setEmail(email);
        req.setPassword("strongPass999");
        req.setPhone("9123456780");
        req.setLocation("Satara, Maharashtra");
        req.setFarmType("Sugarcane");
        req.setLandHolding("4.5 Acres");

        AuthResponse resp = userService.register(req);
        assertNotNull(resp);
        assertNotNull(resp.getToken());

        User user = resp.getUser();
        assertNotNull(user.getId());
        assertTrue(user.getFarmerId().startsWith("AGV-"));
        assertEquals(email, user.getEmail());
        assertEquals("Ramesh Shinde", user.getFullName());
        assertEquals(4.5, user.getLandHolding());

        // Password must be hashed with BCrypt
        assertNotEquals("strongPass999", user.getPassword());
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw("strongPass999", user.getPassword()));

        // Welcome notification must exist in database
        List<Notification> notes = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertFalse(notes.isEmpty(), "Welcome notification must be persisted on registration");
    }

    @Test
    public void testDuplicateEmailRegistrationRejected() {
        String email = "dup_" + System.currentTimeMillis() + "@test.com";

        RegisterRequest req = new RegisterRequest();
        req.setFirstName("First");
        req.setEmail(email);
        req.setPassword("pass1234");
        userService.register(req);

        // Second registration with same email must fail
        RegisterRequest dup = new RegisterRequest();
        dup.setFirstName("Second");
        dup.setEmail(email);
        dup.setPassword("pass1234");

        assertThrows(IllegalArgumentException.class, () -> userService.register(dup));
    }

    @Test
    public void testInvalidRegistrationParameters() {
        // Missing email
        RegisterRequest noEmail = new RegisterRequest();
        noEmail.setPassword("password123");
        assertThrows(IllegalArgumentException.class, () -> userService.register(noEmail));

        // Password under 6 characters
        RegisterRequest shortPwd = new RegisterRequest();
        shortPwd.setEmail("short@test.com");
        shortPwd.setPassword("12345");
        assertThrows(IllegalArgumentException.class, () -> userService.register(shortPwd));
    }

    @Test
    public void testSupabaseIdempotentUserProvisioning() {
        String supabaseUuid = UUID.randomUUID().toString();
        String email = "supa_farmer_" + System.currentTimeMillis() + "@agroverse.test";

        Map<String, Object> meta = Map.of(
                "full_name", "Anil Deshmukh",
                "phone", "9876501234",
                "farm_type", "Organic",
                "land_holding", "7.5",
                "location", "Solapur"
        );

        // 1. Initial resolution provisions new user
        User user1 = userService.resolveOrCreateSupabaseUser(supabaseUuid, email, meta);
        assertNotNull(user1);
        assertEquals(supabaseUuid, user1.getSupabaseUserId());
        assertEquals(email, user1.getEmail());
        assertEquals("Anil Deshmukh", user1.getFullName());
        assertNotNull(user1.getFarmerId());
        assertNull(user1.getPassword(), "Supabase-managed users must not store application passwords");

        // 2. Second resolution is idempotent and returns existing user
        User user2 = userService.resolveOrCreateSupabaseUser(supabaseUuid, email, meta);
        assertEquals(user1.getId(), user2.getId(), "Idempotent lookup must return the exact same user ID");
        assertEquals(user1.getFarmerId(), user2.getFarmerId());

        // 3. Database uniqueness constraints prevent duplicate
        assertEquals(1, userRepository.findBySupabaseUserId(supabaseUuid).stream().count());
    }

    @Test
    public void testRegistration403RegressionResolved() {
        // In the original issue, calling /profile/me failed with 403 when unauthenticated
        // or when registration did not establish proper server authorization.

        // Unauthenticated access returns 401/403
        ResponseEntity<?> unauthResp = userController.getMyProfile(null);
        assertEquals(401, unauthResp.getStatusCode().value());

        // Authenticated farmer calling /profile/me returns 200 OK
        String email = "regression_" + System.currentTimeMillis() + "@test.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword("secret123");
        reg.setFirstName("Regression");
        reg.setLastName("Tester");
        AuthResponse auth = userService.register(reg);

        Authentication authUser = new UsernamePasswordAuthenticationToken(auth.getUser().getId(), null);
        ResponseEntity<?> profileResp = userController.getMyProfile(authUser);
        assertEquals(200, profileResp.getStatusCode().value());
        User fetched = (User) profileResp.getBody();
        assertNotNull(fetched);
        assertEquals(email, fetched.getEmail());
    }

    @Test
    public void testHomeSummaryDerivationFromRealRecords() {
        String email = "summary_test_" + System.currentTimeMillis() + "@test.com";
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword("secret123");
        AuthResponse auth = userService.register(reg);

        Authentication userAuth = new UsernamePasswordAuthenticationToken(auth.getUser().getId(), null);

        // Add 2 real crops
        homeController.addCrop(Map.of("name", "Wheat", "field", "North Block"), userAuth);
        homeController.addCrop(Map.of("name", "Chickpea", "field", "South Block"), userAuth);

        // Add 1 task
        homeController.addTask(Map.of("title", "Check moisture", "type", "field"), userAuth);

        // Fetch home summary
        ResponseEntity<Map<String, Object>> summaryResp = homeController.getHomeSummary(userAuth);
        assertEquals(200, summaryResp.getStatusCode().value());
        Map<String, Object> summary = summaryResp.getBody();
        assertNotNull(summary);

        assertEquals(2L, summary.get("activeCrops"), "Active crops count must match real persisted crops");
        assertEquals(1L, summary.get("tasksToday"), "Tasks today must match real uncompleted tasks");
        assertTrue((Long) summary.get("newNotifications") >= 1L, "New notifications must count real unread notifications");
    }
}
