package com.capstone.rentit.otp.service;

import com.capstone.rentit.otp.exception.OtpNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final Duration OTP_TTL = Duration.ofMinutes(1);

    @Mock
    StringRedisTemplate redis;

    @Mock
    ValueOperations<String, String> valueOps;

    OtpService otpService;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        otpService = new OtpService(redis);
    }

    @Test
    @DisplayName("generateOtp: 길이 5의 숫자 OTP를 생성하고 Redis에 TTL과 함께 저장한다")
    void generateOtp_shouldProduce5DigitCodeAndStoreIt() {
        // given
        String identifier = "user@example.com";
        when(redis.hasKey(anyString())).thenReturn(false); // 충돌 X

        // when
        String otp = otpService.generateOtp(identifier);

        // then
        assertThat(otp).matches("\\d{5}");

        String expectedKey = "otp:" + otp;
        verify(redis).hasKey(expectedKey);
        verify(valueOps).set(eq(expectedKey), eq(identifier), eq(OTP_TTL));
    }

    @Nested
    @DisplayName("validateAndResolveIdentifier 예외 케이스")
    class ValidateOtpExceptionTests {

        @Test
        @DisplayName("존재하지 않거나 만료된 OTP → OtpNotFoundException")
        void validateOtp_notFound() {
            // given
            when(valueOps.get(anyString())).thenReturn(null);

            // expect
            assertThatThrownBy(() -> otpService.validateAndResolveIdentifier("12345"))
                    .isInstanceOf(OtpNotFoundException.class);
        }
    }

    @Test
    @DisplayName("validateAndResolveIdentifier: 올바른 코드이면 식별자를 반환하고 키를 삭제한다")
    void validateOtp_successful() {
        // given
        String identifier = "valid-user";
        String code = "67890";
        String redisKey = "otp:" + code;

        when(valueOps.get(redisKey)).thenReturn(identifier);

        // when
        String resolved = otpService.validateAndResolveIdentifier(code);

        // then
        assertThat(resolved).isEqualTo(identifier);
        verify(redis).delete(redisKey);
    }
}