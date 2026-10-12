package com.agroverse;

import com.agroverse.backend.controller.ChatbotController;
import com.agroverse.backend.controller.HomeController;
import com.agroverse.backend.dto.AuthResponse;
import com.agroverse.backend.dto.ChatRequest;
import com.agroverse.backend.dto.RegisterRequest;
import com.agroverse.backend.model.CropItem;
import com.agroverse.backend.model.User;
import com.agroverse.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SmartCropAndContextIntelligenceTest {

    @Autowired
    private HomeController homeController;

    @Autowired
    private ChatbotController chatbotController;

    @Autowired
    private UserService userService;

    private User farmerOne;
    private User farmerTwo;
    private Authentication authOne;
    private Authentication authTwo;

    @BeforeEach
    public void setup() {
        long ts = System.currentTimeMillis();

        RegisterRequest r1 = new RegisterRequest();
        r1.setFirstName("Ramesh");
        r1.setLastName("Patil");
        r1.setEmail("ramesh_" + ts + "@agroverse.test");
        r1.setPassword("pass12345");
        r1.setLocation("Nashik");
        AuthResponse res1 = userService.register(r1);
        farmerOne = res1.getUser();
        authOne = new UsernamePasswordAuthenticationToken(farmerOne.getId(), null, List.of());

        RegisterRequest r2 = new RegisterRequest();
        r2.setFirstName("Suresh");
        r2.setLastName("Deshmukh");
        r2.setEmail("suresh_" + ts + "@agroverse.test");
        r2.setPassword("pass12345");
        r2.setLocation("Pune");
        AuthResponse res2 = userService.register(r2);
        farmerTwo = res2.getUser();
        authTwo = new UsernamePasswordAuthenticationToken(farmerTwo.getId(), null, List.of());
    }

    @Test
    public void testCropLifecycleCreateUpdateArchiveAndIsolation() {
        // 1. Create a crop with smart lifecycle fields
        Map<String, String> payload = Map.of(
                "name", "Tomato",
                "variety", "Vaishali Hybrid",
                "field", "North Acre #1",
                "growthStage", "Seedling",
                "sowingDate", "2026-03-01",
                "expectedHarvestDate", "2026-06-15",
                "irrigationNotes", "Drip 45 min every 2 days",
                "fertilizerNotes", "NPK 19:19:19 applied"
        );

        ResponseEntity<?> createRes = homeController.addCrop(payload, authOne);
        assertEquals(HttpStatus.CREATED, createRes.getStatusCode());
        CropItem created = (CropItem) createRes.getBody();
        assertNotNull(created);
        assertEquals("Tomato", created.getName());
        assertEquals("Vaishali Hybrid", created.getVariety());
        assertEquals("Seedling", created.getGrowthStage());
        assertEquals("2026-03-01", created.getSowingDate().toString());
        assertFalse(created.isArchived());

        Long cropId = created.getId();

        // 2. Update Growth Stage to Flowering and add treatment notes
        Map<String, String> updatePayload = Map.of(
                "growthStage", "Flowering",
                "treatmentNotes", "Neem oil spray for whiteflies"
        );
        ResponseEntity<?> updateRes = homeController.updateCrop(cropId, updatePayload, authOne);
        assertEquals(HttpStatus.OK, updateRes.getStatusCode());
        CropItem updated = (CropItem) updateRes.getBody();
        assertEquals("Flowering", updated.getGrowthStage());
        assertEquals("Neem oil spray for whiteflies", updated.getTreatmentNotes());

        // 3. User Two should be FORBIDDEN from updating User One's crop
        ResponseEntity<?> forbiddenRes = homeController.updateCrop(cropId, Map.of("growthStage", "Critical"), authTwo);
        assertEquals(HttpStatus.FORBIDDEN, forbiddenRes.getStatusCode());

        // 4. Archive crop
        ResponseEntity<?> archiveRes = homeController.toggleArchiveCrop(cropId, authOne);
        assertEquals(HttpStatus.OK, archiveRes.getStatusCode());
        CropItem archived = (CropItem) archiveRes.getBody();
        assertTrue(archived.isArchived());

        // 5. Query active vs all crops for User One
        ResponseEntity<List<CropItem>> activeCrops = homeController.getCrops(false, authOne);
        assertTrue(activeCrops.getBody().stream().noneMatch(c -> c.getId().equals(cropId)));

        ResponseEntity<List<CropItem>> allCrops = homeController.getCrops(true, authOne);
        assertTrue(allCrops.getBody().stream().anyMatch(c -> c.getId().equals(cropId)));
    }

    @Test
    public void testContextAwareChatbotInvocation() {
        // Invoke Chatbot with contextual field data
        Map<String, Object> context = Map.of(
                "cropName", "Tomato",
                "variety", "Vaishali",
                "growthStage", "Flowering",
                "location", "Nashik, Maharashtra",
                "weather", "28°C, Humidity 60%",
                "recentDiagnosis", "Early Blight (Low Severity)"
        );

        ChatRequest request = new ChatRequest("What is the best irrigation advice right now?", "en", context);
        ResponseEntity<Map<String, Object>> response = chatbotController.chat(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("reply") || response.getBody().containsKey("status"));
    }
}
