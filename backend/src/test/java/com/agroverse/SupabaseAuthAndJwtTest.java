package com.agroverse;

import com.agroverse.backend.config.JwtTokenProvider;
import com.agroverse.backend.config.SupabaseJwksService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SupabaseAuthAndJwtTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SupabaseJwksService supabaseJwksService;

    @Test
    public void testValidAsymmetricEs256SupabaseTokenAccepted() throws Exception {
        // Generate test EC P-256 keypair matching Supabase's ES256 signing algorithm
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = kpg.generateKeyPair();

        String kid = "test-ec-kid-" + UUID.randomUUID();
        supabaseJwksService.registerKey(kid, keyPair.getPublic());

        String supabaseUuid = UUID.randomUUID().toString();
        String email = "farmer_verified@test.com";

        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600_000);

        String token = Jwts.builder()
                .header().keyId(kid).and()
                .subject(supabaseUuid)
                .claim("email", email)
                .claim("role", "authenticated")
                .claim("user_metadata", Map.of("full_name", "Kisan Patil", "phone", "9876543210"))
                .audience().add("authenticated").and()
                .issuedAt(now)
                .expiration(expiry)
                .signWith(keyPair.getPrivate(), Jwts.SIG.ES256)
                .compact();

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token), "Valid ES256 Supabase token must be accepted");
        assertEquals(supabaseUuid, jwtTokenProvider.getSubjectFromToken(token));
        assertEquals(email, jwtTokenProvider.getEmailFromToken(token));
        assertEquals("ROLE_AUTHENTICATED", jwtTokenProvider.getRoleFromToken(token));

        Map<String, Object> meta = jwtTokenProvider.getUserMetadata(token);
        assertNotNull(meta);
        assertEquals("Kisan Patil", meta.get("full_name"));
    }

    @Test
    public void testExpiredTokenRejected() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = kpg.generateKeyPair();

        String kid = "test-expired-kid-" + UUID.randomUUID();
        supabaseJwksService.registerKey(kid, keyPair.getPublic());

        Date past = new Date(System.currentTimeMillis() - 7200_000);
        Date expiredAt = new Date(System.currentTimeMillis() - 3600_000);

        String expiredToken = Jwts.builder()
                .header().keyId(kid).and()
                .subject("expired-user-uuid")
                .claim("email", "expired@test.com")
                .audience().add("authenticated").and()
                .issuedAt(past)
                .expiration(expiredAt)
                .signWith(keyPair.getPrivate(), Jwts.SIG.ES256)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(expiredToken), "Expired JWT token must be rejected");
    }

    @Test
    public void testTamperedSignatureTokenRejected() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = kpg.generateKeyPair();

        String kid = "test-tampered-kid-" + UUID.randomUUID();
        supabaseJwksService.registerKey(kid, keyPair.getPublic());

        String token = Jwts.builder()
                .header().keyId(kid).and()
                .subject("legit-user-uuid")
                .claim("email", "legit@test.com")
                .audience().add("authenticated").and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.ES256)
                .compact();

        // Tampering signature bytes
        String tamperedToken = token.substring(0, token.length() - 5) + "ABCDE";
        assertFalse(jwtTokenProvider.validateToken(tamperedToken), "Tampered token must be rejected");
    }

    @Test
    public void testInvalidAudienceRejected() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = kpg.generateKeyPair();

        String kid = "test-aud-kid-" + UUID.randomUUID();
        supabaseJwksService.registerKey(kid, keyPair.getPublic());

        String tokenWithWrongAudience = Jwts.builder()
                .header().keyId(kid).and()
                .subject("user-uuid")
                .claim("email", "aud@test.com")
                .audience().add("wrong_audience_service").and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.ES256)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(tokenWithWrongAudience), "Token with invalid audience must be rejected");
    }

    @Test
    public void testMalformedOrNullTokenRejected() {
        assertFalse(jwtTokenProvider.validateToken(null), "Null token must be rejected");
        assertFalse(jwtTokenProvider.validateToken(""), "Empty token must be rejected");
        assertFalse(jwtTokenProvider.validateToken("   "), "Blank token must be rejected");
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt"), "Malformed string must be rejected");
    }
}
