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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    // ── 상수 ──────────────────────────────────────────────────
    private static final String SECRET =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 64B
    private static final long EXPIRATION_MS   = 60_000; // 60초
    private static final long REFRESH_TTL_MIN =     10; // 10분

    // ── Mocks ────────────────────────────────────────────────
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private JwtTokenProvider provider;
    private Authentication   auth;     // 공통 Authentication 객체

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);

        // ▸ 생성자 주입은 redis 하나만
        provider = new JwtTokenProvider(redis);

        // ▸ @Value 필드 값 주입을 대신 Reflection 으로 세팅
        ReflectionTestUtils.setField(provider, "secret",            SECRET);
        ReflectionTestUtils.setField(provider, "jwtExpirationInMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(provider, "refreshTtlMin",     REFRESH_TTL_MIN);

        // ▸ @PostConstruct 수동 실행
        provider.init();

        // ▸ Authentication 준비
        auth = new UsernamePasswordAuthenticationToken(
                "testuser", null, Collections.emptyList());
    }

    // ── 액세스 토큰 ────────────────────────────────────────────
    @Test @DisplayName("generateToken → 유효한 액세스 토큰 생성·검증")
    void generateToken_shouldReturnValidToken() {
        String token = provider.generateToken(auth);

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo("testuser");
    }

    // ── 리프레시 토큰 ─────────────────────────────────────────
    @Test @DisplayName("generateRefreshToken → 유효한 리프레시 토큰 생성·검증")
    void generateRefreshToken_shouldReturnValidRefreshToken() {
        String refreshToken = provider.generateRefreshToken(auth);

        verify(valueOps).set(
                startsWith("refresh:"), eq("testuser"), eq(Duration.ofMinutes(REFRESH_TTL_MIN))
        );

        when(redis.hasKey(startsWith("refresh:"))).thenReturn(true);

        assertThat(provider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(provider.getUsername(refreshToken)).isEqualTo("testuser");
    }

    // ── 잘못된·만료된 토큰 ────────────────────────────────────
    @Test @DisplayName("validateToken → 잘못된 토큰이면 false")
    void validateToken_invalidToken() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
    }

    @Test @DisplayName("validateRefreshToken → 잘못된 리프레시 토큰이면 false")
    void validateRefreshToken_invalidToken() {
        lenient().when(redis.hasKey(anyString())).thenReturn(false);
        assertThat(provider.validateRefreshToken("bad-token")).isFalse();
    }

    @Test @DisplayName("validateToken → 만료된 토큰이면 false")
    void validateToken_expiredToken() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(redis);
        ReflectionTestUtils.setField(shortLived, "secret",            SECRET);
        ReflectionTestUtils.setField(shortLived, "jwtExpirationInMs", 1L); // 1 ms
        ReflectionTestUtils.setField(shortLived, "refreshTtlMin",     REFRESH_TTL_MIN);
        shortLived.init();

        String token = shortLived.generateToken(auth);
        Thread.sleep(5);  // 만료 대기

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test @DisplayName("getUsername → 잘못된 형식이면 JwtException")
    void getUsername_invalidToken() {
        assertThatThrownBy(() -> provider.getUsername("invalid.jwt.token"))
                .isInstanceOf(JwtException.class);
    }
}