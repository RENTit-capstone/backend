package com.capstone.rentit.locker.message;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.config.LockerMessagingConfig;
import com.capstone.rentit.locker.dto.*;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.rental.dto.RentalBriefResponseForLocker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MQTT 기반 Producer (서버 → 단말)
 *  topic 패턴 : locker/{deviceId}/{eligible|available|result}
 */
@Component
@RequiredArgsConstructor
public class LockerDeviceProducer {

    private final MessageChannel mqttOutboundChannel;     // LockerMessagingConfig#mqttOutboundChannel
    private final ObjectMapper   mapper;

    /* ---------- 공용 send ---------- */
    private void send(Long deviceId, String suffix, Object payload) {
        String topic = LockerMessagingConfig.RES_TOPIC_PREFIX + deviceId + "/" + suffix;

        // JSON 으로 변환
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("페이로드 직렬화 실패", e);
        }

        mqttOutboundChannel.send(
                MessageBuilder.withPayload(json)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .build()
        );
    }

    /* ---------- 비즈니스 메서드 ---------- */

    public void pushEligibleRentals(Long deviceId,
                                    CommonResponse<?> response) {
        send(deviceId, "eligible", response);
    }

    public void pushAvailableLockers(Long deviceId, CommonResponse<?> response) {
        send(deviceId, "available", response);
    }

    public void pushResult(Long deviceId, CommonResponse<?> response) {
        send(deviceId, "result", response);
    }

    /* ---------- util ---------- */
    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("MQTT payload 직렬화 실패", e);
        }
    }
}
