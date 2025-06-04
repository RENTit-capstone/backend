package com.capstone.rentit.login.provider;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    // ── 상수 ──────────────────────────────────────────────────────────────
    private static final String SECRET =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 64B
    private static final long EXPIRATION_MS    = 60_000;  // 60 초
    private static final long REFRESH_TTL_MIN  =    10;   // 10 분

    // ── Mocks ────────────────────────────────────────────────────────────
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private JwtTokenProvider provider;
    private Authentication   auth;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);   // 공통 스텁

        provider = new JwtTokenProvider(
                SECRET, EXPIRATION_MS, REFRESH_TTL_MIN, redis);

        auth = new UsernamePasswordAuthenticationToken("testuser", null);
    }

    // ── 액세스 토큰 ───────────────────────────────────────────────────────
    @Test @DisplayName("generateToken → 유효한 액세스 토큰 생성·검증")
    void generateToken_shouldReturnValidToken() {
        String token = provider.generateToken(auth);

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo("testuser");
    }

    // ── 리프레시 토큰 ────────────────────────────────────────────────────
    @Test @DisplayName("generateRefreshToken → 유효한 리프레시 토큰 생성·검증")
    void generateRefreshToken_shouldReturnValidRefreshToken() {
        // generateRefreshToken 내부에서 redis.opsForValue().set() 호출
        String refreshToken = provider.generateRefreshToken(auth);

        // 저장 호출 검증
        verify(valueOps).set(
                startsWith("refresh:"), eq("testuser"), eq(Duration.ofMinutes(REFRESH_TTL_MIN))
        );

        // validateRefreshToken() → redis.hasKey()
        when(redis.hasKey(startsWith("refresh:"))).thenReturn(true);

        assertThat(provider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(provider.getUsername(refreshToken)).isEqualTo("testuser");
    }

    // ── 잘못된·만료된 토큰 ────────────────────────────────────────────────
    @Test @DisplayName("validateToken → 잘못된 토큰이면 false")
    void validateToken_invalidToken() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }

    @Test @DisplayName("validateRefreshToken → 잘못된 리프레시 토큰이면 false")
    void validateRefreshToken_invalidToken() {
        lenient().when(redis.hasKey(anyString())).thenReturn(false);   // 존재하지 않음
        assertThat(provider.validateRefreshToken("bad-token")).isFalse();
    }

    @Test @DisplayName("validateToken → 만료된 토큰이면 false")
    void validateToken_expiredToken() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                SECRET, 1L, REFRESH_TTL_MIN, redis);          // 1 ms 유효

        String token = shortLived.generateToken(auth);
        Thread.sleep(5);                                     // 만료 대기

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test @DisplayName("getUsername → 잘못된 형식이면 JwtException")
    void getUsername_invalidToken() {
        assertThatThrownBy(() -> provider.getUsername("invalid.jwt.token"))
                .isInstanceOf(JwtException.class);
    }
}