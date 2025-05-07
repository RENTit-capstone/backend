package com.capstone.rentit.locker.message;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.locker.dto.RentalLockerEventMessage;
import com.capstone.rentit.locker.event.RentalLockerEventType;
import com.capstone.rentit.rental.service.RentalService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalLockerEventListenerTest {

    @Mock RentalService rentalSvc;
    @Mock LockerDeviceProducer producer;
    @Mock FileStorageService fileStorage;   // 현재 로직에선 사용되지 않지만 의존성 주입 필요

    RentalLockerEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new RentalLockerEventListener(rentalSvc, producer, fileStorage);
    }

    /* ---------- 공통 메시지 헬퍼 ---------- */
    private static RentalLockerEventMessage dummy(RentalLockerEventType type) {
        return new RentalLockerEventMessage(
                11L,   // lockerId
                22L,   // rentalId
                33L,   // memberId
                type
        );
    }

    @Nested
    @DisplayName("정상 처리 케이스")
    class SuccessPath {

        @ParameterizedTest(name = "{0} 성공")
        @EnumSource(RentalLockerEventType.class)
        void handle_success(RentalLockerEventType action) {
            // given
            RentalLockerEventMessage msg = dummy(action);

            // 각 action별 rentService 호출 stubbing (void 메서드 → 아무 일도 안 함)
            // when
            listener.consume(msg);

            // then : rentService 의 해당 메서드 1번 호출 & 결과 push
            switch (action) {
                case DROP_OFF_BY_OWNER ->
                        verify(rentalSvc).dropOffToLocker(msg.rentalId(), msg.memberId(), msg.lockerId());
                case PICK_UP_BY_RENTER ->
                        verify(rentalSvc).pickUpByRenter(msg.rentalId(), msg.memberId());
                case RETURN_TO_LOCKER ->
                        verify(rentalSvc).returnToLocker(msg.rentalId(), msg.memberId(), msg.lockerId(), null);
                case RETRIEVE_BY_OWNER ->
                        verify(rentalSvc).retrieveByOwner(msg.rentalId(), msg.memberId());
            }
            verify(producer).pushResult(msg.lockerId(), msg.rentalId(), true, null);
            verifyNoMoreInteractions(producer);
        }
    }

    @Nested
    @DisplayName("예외 발생 케이스")
    class FailurePath {

        @Test
        @DisplayName("서비스 예외 발생 시 pushResult 에 error 메시지 포함")
        void handle_exception() {
            // given
            RentalLockerEventMessage msg = dummy(RentalLockerEventType.DROP_OFF_BY_OWNER);
            RuntimeException boom = new RuntimeException("LOCKER FULL");
            doThrow(boom).when(rentalSvc)
                    .dropOffToLocker(msg.rentalId(), msg.memberId(), msg.lockerId());

            // when
            listener.consume(msg);

            // then
            verify(rentalSvc).dropOffToLocker(msg.rentalId(), msg.memberId(), msg.lockerId());
            verify(producer).pushResult(msg.lockerId(), msg.rentalId(), false, "LOCKER FULL");
        }
    }
}