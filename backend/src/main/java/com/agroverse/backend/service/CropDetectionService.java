package com.agroverse.backend.service;

import ai.onnxruntime.*;
import com.agroverse.backend.model.CropAnalysisHistory;
import com.agroverse.backend.model.User;
import com.agroverse.backend.repository.CropAnalysisRepository;
import com.agroverse.backend.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;

@Service
public class CropDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(CropDetectionService.class);

    private final CropAnalysisRepository cropAnalysisRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${krishi.onnx.confidence-threshold:0.40}")
    private double confidenceThreshold;

    private OrtEnvironment env;
    private OrtSession varietySession;
    private OrtSession diseaseSession;

    private List<String> varietyLabels = new ArrayList<>();
    private List<String> diseaseLabels = new ArrayList<>();
    private Map<String, Map<String, Map<String, Object>>> treatments = new HashMap<>();

    private boolean modelsLoaded = false;
    private String modelInitError = null;

    public CropDetectionService(
            CropAnalysisRepository cropAnalysisRepository,
            UserRepository userRepository
    ) {
        this.cropAnalysisRepository = cropAnalysisRepository;
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public synchronized void init() {
        try {
            logger.info("Initializing Canonical Java ONNX Inference Engine for AgroVerse...");
            this.env = OrtEnvironment.getEnvironment();

            // Load Labels
            this.varietyLabels = loadLabels("models/variety_labels.json", Arrays.asList(
                    "Wheat", "Bajra", "Corn", "Cotton", "Mango", "Soybean", "Rice"
            ));
            this.diseaseLabels = loadLabels("models/disease_labels.json", Arrays.asList(
                    "Healthy", "Leaf Blight", "Powdery Mildew", "Rust", "Bacterial Spot"
            ));

            // Load Treatments
            loadTreatments("models/treatments.json");

            // Load ONNX Sessions
            ClassPathResource varietyRes = new ClassPathResource("models/crop_variety.onnx");
            ClassPathResource diseaseRes = new ClassPathResource("models/crop_disease.onnx");

            if (varietyRes.exists() && diseaseRes.exists()) {
                byte[] vBytes = varietyRes.getInputStream().readAllBytes();
                byte[] dBytes = diseaseRes.getInputStream().readAllBytes();
                this.varietySession = env.createSession(vBytes, new OrtSession.SessionOptions());
                this.diseaseSession = env.createSession(dBytes, new OrtSession.SessionOptions());
                this.modelsLoaded = true;
                logger.info("Successfully loaded crop variety and disease ONNX models in Java.");
            } else {
                this.modelInitError = "ONNX model files not found in models/ directory.";
                logger.warn("ONNX models missing: {}", modelInitError);
            }
        } catch (Throwable t) {
            this.modelInitError = t.getMessage();
            logger.error("Failed to initialize ONNX inference sessions: {}", t.getMessage(), t);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (varietySession != null) varietySession.close();
            if (diseaseSession != null) diseaseSession.close();
            if (env != null) env.close();
        } catch (Exception ignored) {}
    }

    public boolean isModelReady() {
        return modelsLoaded && varietySession != null && diseaseSession != null;
    }

    public Map<String, Object> analyze(MultipartFile file, String language, Long userId, String farmerIdParam) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No image file provided.");
        }

        // 1. Content-Type & Size Validation
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 10MB limit.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File is not a valid image format.");
        }

        // 2. Decode Image and Validate Dimensions (Decompression Bomb Protection)
        BufferedImage originalImage;
        try (InputStream is = file.getInputStream()) {
            originalImage = ImageIO.read(is);
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed image file. Could not decode image.");
        }

        if (originalImage == null) {
            throw new IllegalArgumentException("Unsupported image format or corrupt file data.");
        }

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        if (width > 4096 || height > 4096) {
            throw new IllegalArgumentException("Image dimensions (" + width + "x" + height + ") exceed maximum supported 4096x4096px.");
        }

        if (!isModelReady()) {
            Map<String, Object> errResponse = new HashMap<>();
            errResponse.put("success", false);
            errResponse.put("error", "AI Inference Model is currently unavailable: " + (modelInitError != null ? modelInitError : "Loading in progress"));
            errResponse.put("modelsOffline", true);
            return errResponse;
        }

        try {
            // 3. Preprocess Image to 224x224 RGB Float Tensor (NCHW: [1, 3, 224, 224])
            float[] tensorData = preprocessImage(originalImage, 224, 224);

            // 4. Variety Inference
            long[] shape = new long[]{1, 3, 224, 224};
            OnnxTensor imageTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(tensorData), shape);

            OrtSession.Result vResult = varietySession.run(Collections.singletonMap("image", imageTensor));
            float[][] vLogits = (float[][]) vResult.get("logits").get().getValue();
            float[][] vEmbedding = (float[][]) vResult.get("embedding").get().getValue();

            float[] vProbs = softmax(vLogits[0]);
            int vIdx = argmax(vProbs);
            double vConfidence = vProbs[vIdx];
            String predictedVariety = (vIdx < varietyLabels.size()) ? varietyLabels.get(vIdx) : "Unknown";

            // 5. Disease Inference
            float[][] varietyOneHot = new float[1][varietyLabels.size()];
            if (vIdx < varietyLabels.size()) {
                varietyOneHot[0][vIdx] = 1.0f;
            }

            OnnxTensor embeddingTensor = OnnxTensor.createTensor(env, vEmbedding);
            OnnxTensor varietyOneHotTensor = OnnxTensor.createTensor(env, varietyOneHot);

            Map<String, OnnxTensor> dInputs = new HashMap<>();
            dInputs.put("embedding", embeddingTensor);
            dInputs.put("variety_one_hot", varietyOneHotTensor);

            OrtSession.Result dResult = diseaseSession.run(dInputs);
            float[][] dLogits = (float[][]) dResult.get("logits").get().getValue();
            float[] dProbs = softmax(dLogits[0]);
            int dIdx = argmax(dProbs);
            double dConfidence = dProbs[dIdx];
            String predictedDisease = (dIdx < diseaseLabels.size()) ? diseaseLabels.get(dIdx) : "Unknown";

            // 6. Calibrated Confidence & Diagnostic Certainty
            double rawScore = Math.max(vConfidence, dConfidence);
            double displayConfidence = Math.round(rawScore * 1000.0) / 10.0;

            String diagnosticCertainty;
            String severity;
            String finalCrop = predictedVariety;
            String finalDisease = predictedDisease;

            if (rawScore < confidenceThreshold) {
                diagnosticCertainty = "Low Confidence / Uncertain";
                severity = "Low Confidence";
                finalCrop = "Uncertain";
                finalDisease = "Uncertain (Retake Photo)";
            } else if (rawScore < 0.70) {
                diagnosticCertainty = "Moderate Confidence (Preliminary)";
                severity = predictedDisease.equalsIgnoreCase("Healthy") ? "None" : "Warning";
            } else {
                diagnosticCertainty = "High Confidence (Strong Match)";
                severity = predictedDisease.equalsIgnoreCase("Healthy") ? "None" : "Alert";
            }

            // 7. Localized Treatment Advisory
            String langKey = (language != null && (language.equals("hi") || language.equals("mr"))) ? language : "en";
            String labelKey = (finalCrop.toLowerCase() + "_" + finalDisease.toLowerCase().replace(" ", "_"));

            Map<String, Object> localizedData = getLocalizedTreatment(labelKey, langKey);
            String description = (String) localizedData.getOrDefault("description", "Maintain balanced crop nutrition and observe foliage regularly.");
            @SuppressWarnings("unchecked")
            List<String> treatmentList = (List<String>) localizedData.getOrDefault("treatment", Arrays.asList(
                    "Ensure adequate field drainage.",
                    "Consult your local Krishi Vigyan Kendra (KVK) officer for field verification."
            ));

            String pesticideWarning = "Advisory Notice: Wear protective mask and gloves before spraying agrochemicals. Always adhere to Central Insecticides Board & Registration Committee (CIBRC) approved dosage rates.";

            // 8. Find User & Farmer ID
            User user = null;
            String farmerId = farmerIdParam != null ? farmerIdParam : "AGV-DEMO";
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    farmerId = user.getFarmerId();
                }
            }

            // 9. Persist Analysis Record
            CropAnalysisHistory history = new CropAnalysisHistory();
            history.setUser(user);
            history.setFarmerId(farmerId);
            history.setFileName(file.getOriginalFilename());
            history.setCrop(finalCrop);
            history.setDisease(finalDisease);
            history.setConfidence(displayConfidence);
            history.setDiagnosticCertainty(diagnosticCertainty);
            history.setSeverity(severity);
            history.setDescription(description);
            history.setPesticideWarning(pesticideWarning);
            try {
                history.setTreatmentsJson(objectMapper.writeValueAsString(treatmentList));
            } catch (Exception ignored) {}

            CropAnalysisHistory saved = cropAnalysisRepository.save(history);

            // 10. Construct Final Result Map
            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("success", true);
            response.put("crop", finalCrop);
            response.put("disease", finalDisease);
            response.put("confidence", displayConfidence);
            response.put("diagnosticCertainty", diagnosticCertainty);
            response.put("severity", severity);
            response.put("description", description);
            response.put("treatments", treatmentList);
            response.put("pesticideWarning", pesticideWarning);
            response.put("farmerId", farmerId);
            response.put("fileName", file.getOriginalFilename());
            response.put("timestamp", saved.getCreatedAt().toString());

            return response;

        } catch (Exception e) {
            logger.error("Inference execution failed: {}", e.getMessage(), e);
            Map<String, Object> errResponse = new HashMap<>();
            errResponse.put("success", false);
            errResponse.put("error", "Crop diagnosis pipeline encountered an error: " + e.getMessage());
            return errResponse;
        }
    }

    public List<CropAnalysisHistory> getUserHistory(Long userId) {
        if (userId == null) return Collections.emptyList();
        return cropAnalysisRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private float[] preprocessImage(BufferedImage img, int targetW, int targetH) {
        BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, targetW, targetH, null);
        g.dispose();

        float[] data = new float[3 * targetW * targetH];
        float[] mean = new float[]{0.485f, 0.456f, 0.406f};
        float[] std = new float[]{0.229f, 0.224f, 0.225f};

        int channelSize = targetW * targetH;
        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                int rgb = resized.getRGB(x, y);
                float r = ((rgb >> 16) & 0xFF) / 255.0f;
                float gVal = ((rgb >> 8) & 0xFF) / 255.0f;
                float b = (rgb & 0xFF) / 255.0f;

                int idx = y * targetW + x;
                data[idx] = (r - mean[0]) / std[0];
                data[channelSize + idx] = (gVal - mean[1]) / std[1];
                data[2 * channelSize + idx] = (b - mean[2]) / std[2];
            }
        }
        return data;
    }

    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) if (v > max) max = v;

        float sum = 0f;
        float[] exp = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max);
            sum += exp[i];
        }
        for (int i = 0; i < logits.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    private int argmax(float[] arr) {
        int bestIdx = 0;
        float maxVal = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > maxVal) {
                maxVal = arr[i];
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private List<String> loadLabels(String resourcePath, List<String> fallback) {
        try {
            ClassPathResource res = new ClassPathResource(resourcePath);
            if (res.exists()) {
                return objectMapper.readValue(res.getInputStream(), new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            logger.warn("Could not load labels from {}: {}", resourcePath, e.getMessage());
        }
        return fallback;
    }

    private void loadTreatments(String resourcePath) {
        try {
            ClassPathResource res = new ClassPathResource(resourcePath);
            if (res.exists()) {
                this.treatments = objectMapper.readValue(res.getInputStream(),
                        new TypeReference<Map<String, Map<String, Map<String, Object>>>>() {});
                logger.info("Loaded {} treatment entries.", treatments.size());
            }
        } catch (Exception e) {
            logger.warn("Could not load treatments from {}: {}", resourcePath, e.getMessage());
        }
    }

    private Map<String, Object> getLocalizedTreatment(String labelKey, String lang) {
        Map<String, Map<String, Object>> entry = treatments.get(labelKey);
        if (entry == null) {
            entry = treatments.get("unknown");
        }
        if (entry != null) {
            Map<String, Object> langData = entry.get(lang);
            if (langData != null) return langData;
            Map<String, Object> enData = entry.get("en");
            if (enData != null) return enData;
        }
        Map<String, Object> def = new HashMap<>();
        def.put("description", "Crop requires field verification. Monitor moisture and nutrient balance.");
        def.put("treatment", Arrays.asList(
                "Maintain healthy organic soil conditions.",
                "Consult agricultural specialist if symptoms spread."
        ));
        return def;
    }
}
