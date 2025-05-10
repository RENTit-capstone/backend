package com.capstone.rentit.locker.message;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.config.LockerMessagingConfig;
import com.capstone.rentit.locker.dto.*;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.locker.service.LockerService;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.dto.StudentDto;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.otp.service.OtpService;
import com.capstone.rentit.rental.dto.RentalBriefResponseForLocker;
import com.capstone.rentit.rental.service.RentalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockerDeviceRequestListenerTest {

    @Mock OtpService otpService;
    @Mock MemberService memberService;
    @Mock RentalService rentalService;
    @Mock LockerService lockerService;
    @Mock LockerDeviceProducer producer;

    private LockerDeviceRequestListener listener;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        listener = new LockerDeviceRequestListener(
                mapper, otpService, memberService, rentalService, lockerService, producer);
    }

    private Message<byte[]> mqttMsg(String subTopic, LockerDeviceRequest req) throws Exception {
        String fullTopic = LockerMessagingConfig.REQ_TOPIC_PREFIX + subTopic;
        byte[] json = mapper.writeValueAsBytes(req);
        return MessageBuilder.withPayload(json)
                .setHeader(MqttHeaders.RECEIVED_TOPIC, fullTopic)
                .build();
    }

    private Message<String> mqttStringMsg(String subTopic, LockerDeviceRequest req) throws Exception {
        String fullTopic = LockerMessagingConfig.REQ_TOPIC_PREFIX + subTopic;
        String json = mapper.writeValueAsString(req);
        return MessageBuilder.withPayload(json)
                .setHeader(MqttHeaders.RECEIVED_TOPIC, fullTopic)
                .build();
    }

    private static LockerDeviceRequest dummyReq(RentalLockerAction action) {
        return new LockerDeviceRequest(
                10L,                   // deviceId
                "00000",               // otpCode (available path에선 사용되지 않음)
                action,                // action
                20L                    // rentalId
        );
    }

    @Nested
    @DisplayName("sub-topic = eligible")
    class Eligible {
        @Test
        @DisplayName("OTP 검증 후 조건에 맞는 렌털 목록을 CommonResponse로 Push")
        void handleEligible() throws Exception {
            // given
            var req = dummyReq(RentalLockerAction.DROP_OFF_BY_OWNER);
            Message<byte[]> msg = mqttMsg("eligible", req);

            when(otpService.validateAndResolveIdentifier(req.otpCode()))
                    .thenReturn("user@test");
            MemberDto member = StudentDto.builder()
                    .memberId(1L)
                    .name("Test User")
                    .build();
            when(memberService.getMemberByEmail("user@test")).thenReturn(member);

            List<RentalBriefResponseForLocker> rentals =
                    List.of(mock(RentalBriefResponseForLocker.class));
            when(rentalService.findEligibleRentals(member.getMemberId(), req.action()))
                    .thenReturn(rentals);

            // when
            listener.handle(msg);

            // then
            ArgumentCaptor<CommonResponse<?>> captor =
                    ArgumentCaptor.forClass(CommonResponse.class);
            verify(producer).pushEligibleRentals(eq(req.deviceId()), captor.capture());

            CommonResponse<?> resp = captor.getValue();
            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getData()).isInstanceOf(EligibleRentalsEvent.class);

            var evt = (EligibleRentalsEvent) resp.getData();
            assertThat(evt.deviceId()).isEqualTo(req.deviceId());
            assertThat(evt.action()).isEqualTo(req.action());
            assertThat(evt.memberId()).isEqualTo(member.getMemberId());
            assertThat(evt.rentals()).isEqualTo(rentals);
        }
    }

    @Nested
    @DisplayName("sub-topic = available")
    class Available {
        @Test
        @DisplayName("사용 가능 사물함 목록을 CommonResponse로 Push")
        void handleAvailable() throws Exception {
            // given
            var req = dummyReq(RentalLockerAction.RETURN_BY_RENTER);
            Message<String> msg = mqttStringMsg("available", req);

            List<LockerBriefResponse> lockers =
                    List.of(mock(LockerBriefResponse.class));
            when(lockerService.findAvailableLockers(req.deviceId()))
                    .thenReturn(lockers);

            // when
            listener.handle(msg);

            // then
            ArgumentCaptor<CommonResponse<?>> captor =
                    ArgumentCaptor.forClass(CommonResponse.class);
            verify(producer).pushAvailableLockers(eq(req.deviceId()), captor.capture());

            CommonResponse<?> resp = captor.getValue();
            assertThat(resp.isSuccess()).isTrue();
            assertThat(resp.getData()).isInstanceOf(AvailableLockersEvent.class);

            var evt = (AvailableLockersEvent) resp.getData();
            assertThat(evt.deviceId()).isEqualTo(req.deviceId());
            assertThat(evt.rentalId()).isEqualTo(req.rentalId());
            assertThat(evt.lockers()).isEqualTo(lockers);
        }
    }

    @Test
    @DisplayName("알 수 없는 sub-topic인 경우 producer 호출 없음")
    void handleUnknownSubTopic() throws Exception {
        var req = dummyReq(RentalLockerAction.PICK_UP_BY_RENTER);
        Message<byte[]> msg = mqttMsg("unknown", req);

        listener.handle(msg);

        verifyNoInteractions(producer);
    }
}
