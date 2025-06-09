package com.capstone.rentit.register.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.capstone.rentit.register.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class UnivCertServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UnivCertService univCertService;

    private static final String BASE = "https://univcert.com/api/v1";

    @BeforeEach
    void setUp() {
        // @Value로 주입되는 apiKey 설정
        ReflectionTestUtils.setField(univCertService, "apiKey", "test-key");
    }

    @Nested
    @DisplayName("validateUniversity")
    class ValidateUniversity {

        @Test
        @DisplayName("성공: 예외 없이 반환")
        void success() {
            Map<String, Object> body = Map.of("success", true);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/check"), any(), eq(Map.class)))
                    .thenReturn(resp);

            assertDoesNotThrow(() ->
                    univCertService.validateUniversity("AnyUniversity")
            );
        }

        @Test
        @DisplayName("실패: InvalidUniversityException 발생")
        void failure() {
            Map<String, Object> body = Map.of("success", false);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/check"), any(), eq(Map.class)))
                    .thenReturn(resp);

            assertThrows(InvalidUniversityException.class, () ->
                    univCertService.validateUniversity("BadUniversity")
            );
        }

        @Test
        @DisplayName("응답 바디 null: UnivServiceException 발생")
        void nullBody() {
            ResponseEntity<Map> resp = new ResponseEntity<>(null, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/check"), any(), eq(Map.class)))
                    .thenReturn(resp);

            UnivServiceException ex = assertThrows(UnivServiceException.class, () ->
                    univCertService.validateUniversity("Any")
            );
            assertEquals("외부 인증 서비스 응답이 비어 있습니다.", ex.getMessage());
        }

        @Test
        @DisplayName("RestTemplate 예외: UnivServiceException 발생")
        void restClientException() {
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                    .thenThrow(new RuntimeException("timeout"));

            UnivServiceException ex = assertThrows(UnivServiceException.class, () ->
                    univCertService.validateUniversity("Any")
            );
            assertTrue(ex.getMessage().contains("외부 인증 서비스 호출 중 오류가 발생했습니다."));
        }
    }

    @Nested
    @DisplayName("sendCertification")
    class SendCertification {

        @Test
        @DisplayName("성공: 예외 없이 반환")
        void success() {
            Map<String, Object> body = Map.of("success", true);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/certify"), any(), eq(Map.class)))
                    .thenReturn(resp);

            assertDoesNotThrow(() ->
                    univCertService.sendCertification("a@b.com", "Uni", false)
            );
        }

        @Test
        @DisplayName("실패: CertificationSendFailureException 발생")
        void failure() {
            Map<String, Object> body = Map.of("success", false);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/certify"), any(), eq(Map.class)))
                    .thenReturn(resp);

            assertThrows(CertificationSendFailureException.class, () ->
                    univCertService.sendCertification("a@b.com", "Uni", false)
            );
        }
    }

    @Nested
    @DisplayName("verifyCode")
    class VerifyCode {

        @Test
        @DisplayName("성공: 예외 없이 반환")
        void success() {
            Map<String, Object> body = Map.of("success", true);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/certifycode"), any(), eq(Map.class)))
                    .thenReturn(resp);

            assertDoesNotThrow(() ->
                    univCertService.verifyCode("a@b.com", "Uni", 12345)
            );
        }

        @Test
        @DisplayName("실패: InvalidVerificationCodeException 발생")
        void failure() {
            Map<String, Object> body = Map.of("success", false);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/certifycode"), any(), eq(Map.class)))
                    .thenReturn(resp);

            assertThrows(InvalidVerificationCodeException.class, () ->
                    univCertService.verifyCode("a@b.com", "Uni", 12345)
            );
        }
    }

    @Nested
    @DisplayName("ensureCertified")
    class EnsureCertified {

        @Test
        @DisplayName("성공: 예외 없이 반환")
        void success() {
            Map<String, Object> body = Map.of("success", true);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/status"), any(), eq(Map.class)))
                    .thenReturn(resp);

            assertDoesNotThrow(() ->
                    univCertService.ensureCertified("a@b.com")
            );
        }

        @Test
        @DisplayName("실패: UnivNotCertifiedException 발생")
        void failure() {
            Map<String, Object> body = Map.of("success", false);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/status"), any(), eq(Map.class)))
                    .thenReturn(resp);

            assertThrows(UnivNotCertifiedException.class, () ->
                    univCertService.ensureCertified("a@b.com")
            );
        }
    }

    @Nested
    @DisplayName("clearAll")
    class ClearAll {

        @Test
        @DisplayName("성공: 예외 없이 반환")
        void success() {
            Map<String, Object> body = Map.of("success", true);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/clear"), any(), eq(Map.class)))
                    .thenReturn(resp);

            assertDoesNotThrow(() ->
                    univCertService.clearAll()
            );
        }

        @Test
        @DisplayName("실패: UnivServiceException 발생")
        void failure() {
            Map<String, Object> body = Map.of("success", false);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/clear"), any(), eq(Map.class)))
                    .thenReturn(resp);

            UnivServiceException ex = assertThrows(UnivServiceException.class, () ->
                    univCertService.clearAll()
            );
            assertEquals("인증 데이터 초기화에 실패했습니다.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("showAll")
    class ShowAll {

        @Test
        @DisplayName("성공: data 반환")
        void success() {
            List<String> dummy = List.of("a", "b");
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("data", dummy);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/certifiedlist"), any(), eq(Map.class)))
                    .thenReturn(resp);

            Object result = univCertService.showAll();
            assertSame(dummy, result);
        }

        @Test
        @DisplayName("실패(success=false): UnivServiceException 발생")
        void failureSuccessFalse() {
            Map<String, Object> body = Map.of("success", false);
            ResponseEntity<Map> resp = new ResponseEntity<>(body, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/certifiedlist"), any(), eq(Map.class)))
                    .thenReturn(resp);

            UnivServiceException ex = assertThrows(UnivServiceException.class, () ->
                    univCertService.showAll()
            );
            assertEquals("인증 정보 조회에 실패했습니다.", ex.getMessage());
        }

        @Test
        @DisplayName("null body: UnivServiceException 발생")
        void nullBody() {
            ResponseEntity<Map> resp = new ResponseEntity<>(null, HttpStatus.OK);

            when(restTemplate.postForEntity(eq(BASE + "/certifiedlist"), any(), eq(Map.class)))
                    .thenReturn(resp);

            UnivServiceException ex = assertThrows(UnivServiceException.class, () ->
                    univCertService.showAll()
            );
            assertEquals("외부 인증 서비스 응답이 비어 있습니다.", ex.getMessage());
        }

        @Test
        @DisplayName("RestTemplate 예외: UnivServiceException 발생")
        void restClientException() {
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                    .thenThrow(new RuntimeException("network error"));

            UnivServiceException ex = assertThrows(UnivServiceException.class, () ->
                    univCertService.showAll()
            );
            assertTrue(ex.getMessage().contains("외부 인증 서비스 호출 중 오류가 발생했습니다."));
        }
    }
}