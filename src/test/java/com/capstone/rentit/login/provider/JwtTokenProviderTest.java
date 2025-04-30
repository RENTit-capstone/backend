package com.capstone.rentit.login.provider;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    // HS512 용 512비트(64바이트) 키: UTF-8 기준 1글자=1바이트
    private static final String SECRET =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final long EXPIRATION_MS = 60 * 60 * 1000L;  // 0.5초

    private JwtTokenProvider provider;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
        auth = new UsernamePasswordAuthenticationToken("testuser", null);
    }

    @Test
    @DisplayName("generateToken → 유효한 액세스 토큰 생성 및 검증")
    void generateToken_shouldReturnValidToken() {
        String token = provider.generateToken(auth);

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromJWT(token)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("generateRefreshToken → 유효한 리프레시 토큰 생성 및 검증")
    void generateRefreshToken_shouldReturnValidRefreshToken() {
        String refreshToken = provider.generateRefreshToken(auth);

        assertThat(refreshToken).isNotBlank();
        assertThat(provider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(provider.getUsernameFromRefreshToken(refreshToken)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("validateToken → 잘못된 토큰이면 false 반환")
    void validateToken_invalidToken() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("validateRefreshToken → 잘못된 리프레시 토큰이면 false 반환")
    void validateRefreshToken_invalidToken() {
        assertThat(provider.validateRefreshToken("bad-token")).isFalse();
    }

    @Test
    @DisplayName("validateToken → 만료된 토큰이면 false 반환")
    void validateToken_expiredToken() throws InterruptedException {
        // 만료 시간을 1ms로 짧게 설정한 프로바이더
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 1L);
        String shortToken = shortLived.generateToken(auth);

        // 토큰 만료 대기
        Thread.sleep(5L);

        assertThat(shortLived.validateToken(shortToken)).isFalse();
    }

    @Test
    @DisplayName("getUsernameFromJWT → 잘못된 형식 토큰이면 JwtException 발생")
    void getUsernameFromJWT_invalidToken() {
        assertThatThrownBy(() -> provider.getUsernameFromJWT("invalid.jwt.token"))
                .isInstanceOf(JwtException.class);
    }
}