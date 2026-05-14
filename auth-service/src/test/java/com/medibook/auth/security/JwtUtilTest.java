package com.medibook.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private final String SECRET = "mySuperSecretKeyForJwtWhichIsAtLeast32CharactersLong";
    private final long EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    void testGenerateTokenAndExtractors() {
        String token = jwtUtil.generateToken("test@example.com", "ROLE_PATIENT", 1L);

        assertNotNull(token);

        String extractedEmail = jwtUtil.extractUsername(token);
        assertEquals("test@example.com", extractedEmail);

        String extractedRole = jwtUtil.extractRole(token);
        assertEquals("ROLE_PATIENT", extractedRole);

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testExtractRole_adminRole() {
        String token = jwtUtil.generateToken("admin@example.com", "ADMIN", 2L);

        assertEquals("ADMIN", jwtUtil.extractRole(token));
        assertEquals("admin@example.com", jwtUtil.extractUsername(token));
    }

    @Test
    void testExtractRole_withUserId() {
        String token = jwtUtil.generateToken("user@example.com", "PROVIDER", 99L);

        assertNotNull(token);
        assertEquals("PROVIDER", jwtUtil.extractRole(token));
        assertEquals("user@example.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_Invalid() {
        assertFalse(jwtUtil.validateToken("invalid.token.string"));
    }

    @Test
    void testValidateToken_EmptyString() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void testValidateToken_NullLikeGarbage() {
        assertFalse(jwtUtil.validateToken("not-a-jwt-at-all"));
    }

    @Test
    void testGenerateToken_differentUsers_distinctTokens() {
        String token1 = jwtUtil.generateToken("user1@example.com", "PATIENT", 1L);
        String token2 = jwtUtil.generateToken("user2@example.com", "ADMIN", 2L);

        assertNotEquals(token1, token2);
        assertEquals("user1@example.com", jwtUtil.extractUsername(token1));
        assertEquals("user2@example.com", jwtUtil.extractUsername(token2));
    }
}
