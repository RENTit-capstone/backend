package com.capstone.rentit.register.service;

import com.capstone.rentit.register.exception.InvalidVerificationCodeException;
import com.capstone.rentit.register.exception.UnivNotCertifiedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @InjectMocks
    private VerificationService verificationService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private EmailService emailService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        // Mockito가 redisTemplate.opsForValue() 호출 시 우리가 만든 가짜(mock) valueOperations를 반환하도록 설정
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("인증 코드 생성 및 발송 테스트")
    class GenerateAndSendCode {
        @Test
        @DisplayName("성공: 6자리 코드를 생성하여 Redis에 5분 TTL로 저장하고 이메일을 발송해야 한다")
        void generateAndSendVerificationCode_Success() {
            // Given
            String email = "test@example.com";
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

            // When
            verificationService.generateAndSendVerificationCode(email);

            // Then
            // 1. Redis에 set이 1번 호출되었는지, 키와 TTL이 올바른지 검증
            verify(valueOperations, times(1)).set(
                    eq("verification-code:" + email), // 키가 정확한지
                    codeCaptor.capture(),             // 저장되는 코드를 캡처
                    eq(Duration.ofMinutes(5))      // TTL이 5분인지
            );

            // 2. 캡처된 코드가 6자리 숫자인지 검증
            String generatedCode = codeCaptor.getValue();
            assertThat(generatedCode).hasSize(6).matches("\\d+");

            // 3. EmailService의 send 메소드가 1번 호출되었는지, 이메일과 코드가 올바른지 검증
            verify(emailService, times(1)).sendVerificationEmail(email, generatedCode);
        }
    }


    @Nested
    @DisplayName("인증 코드 검증 테스트")
    class VerifyCode {
        @Test
        @DisplayName("성공: 코드가 일치하면 Redis에서 코드를 삭제하고 '인증 완료' 상태를 저장해야 한다")
        void verifyCode_Success() {
            // Given
            String email = "test@example.com";
            int userCode = 123456;
            String storedCode = "123456";
            String codeKey = "verification-code:" + email;
            String verifiedKey = "verified-email:" + email;

            when(valueOperations.get(codeKey)).thenReturn(storedCode);

            // When
            verificationService.verifyCode(email, userCode);

            // Then
            // 1. Redis에서 인증 코드가 삭제되었는지 검증
            verify(redisTemplate, times(1)).delete(codeKey);
            // 2. Redis에 '인증 완료' 상태가 10분 TTL로 저장되었는지 검증
            verify(valueOperations, times(1)).set(verifiedKey, "true", Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("실패: 코드가 존재하지 않으면 InvalidVerificationCodeException 예외가 발생해야 한다")
        void verifyCode_Failure_CodeNotFound() {
            // Given
            String email = "test@example.com";
            int userCode = 123456;
            String codeKey = "verification-code:" + email;

            when(valueOperations.get(codeKey)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> verificationService.verifyCode(email, userCode))
                    .isInstanceOf(InvalidVerificationCodeException.class);

            // '인증 완료' 상태 저장이 호출되지 않았는지 검증
            verify(valueOperations, never()).set(anyString(), eq("true"), any(Duration.class));
        }

        @Test
        @DisplayName("실패: 코드가 일치하지 않으면 InvalidVerificationCodeException 예외가 발생해야 한다")
        void verifyCode_Failure_CodeMismatch() {
            // Given
            String email = "test@example.com";
            int userCode = 111111;
            String storedCode = "999999";
            String codeKey = "verification-code:" + email;

            when(valueOperations.get(codeKey)).thenReturn(storedCode);

            // When & Then
            assertThatThrownBy(() -> verificationService.verifyCode(email, userCode))
                    .isInstanceOf(InvalidVerificationCodeException.class);

            // Redis에서 키가 삭제되지 않았는지 검증
            verify(redisTemplate, never()).delete(anyString());
        }
    }


    @Nested
    @DisplayName("이메일 인증 상태 확인 테스트")
    class EnsureEmailVerified {
        @Test
        @DisplayName("성공: Redis에 '인증 완료' 상태가 있으면 예외 없이 통과해야 한다")
        void ensureEmailVerified_Success() {
            // Given
            String email = "test@example.com";
            when(valueOperations.get("verified-email:" + email)).thenReturn("true");

            // When & Then
            assertDoesNotThrow(() -> verificationService.ensureEmailVerified(email));
        }

        @Test
        @DisplayName("실패: Redis에 '인증 완료' 상태가 없으면 UnivNotCertifiedException 예외가 발생해야 한다")
        void ensureEmailVerified_Failure() {
            // Given
            String email = "test@example.com";
            when(valueOperations.get("verified-email:" + email)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> verificationService.ensureEmailVerified(email))
                    .isInstanceOf(UnivNotCertifiedException.class);
        }
    }


    @Nested
    @DisplayName("관리자 기능 테스트")
    class AdminFeatures {
        @Test
        @DisplayName("성공: clearVerification 호출 시 특정 이메일의 '인증 완료' 키만 삭제해야 한다")
        void clearVerification_Success() {
            // Given
            String email = "test@example.com";
            String verifiedKey = "verified-email:" + email;

            // When
            verificationService.clearVerification(email);

            // Then
            verify(redisTemplate, times(1)).delete(verifiedKey);
        }

        @Test
        @DisplayName("성공: clearAllVerifications 호출 시 모든 관련 키들을 삭제해야 한다")
        void clearAllVerifications_Success() {
            // Given
            Set<String> codeKeys = Set.of("verification-code:1", "verification-code:2");
            Set<String> verifiedKeys = Set.of("verified-email:A", "verified-email:B");

            when(redisTemplate.keys("verification-code:*")).thenReturn(codeKeys);
            when(redisTemplate.keys("verified-email:*")).thenReturn(verifiedKeys);

            // When
            verificationService.clearAllVerifications();

            // Then
            verify(redisTemplate, times(1)).delete(codeKeys);
            verify(redisTemplate, times(1)).delete(verifiedKeys);
        }
    }
}