package com.agroverse.backend.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * Unified JWT provider and validator supporting:
 * 1. Supabase Auth asymmetric tokens (ES256 / RS256) via JWKS.
 * 2. Supabase / Local symmetric tokens (HS256) via HMAC SecretKey.
 * Validates cryptographic signatures, issuer, audience, and expiration.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey hmacKey;
    private final long expirationMs;
    private final SupabaseJwksService jwksService;
    private final String expectedIssuer;
    private final String expectedAudience;

    public JwtTokenProvider(
            @Value("${agroverse.jwt.secret:agroverse-production-secure-hmac-sha256-key-with-minimum-32-characters!}") String secret,
            @Value("${agroverse.jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${agroverse.supabase.jwt-issuer:}") String expectedIssuer,
            @Value("${agroverse.supabase.jwt-audience:authenticated}") String expectedAudience,
            SupabaseJwksService jwksService
    ) {
        this.expirationMs = expirationMs;
        this.jwksService = jwksService;
        this.expectedIssuer = expectedIssuer != null ? expectedIssuer.trim() : "";
        this.expectedAudience = expectedAudience != null ? expectedAudience.trim() : "";

        // Pad key to at least 256 bits (32 bytes) for HS256 compatibility
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            for (int i = keyBytes.length; i < 32; i++) {
                padded[i] = (byte) ('0' + (i % 10));
            }
            this.hmacKey = Keys.hmacShaKeyFor(padded);
        } else {
            this.hmacKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    private Locator<Key> createKeyLocator() {
        return header -> {
            if (header instanceof JwsHeader jwsHeader) {
                String alg = jwsHeader.getAlgorithm();
                String kid = jwsHeader.getKeyId();

                if ("ES256".equalsIgnoreCase(alg) || "RS256".equalsIgnoreCase(alg)) {
                    Key key = jwksService.getKey(kid);
                    if (key != null) return key;
                } else if ("HS256".equalsIgnoreCase(alg)) {
                    return hmacKey;
                }
            }
            // Fallback for HS256 tokens without explicit JWS header alg match
            return hmacKey;
        };
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            Claims claims = Jwts.parser()
                    .keyLocator(createKeyLocator())
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();

            // Validate Subject
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                logger.warn("JWT rejected: Missing subject (sub) claim");
                return false;
            }

            // Validate Issuer if explicitly configured
            if (!expectedIssuer.isEmpty() && claims.getIssuer() != null) {
                if (!claims.getIssuer().equals(expectedIssuer) && !claims.getIssuer().startsWith(expectedIssuer)) {
                    logger.warn("JWT rejected: Unexpected issuer {}", claims.getIssuer());
                    return false;
                }
            }

            // Validate Audience if claim is present
            if (!expectedAudience.isEmpty() && claims.getAudience() != null) {
                if (!claims.getAudience().contains(expectedAudience)) {
                    logger.warn("JWT rejected: Unexpected audience {}", claims.getAudience());
                    return false;
                }
            }

            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("Expired JWT token: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .keyLocator(createKeyLocator())
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }

    public String getSubjectFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        String sub = claims.getSubject();
        try {
            return Long.parseLong(sub);
        } catch (NumberFormatException e) {
            // Sub is a UUID (Supabase Auth). Return null so filter can resolve DB user ID via sub
            Object dbId = claims.get("dbId");
            if (dbId instanceof Number) {
                return ((Number) dbId).longValue();
            }
            return null;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaims(token);
        String email = claims.get("email", String.class);
        if (email != null && !email.isBlank()) {
            return email;
        }
        String sub = claims.getSubject();
        if (sub != null && sub.contains("@")) {
            return sub;
        }
        return null;
    }

    public String getFarmerIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("farmerId", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            role = "ROLE_FARMER";
        } else if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role.toUpperCase();
        }
        return role;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserMetadata(String token) {
        Claims claims = getClaims(token);
        return claims.get("user_metadata", Map.class);
    }

    public String generateToken(Long userId, String email, String farmerId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("farmerId", farmerId)
                .claim("role", role != null ? role : "ROLE_FARMER")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(hmacKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateSupabaseStyleToken(String supabaseUuid, String email, String role, long customExpMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (customExpMs > 0 ? customExpMs : expirationMs));

        var builder = Jwts.builder()
                .subject(supabaseUuid)
                .claim("email", email)
                .claim("role", role != null ? role : "authenticated")
                .audience().add(expectedAudience.isEmpty() ? "authenticated" : expectedAudience).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(hmacKey, Jwts.SIG.HS256);

        if (!expectedIssuer.isEmpty()) {
            builder.issuer(expectedIssuer);
        }

        return builder.compact();
    }
}
