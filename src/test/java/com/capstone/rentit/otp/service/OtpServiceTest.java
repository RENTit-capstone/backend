package com.capstone.rentit.otp.service;

import com.capstone.rentit.otp.dto.OtpDto;
import com.capstone.rentit.otp.exception.OtpExpiredException;
import com.capstone.rentit.otp.exception.OtpMismatchException;
import com.capstone.rentit.otp.exception.OtpNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class OtpServiceTest {
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
    }

    @Test
    @DisplayName("generateOtp: 길이 5의 숫자 OTP를 생성하고 내부 저장소에 보관한다")
    void generateOtp_shouldProduce5DigitCodeAndStoreIt() throws Exception {
        String identifier = "user@example.com";

        String otp = otpService.generateOtp(identifier);

        // 길이 체크
        assertThat(otp).hasSize(5);
        // 숫자만 구성되었는지 체크
        assertThat(otp).matches("\\d{5}");

        // 내부 store에 잘 저장되었는지 리플렉션으로 확인
        @SuppressWarnings("unchecked")
        Map<String, OtpDto> store = getInternalStore();
        OtpDto dto = store.get(identifier);
        assertThat(dto).isNotNull();
        assertThat(dto.getCode()).isEqualTo(otp);
        assertThat(dto.getExpiresAt())
                .isAfter(Instant.now())
                .isBeforeOrEqualTo(Instant.now().plus(Duration.ofMinutes(1)));
    }

    @Nested
    @DisplayName("validateOtp 예외 케이스")
    class ValidateOtpExceptionTests {

        @Test
        @DisplayName("존재하지 않는 identifier로 검증 시 OtpNotFoundException 발생")
        void validateOtp_notFound() {
            assertThatThrownBy(() -> otpService.validateOtp("missing-id", "12345"))
                    .isInstanceOf(OtpNotFoundException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }

        @Test
        @DisplayName("만료된 OTP 코드로 검증 시 OtpExpiredException 발생")
        void validateOtp_expired() throws Exception {
            String identifier = "expired-user";
            String code = "00000";
            // 만료된 DTO 생성
            OtpDto expiredDto = new OtpDto(code, Instant.now().minusSeconds(60));
            @SuppressWarnings("unchecked")
            Map<String, OtpDto> store = getInternalStore();
            store.put(identifier, expiredDto);

            assertThatThrownBy(() -> otpService.validateOtp(identifier, code))
                    .isInstanceOf(OtpExpiredException.class)
                    .hasMessageContaining("만료되었습니다");
            // 한번 사용 후 제거되는지 확인
            assertThat(store).doesNotContainKey(identifier);
        }

        @Test
        @DisplayName("잘못된 코드로 검증 시 OtpMismatchException 발생")
        void validateOtp_mismatch() throws Exception {
            String identifier = "user1";
            String realCode = otpService.generateOtp(identifier);

            assertThatThrownBy(() -> otpService.validateOtp(identifier, realCode + "X"))
                    .isInstanceOf(OtpMismatchException.class)
                    .hasMessageContaining("일치하지 않습니다");
            // 실패 시에도 store에 남아 있어 재시도 가능
            @SuppressWarnings("unchecked")
            Map<String, OtpDto> store = getInternalStore();
            assertThat(store).containsKey(identifier);
        }
    }

    @Test
    @DisplayName("validateOtp: 올바른 코드 검증 시 예외 없이 통과하고 store에서 제거된다")
    void validateOtp_successful() throws Exception {
        String identifier = "valid-user";
        String code = otpService.generateOtp(identifier);

        // 검증 실행
        otpService.validateOtp(identifier, code);

        // 검증 후 store에서 제거되었는지 확인
        @SuppressWarnings("unchecked")
        Map<String, OtpDto> store = getInternalStore();
        assertThat(store).doesNotContainKey(identifier);
    }

    /**
     * private final Map<String, OtpDto> store 필드에 접근하기 위한 유틸리티
     */
    @SuppressWarnings("unchecked")
    private Map<String, OtpDto> getInternalStore() throws Exception {
        Field storeField = OtpService.class.getDeclaredField("store");
        storeField.setAccessible(true);
        return (Map<String, OtpDto>) storeField.get(otpService);
    }
}