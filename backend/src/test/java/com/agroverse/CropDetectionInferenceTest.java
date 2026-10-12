package com.agroverse;

import com.agroverse.backend.service.CropDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CropDetectionInferenceTest {

    @Autowired
    private CropDetectionService cropDetectionService;

    @Test
    public void testModelIsLoaded() {
        assertTrue(cropDetectionService.isModelReady(), "Canonical Java ONNX models must be loaded and ready.");
    }

    @Test
    public void testInferencePipelineWithSyntheticImage() throws Exception {
        // Generate a 224x224 green leaf synthetic test image
        BufferedImage img = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(34, 139, 34)); // Forest green
        g.fillRect(0, 0, 224, 224);
        g.setColor(new Color(139, 69, 19)); // Brown leaf spot simulation
        g.fillOval(50, 50, 40, 40);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] bytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "image", "leaf_test.png", "image/png", bytes
        );

        Map<String, Object> result = cropDetectionService.analyze(file, "en", null, "AGV-TEST-001");
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertNotNull(result.get("crop"));
        assertNotNull(result.get("disease"));
        assertNotNull(result.get("confidence"));
        assertNotNull(result.get("diagnosticCertainty"));
        assertNotNull(result.get("severity"));
        assertNotNull(result.get("treatments"));
        assertNotNull(result.get("pesticideWarning"));
    }

    @Test
    public void testValidationRejectsInvalidFiles() {
        // Empty file
        MockMultipartFile emptyFile = new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> cropDetectionService.analyze(emptyFile, "en", null, null));

        // Corrupt file
        MockMultipartFile corruptFile = new MockMultipartFile("image", "bad.png", "image/png", new byte[]{1, 2, 3, 4, 5});
        assertThrows(IllegalArgumentException.class, () -> cropDetectionService.analyze(corruptFile, "en", null, null));
    }
}
