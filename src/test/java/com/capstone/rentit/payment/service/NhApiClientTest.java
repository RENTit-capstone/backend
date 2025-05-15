package com.capstone.rentit.payment.service;

import com.capstone.rentit.payment.config.NhApiProperties;
import com.capstone.rentit.payment.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NhApiClient 단위 테스트
 */
class NhApiClientTest {

    @Mock
    RestTemplate restTemplate;

    NhApiClient sut;          // System Under Test

    // 고정 테스트 설정값
    NhApiProperties props = new NhApiProperties(
            "https://developers.nonghyup.com",
            "900001",                   // iscd
            "001",                      // fintech aps no
            "dummy-token",
            new NhApiProperties.SvcCodes(
                    "DrawingTransferA",
                    "ReceivedTransferAccountNumberA")
    );

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sut = new NhApiClient(restTemplate, props);
    }

    /* ---------------- 지갑 충전 ---------------- */
    @Nested
    @DisplayName("drawingTransfer")
    class DrawingTransferTest {

        final String pinAccount = "12345678901234567890123456";
        final long amount = 50_000L;
        final String memo = "테스트 충전";

        @Test
        @DisplayName("정상 호출 시 RestTemplate 로 JSON 전송하고 응답을 그대로 반환한다")
        void drawingTransfer_success() {

            // given
            DrawingTransferResponse dummyRes =
                    new DrawingTransferResponse(null, pinAccount, "20250101");

            when(restTemplate.postForObject(
                    anyString(),
                    any(HttpEntity.class),
                    eq(DrawingTransferResponse.class)
            )).thenReturn(dummyRes);

            // when
            DrawingTransferResponse result =
                    sut.drawingTransfer(pinAccount, amount, memo);

            // then
            assertThat(result).isSameAs(dummyRes);

            // --- RestTemplate 호출 파라미터 검증 ---
            ArgumentCaptor<HttpEntity> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForObject(
                    eq(props.baseUrl() + "/nhapis/v1/DrawingTransfer.nh"),
                    entityCap.capture(),
                    eq(DrawingTransferResponse.class)
            );

            HttpEntity<?> sentEntity = entityCap.getValue();
            assertThat(sentEntity.getHeaders().getContentType())
                    .isEqualTo(MediaType.APPLICATION_JSON);

            DrawingTransferRequest body = (DrawingTransferRequest) sentEntity.getBody();
            assertThat(body.FinAcno()).isEqualTo(pinAccount);
            assertThat(body.Tram()).isEqualTo(String.valueOf(amount));
            assertThat(body.DractOtlt()).isEqualTo(memo);
        }
    }

    /* ---------------- 지갑 환급 ---------------- */
    @Nested
    @DisplayName("deposit")
    class DepositTest {

        final String pinAccount = "99988877766655544433322211";
        final long amount = 30_000L;
        final String memo = "테스트 출금";

        @Test
        @DisplayName("정상 호출 시 RestTemplate 로 JSON 전송하고 응답을 그대로 반환한다")
        void deposit_success() {

            // given
            DepositResponse dummyRes =
                    new DepositResponse(null, pinAccount, "20250102");

            when(restTemplate.postForObject(
                    anyString(),
                    any(HttpEntity.class),
                    eq(DepositResponse.class)
            )).thenReturn(dummyRes);

            // when
            DepositResponse result = sut.deposit(pinAccount, amount, memo);

            // then
            assertThat(result).isSameAs(dummyRes);

            // --- RestTemplate 호출 파라미터 검증 ---
            ArgumentCaptor<HttpEntity> entityCap = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForObject(
                    eq(props.baseUrl() + "/nhapis/v1/ReceivedTransferAccountNumber.nh"),
                    entityCap.capture(),
                    eq(DepositResponse.class)
            );

            HttpEntity<?> sentEntity = entityCap.getValue();
            assertThat(sentEntity.getHeaders().getContentType())
                    .isEqualTo(MediaType.APPLICATION_JSON);

            DepositRequest body = (DepositRequest) sentEntity.getBody();
            assertThat(body.FinAcno()).isEqualTo(pinAccount);
            assertThat(body.Tram()).isEqualTo(String.valueOf(amount));
            assertThat(body.MractOtlt()).isEqualTo(memo);
        }
    }
}