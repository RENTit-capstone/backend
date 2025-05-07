package com.capstone.rentit.locker.message;

import com.capstone.rentit.config.LockerMessagingConfig;
import com.capstone.rentit.locker.dto.AvailableLockersEvent;
import com.capstone.rentit.locker.dto.EligibleRentalsEvent;
import com.capstone.rentit.locker.dto.LockerActionResultEvent;
import com.capstone.rentit.locker.dto.LockerDto;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.rental.dto.RentalBriefResponseForLocker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockerDeviceProducerTest {

    @Mock  RabbitTemplate rabbit;
    @InjectMocks LockerDeviceProducer producer;

    /* ────────────────────────── 헬퍼 ────────────────────────── */

    private <T> T capturePayload(String expectedKey, Class<T> type) {
        // exchange 는 고정 상수이므로 바로 검증
        ArgumentCaptor<String>   keyCaptor  = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object>   msgCaptor  = ArgumentCaptor.forClass(Object.class);

        verify(rabbit, times(1))
                .convertAndSend(eq(LockerMessagingConfig.DEVICE_EX),
                        keyCaptor.capture(),
                        msgCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo(expectedKey);
        assertThat(msgCaptor.getValue()).isInstanceOf(type);
        //noinspection unchecked
        return (T) msgCaptor.getValue();
    }

    /* ────────────────────────── 테스트 ───────────────────────── */

    @Nested
    @DisplayName("pushEligibleRentals()")
    class Eligible {

        @Test @DisplayName("정상 메시지 발행")
        void pushEligibleRentals_success() {
            // given
            long lockerId = 11L;
            var rentals = List.of(mock(RentalBriefResponseForLocker.class));

            // when
            producer.pushEligibleRentals(lockerId,
                    RentalLockerAction.DROP_OFF_BY_OWNER,
                    rentals);

            // then
            String rk = "locker." + lockerId + ".eligible";
            var event = capturePayload(rk, EligibleRentalsEvent.class);

            assertThat(event.lockerId()).isEqualTo(lockerId);
            assertThat(event.action()).isEqualTo(RentalLockerAction.DROP_OFF_BY_OWNER);
            assertThat(event.rentals()).isEqualTo(rentals);
        }
    }

    @Nested
    @DisplayName("pushAvailableLockers()")
    class Available {

        @Test
        @DisplayName("정상 호출 시 라우팅키·페이로드가 올바르게 전송된다")
        void pushAvailableLockers_success() {
            // given
            long lockerId = 11L;
            long rentalId = 22L;
            List<LockerDto> lockers = List.of(mock(LockerDto.class));

            // when
            producer.pushAvailableLockers(lockerId, rentalId, lockers);

            // then
            ArgumentCaptor<String> routingKeyCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<AvailableLockersEvent> payloadCap =
                    ArgumentCaptor.forClass(AvailableLockersEvent.class);

            verify(rabbit).convertAndSend(
                    eq(LockerMessagingConfig.DEVICE_EX),
                    routingKeyCap.capture(),
                    payloadCap.capture()
            );
            verifyNoMoreInteractions(rabbit);

            /* 라우팅키 검증 */
            assertThat(routingKeyCap.getValue())
                    .isEqualTo("locker." + lockerId + ".available");

            /* 페이로드 검증 */
            AvailableLockersEvent event = payloadCap.getValue();
            assertThat(event.lockerId()).isEqualTo(lockerId);
            assertThat(event.rentalId()).isEqualTo(rentalId);
            assertThat(event.lockers()).isEqualTo(lockers);
        }
    }

    @Nested
    @DisplayName("pushResult()")
    class Result {

        @Test @DisplayName("성공 결과 메시지")
        void pushResult_success() {
            // given
            long lockerId = 33L;
            long rentalId = 99L;

            // when
            producer.pushResult(lockerId, rentalId, true, "ignored");

            // then
            String rk = "locker." + lockerId + ".result";
            var event = capturePayload(rk, LockerActionResultEvent.class);

            assertThat(event.success()).isTrue();
            assertThat(event.message()).isEmpty();
        }

        @Test @DisplayName("실패 결과 메시지")
        void pushResult_failure() {
            // given
            long lockerId = 33L;
            long rentalId = 99L;
            String error  = "LOCKER_BUSY";

            // when
            producer.pushResult(lockerId, rentalId, false, error);

            // then
            String rk = "locker." + lockerId + ".result";
            var event = capturePayload(rk, LockerActionResultEvent.class);

            assertThat(event.success()).isFalse();
            assertThat(event.message()).isEqualTo(error);
        }
    }
}