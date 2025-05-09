package com.capstone.rentit.locker.message;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.config.LockerMessagingConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.messaging.MessageChannel;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockerDeviceProducerTest {

    @Mock MessageChannel mqttOutboundChannel;
    ObjectMapper mapper;
    LockerDeviceProducer producer;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        producer = new LockerDeviceProducer(mqttOutboundChannel, mapper);
    }

    /**
     * 공통 JSON payload 캡처 헬퍼
     */
    private String captureJson(String expectedTopic) {
        ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
        verify(mqttOutboundChannel).send(captor.capture());
        verifyNoMoreInteractions(mqttOutboundChannel);

        Message<?> msg = captor.getValue();
        assertThat(msg.getHeaders().get(MqttHeaders.TOPIC))
                .isEqualTo(expectedTopic);

        Object payload = msg.getPayload();
        assertThat(payload).isInstanceOf(String.class);
        return (String) payload;
    }

    @Nested
    @DisplayName("pushEligibleRentals")
    class PushEligible {
        @Test
        @DisplayName("정상 전송 시 CommonResponse가 JSON으로 전송된다")
        void pushEligibleRentals_success() {
            long deviceId = 1L;
            CommonResponse<String> response = CommonResponse.success("hello");

            producer.pushEligibleRentals(deviceId, response);

            String topic = LockerMessagingConfig.RES_TOPIC_PREFIX + deviceId + "/eligible";
            String json = captureJson(topic);

            assertThat(json).contains("\"data\":\"hello\"");
            assertThat(json).contains("\"success\":true");
        }
    }

    @Nested
    @DisplayName("pushAvailableLockers")
    class PushAvailable {
        @Test
        @DisplayName("정상 전송 시 리스트 형태의 데이터가 JSON으로 전송된다")
        void pushAvailableLockers_success() {
            long deviceId = 2L;
            List<String> list = List.of("A", "B");
            CommonResponse<List<String>> response = CommonResponse.success(list);

            producer.pushAvailableLockers(deviceId, response);

            String topic = LockerMessagingConfig.RES_TOPIC_PREFIX + deviceId + "/available";
            String json = captureJson(topic);

            assertThat(json).contains("\"data\":[\"A\",\"B\"]");
            assertThat(json).contains("\"success\":true");
        }
    }

    @Nested
    @DisplayName("pushResult")
    class PushResult {
        @Test
        @DisplayName("정상 전송 시 null 데이터도 올바르게 JSON으로 전송된다")
        void pushResult_successWithNullData() {
            long deviceId = 3L;
            CommonResponse<Void> response = CommonResponse.success(null);

            producer.pushResult(deviceId, response);

            String topic = LockerMessagingConfig.RES_TOPIC_PREFIX + deviceId + "/result";
            String json = captureJson(topic);

            assertThat(json).contains("\"data\":null");
            assertThat(json).contains("\"success\":true");
        }
    }

    @Nested
    @DisplayName("JSON 직렬화 실패")
    class SerializationFailure {
        @Test
        @DisplayName("mapper.writeValueAsString 오류 시 IllegalStateException 발생 및 채널 전송 없음")
        void throwsIllegalStateOnSerializationError() throws Exception {
            ObjectMapper badMapper = mock(ObjectMapper.class);
            when(badMapper.writeValueAsString(any()))
                    .thenThrow(JsonProcessingException.class);
            producer = new LockerDeviceProducer(mqttOutboundChannel, badMapper);
            CommonResponse<String> response = CommonResponse.success("fail");

            assertThatThrownBy(() -> producer.pushEligibleRentals(1L, response))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("페이로드 직렬화 실패");

            verifyNoInteractions(mqttOutboundChannel);
        }
    }
}
