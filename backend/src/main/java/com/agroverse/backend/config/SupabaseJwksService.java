package com.agroverse.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to retrieve, parse, and safely cache Supabase Auth public verification keys (JWKS).
 * Supports cryptographic ES256 (NIST P-256) and RS256 signing keys with safe key-rotation handling.
 */
@Service
public class SupabaseJwksService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseJwksService.class);

    private final String jwksUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile long lastFetchTime = 0;
    private static final long REFRESH_COOLDOWN_MS = 10_000; // 10s cooldown to prevent DoS on bad tokens

    public SupabaseJwksService(
            @Value("${agroverse.supabase.jwks-url:}") String configuredJwksUrl,
            @Value("${agroverse.supabase.url:https://ejqmibvsdyffohytfibl.supabase.co}") String supabaseUrl
    ) {
        if (configuredJwksUrl != null && !configuredJwksUrl.trim().isEmpty()) {
            this.jwksUrl = configuredJwksUrl.trim();
        } else {
            String baseUrl = supabaseUrl != null ? supabaseUrl.trim().replaceAll("/+$", "") : "";
            this.jwksUrl = baseUrl + "/auth/v1/.well-known/jwks.json";
        }

        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();

        // Initial warm up if URL is valid
        refreshJwksSafely();
    }

    public PublicKey getKey(String kid) {
        if (kid == null) {
            // If only one key exists in cache, default to it
            if (keyCache.size() == 1) {
                return keyCache.values().iterator().next();
            }
            return null;
        }

        PublicKey key = keyCache.get(kid);
        if (key != null) {
            return key;
        }

        // Key rotation: try refreshing JWKS if not recently fetched
        long now = System.currentTimeMillis();
        if (now - lastFetchTime > REFRESH_COOLDOWN_MS) {
            refreshJwksSafely();
            return keyCache.get(kid);
        }

        return null;
    }

    public synchronized void refreshJwksSafely() {
        if (jwksUrl == null || jwksUrl.isBlank() || jwksUrl.contains("placeholder")) {
            return;
        }

        lastFetchTime = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                try (InputStream is = response.body()) {
                    JsonNode root = objectMapper.readTree(is);
                    JsonNode keysNode = root.get("keys");
                    if (keysNode != null && keysNode.isArray()) {
                        int loaded = 0;
                        for (JsonNode keyNode : keysNode) {
                            String kid = keyNode.path("kid").asText(null);
                            String kty = keyNode.path("kty").asText("");
                            PublicKey publicKey = null;

                            if ("EC".equalsIgnoreCase(kty)) {
                                String x = keyNode.path("x").asText();
                                String y = keyNode.path("y").asText();
                                String crv = keyNode.path("crv").asText("P-256");
                                publicKey = parseEcPublicKey(x, y, crv);
                            } else if ("RSA".equalsIgnoreCase(kty)) {
                                String n = keyNode.path("n").asText();
                                String e = keyNode.path("e").asText();
                                publicKey = parseRsaPublicKey(n, e);
                            }

                            if (publicKey != null) {
                                if (kid != null) {
                                    keyCache.put(kid, publicKey);
                                }
                                keyCache.put("default", publicKey);
                                loaded++;
                            }
                        }
                        logger.info("Loaded and cached {} public verification key(s) from Supabase JWKS: {}", loaded, jwksUrl);
                    }
                }
            } else {
                logger.warn("Supabase JWKS endpoint returned HTTP {}: {}", response.statusCode(), jwksUrl);
            }
        } catch (Exception e) {
            logger.warn("Could not fetch Supabase JWKS from {}: {} (Offline/mock mode active)", jwksUrl, e.getMessage());
        }
    }

    public void registerKey(String kid, PublicKey key) {
        if (kid != null && key != null) {
            keyCache.put(kid, key);
            keyCache.put("default", key);
        }
    }

    public Map<String, PublicKey> getCachedKeys() {
        return keyCache;
    }

    private PublicKey parseEcPublicKey(String xB64, String yB64, String crv) throws Exception {
        byte[] xBytes = Base64.getUrlDecoder().decode(xB64);
        byte[] yBytes = Base64.getUrlDecoder().decode(yB64);
        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);
        ECPoint point = new ECPoint(x, y);

        String curveName = "secp256r1"; // standard for P-256
        if ("P-384".equalsIgnoreCase(crv)) curveName = "secp384r1";
        else if ("P-521".equalsIgnoreCase(crv)) curveName = "secp521r1";

        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec(curveName));
        ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);

        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(new ECPublicKeySpec(point, ecSpec));
    }

    private PublicKey parseRsaPublicKey(String nB64, String eB64) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(nB64);
        byte[] eBytes = Base64.getUrlDecoder().decode(eB64);
        BigInteger n = new BigInteger(1, nBytes);
        BigInteger e = new BigInteger(1, eBytes);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
