package com.capstone.rentit.locker.message;

import com.capstone.rentit.config.LockerMessagingConfig;
import com.capstone.rentit.locker.dto.LockerDeviceRequest;
import com.capstone.rentit.locker.dto.LockerDto;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.locker.service.LockerService;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.StudentDto;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.otp.service.OtpService;
import com.capstone.rentit.rental.dto.RentalBriefResponseForLocker;
import com.capstone.rentit.rental.service.RentalService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockerDeviceRequestListenerTest {

    @Mock OtpService otpService;
    @Mock MemberService memberService;
    @Mock RentalService rentalService;
    @Mock LockerService lockerService;
    @Mock LockerDeviceProducer producer;

    LockerDeviceRequestListener listener;

    @BeforeEach
    void setUp() {
        listener = new LockerDeviceRequestListener(
                otpService, memberService, rentalService, lockerService, producer);
    }

    /* ------------ 공통 헬퍼 ------------- */
    private static Message messageWithKey(String rk) {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey(rk);
        return new Message("{}".getBytes(StandardCharsets.UTF_8), props);
    }

    private static LockerDeviceRequest dummyReq(RentalLockerAction action) {
        return new LockerDeviceRequest(
                10L, // lockerId
                "user@test", // email
                "00000", // otpCode
                action, // action
                20L, // rentalId
                11L, // targetLockerId
                "MY-UNI" // university
        );
    }

    /* ------------ eligible 분기 ------------- */
    @Nested
    @DisplayName("locker.request.eligible")
    class Eligible {

        @Test
        @DisplayName("OTP·멤버 검증 후 eligible 목록을 push 한다")
        void handleEligible() {
            // given
            LockerDeviceRequest req = dummyReq(RentalLockerAction.DROP_OFF_BY_OWNER);
            Message msg = messageWithKey("locker.request.eligible");

            when(otpService.validateAndResolveIdentifier(req.otpCode(), req.email()))
                    .thenReturn(req.email());
            MemberDto member = StudentDto.builder()
                    .memberId(1L).email(req.email()).name("name")
                    .build();
            when(memberService.getMemberByEmail(req.email())).thenReturn(member);

            List<RentalBriefResponseForLocker> rentals = List.of(
                    mock(RentalBriefResponseForLocker.class));
            when(rentalService.findEligibleRentals(member.getMemberId(), req.action()))
                    .thenReturn(rentals);

            // when
            listener.handle(req, msg);

            // then
            verify(producer).pushEligibleRentals(
                    req.lockerId(), req.action(), rentals);
            verifyNoMoreInteractions(producer);
        }
    }

    /* ------------ available 분기 ------------- */
    @Nested
    @DisplayName("locker.request.available")
    class Available {

        @Test
        @DisplayName("대학별 빈 사물함 목록을 push 한다")
        void handleAvailable() {
            // given
            LockerDeviceRequest req = dummyReq(RentalLockerAction.PICK_UP_BY_RENTER);
            Message msg = messageWithKey("locker.request.available");

            when(otpService.validateAndResolveIdentifier(req.otpCode(), req.email()))
                    .thenReturn(req.email());
            when(memberService.getMemberByEmail(req.email()))
                    .thenReturn(mock(MemberDto.class));

            List<LockerDto> lockers = List.of(mock(LockerDto.class));
            when(lockerService.findAvailableLockers(req.university()))
                    .thenReturn(lockers);

            // when
            listener.handle(req, msg);

            // then
            verify(producer).pushAvailableLockers(
                    req.lockerId(), req.rentalId(), lockers);
            verifyNoMoreInteractions(producer);
        }
    }

    /* ------------ 기타(알 수 없는 라우팅키) ------------- */
    @Test
    @DisplayName("정의되지 않은 라우팅키면 아무 이벤트도 전송하지 않는다")
    void handleUnknownRoutingKey() {
        // given
        LockerDeviceRequest req = dummyReq(RentalLockerAction.RETURN_BY_RENTER);
        Message msg = messageWithKey("locker.request.unknown");

        when(otpService.validateAndResolveIdentifier(req.otpCode(), req.email()))
                .thenReturn(req.email());
        when(memberService.getMemberByEmail(req.email()))
                .thenReturn(mock(MemberDto.class));

        // when
        listener.handle(req, msg);

        // then
        verifyNoInteractions(producer);
    }
}