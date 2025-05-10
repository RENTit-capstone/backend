package com.capstone.rentit.locker.message;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.locker.dto.LockerActionResultEvent;
import com.capstone.rentit.locker.dto.RentalLockerEventMessage;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.rental.service.RentalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalLockerEventListenerTest {

    @Mock
    private RentalService rentalService;
    @Mock
    private LockerDeviceProducer producer;

    private RentalLockerEventListener listener;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        listener = new RentalLockerEventListener(mapper, rentalService, producer);
    }

    /** Helper to build MQTT message with RECEIVED_TOPIC header */
    private Message<byte[]> mqttMsg(RentalLockerEventMessage event, String topic) throws Exception {
        byte[] payload = mapper.writeValueAsBytes(event);
        return MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.RECEIVED_TOPIC, topic)
                .build();
    }

    @Nested
    @DisplayName("Unknown topic")
    class UnknownTopic {
        @Test
        @DisplayName("Topic not starting with 'locker/request/event' is ignored")
        void shouldIgnoreNonEventTopic() throws Exception {
            // given
            var msg = new RentalLockerEventMessage(
                    1L, // deviceId
                    2L, // lockerId
                    3L, // rentalId
                    4L, // memberId
                    RentalLockerAction.DROP_OFF_BY_OWNER
            );
            Message<byte[]> mqtt = mqttMsg(msg, "locker/other/topic");

            // when
            listener.consume(mqtt);

            // then
            verifyNoInteractions(rentalService, producer);
        }
    }

    @Nested
    @DisplayName("Successful handling")
    class SuccessPath {
        @Test
        @DisplayName("All event types invoke correct service and push success response")
        void shouldHandleAllEventTypesSuccessfully() throws Exception {
            for (RentalLockerAction type : RentalLockerAction.values()) {
                // reset between iterations
                reset(rentalService, producer);

                // given
                var ev = new RentalLockerEventMessage(
                        10L, // deviceId
                        20L, // lockerId
                        30L, // rentalId
                        40L, // memberId
                        type
                );
                Message<byte[]> mqtt = mqttMsg(ev, "locker/request/event");

                // when
                listener.consume(mqtt);

                // then: service method invoked
                switch (type) {
                    case DROP_OFF_BY_OWNER ->
                            verify(rentalService).dropOffToLocker(30L, 40L, 10L, 20L);
                    case PICK_UP_BY_RENTER ->
                            verify(rentalService).pickUpByRenter(30L, 40L);
                    case RETURN_BY_RENTER ->
                            verify(rentalService).returnToLocker(30L, 40L, 10L, 20L, null);
                    case RETRIEVE_BY_OWNER ->
                            verify(rentalService).retrieveByOwner(30L, 40L);
                }

                // then: producer.pushResult with success
                ArgumentCaptor<CommonResponse> cap = ArgumentCaptor.forClass(CommonResponse.class);
                verify(producer).pushResult(eq(10L), cap.capture());
                CommonResponse<?> resp = cap.getValue();
                assertThat(resp.isSuccess()).isTrue();
                assertThat(resp.getData()).isInstanceOf(LockerActionResultEvent.class);

                // verify event fields
                var data = (LockerActionResultEvent) resp.getData();
                assertThat(data.deviceId()).isEqualTo(10L);
                assertThat(data.lockerId()).isEqualTo(20L);
                assertThat(data.rentalId()).isEqualTo(30L);
            }
        }
    }

    @Nested
    @DisplayName("Failure handling")
    class FailurePath {
        @Test
        @DisplayName("Exception in service leads to push failure response with error message")
        void shouldPushFailureOnException() throws Exception {
            // given
            var ev = new RentalLockerEventMessage(
                    5L,  // deviceId
                    6L,  // lockerId
                    7L,  // rentalId
                    8L,  // memberId
                    RentalLockerAction.DROP_OFF_BY_OWNER
            );
            Message<byte[]> mqtt = mqttMsg(ev, "locker/request/event");

            var ex = new RuntimeException("X error");
            doThrow(ex).when(rentalService)
                    .dropOffToLocker(7L, 8L, 5L, 6L);

            // when
            listener.consume(mqtt);

            // then: producer.pushResult with failure
            ArgumentCaptor<CommonResponse> cap = ArgumentCaptor.forClass(CommonResponse.class);
            verify(producer).pushResult(eq(5L), cap.capture());
            CommonResponse<?> resp = cap.getValue();
            assertThat(resp.isSuccess()).isFalse();
            assertThat(resp.getMessage()).isEqualTo("X error");
        }
    }
}
