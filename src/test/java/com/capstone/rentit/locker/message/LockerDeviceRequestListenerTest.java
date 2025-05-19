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

import static com.capstone.rentit.locker.event.RentalLockerAction.*;
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
                mapper, otpService, memberService, rentalService, lockerService, producer
        );
    }

    /** helper to build a Message<?> with byte[] payload */
    private Message<byte[]> mqttBytes(String subTopic, Object payload) throws Exception {
        String fullTopic = LockerMessagingConfig.REQ_TOPIC_PREFIX + subTopic;
        byte[] json = mapper.writeValueAsBytes(payload);
        return MessageBuilder.withPayload(json)
                .setHeader(MqttHeaders.RECEIVED_TOPIC, fullTopic)
                .build();
    }

    /** helper to build a Message<?> with String payload */
    private Message<String> mqttString(String subTopic, Object payload) throws Exception {
        String fullTopic = LockerMessagingConfig.REQ_TOPIC_PREFIX + subTopic;
        String json = mapper.writeValueAsString(payload);
        return MessageBuilder.withPayload(json)
                .setHeader(MqttHeaders.RECEIVED_TOPIC, fullTopic)
                .build();
    }

    private static LockerDeviceRequest makeReq(RentalLockerAction action) {
        return new LockerDeviceRequest(10L, "00000", action, 20L);
    }

    @Nested
    @DisplayName("sub-topic = eligible")
    class EligibleTests {
        @Test
        @DisplayName("OTP 검증 후 eligible list push")
        void shouldHandleEligible() throws Exception {
            // given
            var req = makeReq(DROP_OFF_BY_OWNER);
            Message<byte[]> msg = mqttBytes("eligible", req);

            when(otpService.validateAndResolveIdentifier(req.otpCode()))
                    .thenReturn("user@example.com");
            MemberDto member = StudentDto.builder().memberId(1L).name("member").build();
            when(memberService.getMemberByEmail("user@example.com"))
                    .thenReturn(member);
            List<RentalBriefResponseForLocker> rentals = List.of(mock(RentalBriefResponseForLocker.class));
            when(rentalService.findEligibleRentals(member.getMemberId(), req.action()))
                    .thenReturn(rentals);

            // when
            listener.consume(msg);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<CommonResponse<EligibleRentalsEvent>> captor =
                    ArgumentCaptor.forClass(CommonResponse.class);
            verify(producer).pushEligibleRentals(eq(req.deviceId()), captor.capture());

            CommonResponse<EligibleRentalsEvent> resp = captor.getValue();
            assertThat(resp.isSuccess()).isTrue();
            EligibleRentalsEvent evt = resp.getData();
            assertThat(evt.deviceId()).isEqualTo(req.deviceId());
            assertThat(evt.action()).isEqualTo(req.action());
            assertThat(evt.memberId()).isEqualTo(member.getMemberId());
            assertThat(evt.rentals()).isEqualTo(rentals);
        }
    }

    @Nested
    @DisplayName("sub-topic = available")
    class AvailableTests {
        @Test
        @DisplayName("available list push")
        void shouldHandleAvailable() throws Exception {
            // given
            var req = makeReq(RETURN_BY_RENTER);
            Message<String> msg = mqttString("available", req);

            List<LockerBriefResponse> lockers = List.of(mock(LockerBriefResponse.class));
            when(lockerService.findAvailableLockers(req.deviceId()))
                    .thenReturn(lockers);

            // when
            listener.consume(msg);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<CommonResponse<AvailableLockersEvent>> captor =
                    ArgumentCaptor.forClass(CommonResponse.class);
            verify(producer).pushAvailableLockers(eq(req.deviceId()), captor.capture());

            CommonResponse<AvailableLockersEvent> resp = captor.getValue();
            assertThat(resp.isSuccess()).isTrue();
            AvailableLockersEvent evt = resp.getData();
            assertThat(evt.deviceId()).isEqualTo(req.deviceId());
            assertThat(evt.rentalId()).isEqualTo(req.rentalId());
            assertThat(evt.lockers()).isEqualTo(lockers);
        }
    }

    @Nested
    @DisplayName("sub-topic = event")
    class EventTests {
        private RentalLockerEventMessage makeEvent(RentalLockerAction action) {
            return new RentalLockerEventMessage(
                    20L,  // rentalId
                    30L,  // memberId
                    10L,  // deviceId
                    5L,   // lockerId
                    action,
                    1234L // fee
            );
        }

        @Test
        @DisplayName("DROP_OFF_BY_OWNER 처리 및 pushResult")
        void shouldHandleDropOffByOwner() throws Exception {
            // given
            var event = makeEvent(DROP_OFF_BY_OWNER);
            Message<String> msg = mqttString("event", event);

            // when
            listener.consume(msg);

            // then
            verify(rentalService).dropOffToLocker(
                    event.rentalId(),
                    event.memberId(),
                    event.deviceId(),
                    event.lockerId()
            );
            @SuppressWarnings("unchecked")
            ArgumentCaptor<CommonResponse<LockerActionResultEvent>> captor =
                    ArgumentCaptor.forClass(CommonResponse.class);
            verify(producer).pushResult(eq(event.deviceId()), captor.capture());

            CommonResponse<LockerActionResultEvent> resp = captor.getValue();
            assertThat(resp.isSuccess()).isTrue();
            LockerActionResultEvent re = resp.getData();
            assertThat(re.deviceId()).isEqualTo(event.deviceId());
            assertThat(re.lockerId()).isEqualTo(event.lockerId());
            assertThat(re.rentalId()).isEqualTo(event.rentalId());
        }

        @Test
        @DisplayName("예외 발생 시 failure pushResult")
        void shouldPushFailureOnException() throws Exception {
            // given
            var evt = makeEvent(PICK_UP_BY_RENTER);
            Message<String> msg = mqttString("event", evt);
            doThrow(new IllegalStateException("oops"))
                    .when(rentalService).pickUpByRenter(
                            evt.rentalId(), evt.memberId(), evt.fee()
                    );

            // when
            listener.consume(msg);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<CommonResponse<LockerActionResultEvent>> cap =
                    ArgumentCaptor.forClass(CommonResponse.class);
            verify(producer).pushResult(eq(evt.deviceId()), cap.capture());

            CommonResponse<LockerActionResultEvent> resp = cap.getValue();
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getMessage()).isEqualTo("oops");
        }
    }

    @Test
    @DisplayName("알 수 없는 sub-topic 무시")
    void shouldIgnoreUnknownSubTopic() throws Exception {
        var req = makeReq(PICK_UP_BY_RENTER);
        Message<String> msg = mqttString("foo", req);

        listener.consume(msg);

        verifyNoInteractions(otpService, memberService, rentalService,
                lockerService, producer);
    }
}
