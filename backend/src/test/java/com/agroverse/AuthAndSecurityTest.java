package com.agroverse;

import com.agroverse.backend.config.JwtTokenProvider;
import com.agroverse.backend.dto.AuthResponse;
import com.agroverse.backend.dto.LoginRequest;
import com.agroverse.backend.dto.RegisterRequest;
import com.agroverse.backend.model.User;
import com.agroverse.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AuthAndSecurityTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testJwtGenerationAndValidation() {
        String token = jwtTokenProvider.generateToken(999L, "farmer99@test.com", "AGV-2026-T99", "ROLE_FARMER");
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));

        assertEquals(999L, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("farmer99@test.com", jwtTokenProvider.getEmailFromToken(token));
        assertEquals("AGV-2026-T99", jwtTokenProvider.getFarmerIdFromToken(token));
        assertEquals("ROLE_FARMER", jwtTokenProvider.getRoleFromToken(token));

        // Tampered token must fail
        assertFalse(jwtTokenProvider.validateToken(token + "tampered"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    public void testRegistrationAndLoginFlow() {
        String testEmail = "farmer_" + System.currentTimeMillis() + "@test.com";

        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("Suresh");
        reg.setLastName("Patil");
        reg.setEmail(testEmail);
        reg.setPassword("secretPassword123");
        reg.setPhone("9876543210");
        reg.setLocation("Pune");
        reg.setFarmType("Cotton");

        AuthResponse regResponse = userService.register(reg);
        assertNotNull(regResponse);
        assertNotNull(regResponse.getToken());
        assertNotNull(regResponse.getUser());
        assertEquals("Suresh Patil", regResponse.getUser().getFullName());
        assertNotNull(regResponse.getUser().getFarmerId());

        // Password hash check: raw password must NOT be in DB
        assertNotEquals("secretPassword123", regResponse.getUser().getPassword());

        // Login with correct credentials
        LoginRequest login = new LoginRequest();
        login.setEmail(testEmail);
        login.setPassword("secretPassword123");
        AuthResponse loginResponse = userService.login(login);
        assertNotNull(loginResponse.getToken());
        assertTrue(jwtTokenProvider.validateToken(loginResponse.getToken()));

        // Login with bad password must throw
        LoginRequest badLogin = new LoginRequest();
        badLogin.setEmail(testEmail);
        badLogin.setPassword("wrongPassword");
        assertThrows(RuntimeException.class, () -> userService.login(badLogin));

        // Duplicate registration with same email must throw
        assertThrows(RuntimeException.class, () -> userService.register(reg), 
            "Registering duplicate email must be rejected");

        // Short password (<6 chars) must throw
        RegisterRequest shortPwdReg = new RegisterRequest();
        shortPwdReg.setEmail("short_" + System.currentTimeMillis() + "@test.com");
        shortPwdReg.setPassword("12345");
        assertThrows(RuntimeException.class, () -> userService.register(shortPwdReg),
            "Password under 6 characters must be rejected");
    }

    @Test
    public void testPasswordNeverExposedInSerialization() throws Exception {
        User user = new User();
        user.setId(10L);
        user.setEmail("safe@test.com");
        user.setPassword("secretHashedValue");

        String json = objectMapper.writeValueAsString(user);
        assertFalse(json.contains("secretHashedValue"), "Password must never be serialized into JSON responses!");
    }
}
