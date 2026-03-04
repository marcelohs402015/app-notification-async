package com.appnotification.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET = "7a9b2c4d6e8f1a3b5c7d9e2f4a6b8c1d3e5f7a9b2c4d6e8f1a3b5c7d9e2f4a6b";
    private static final long TEST_EXPIRATION_MS = 86400000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", TEST_EXPIRATION_MS);
    }

    @Test
    void generateToken_shouldReturnNonBlankToken() {
        UUID inputUserId = UUID.randomUUID();
        String inputEmail = "joao@email.com";

        String actualToken = jwtService.generateToken(inputUserId, inputEmail);

        assertThat(actualToken).isNotBlank();
        assertThat(actualToken.split("\\.")).hasSize(3);
    }

    @Test
    void extractUserId_shouldReturnOriginalUserId() {
        UUID inputUserId = UUID.randomUUID();
        String token = jwtService.generateToken(inputUserId, "joao@email.com");

        UUID actualUserId = jwtService.extractUserId(token);

        assertThat(actualUserId).isEqualTo(inputUserId);
    }

    @Test
    void extractEmail_shouldReturnOriginalEmail() {
        String inputEmail = "joao@email.com";
        String token = jwtService.generateToken(UUID.randomUUID(), inputEmail);

        String actualEmail = jwtService.extractEmail(token);

        assertThat(actualEmail).isEqualTo(inputEmail);
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenIsValid() {
        String token = jwtService.generateToken(UUID.randomUUID(), "joao@email.com");

        boolean actualResult = jwtService.isTokenValid(token);

        assertThat(actualResult).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String expiredToken = jwtService.generateToken(UUID.randomUUID(), "joao@email.com");

        boolean actualResult = jwtService.isTokenValid(expiredToken);

        assertThat(actualResult).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsMalformed() {
        boolean actualResult = jwtService.isTokenValid("token.invalido.mesmo");

        assertThat(actualResult).isFalse();
    }
}
