package com.capstone.rentit.locker.message;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.locker.dto.LockerActionResultEvent;
import com.capstone.rentit.locker.dto.RentalLockerEventMessage;
import com.capstone.rentit.rental.service.RentalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

/**
 * MQTT Listener : 단말이 문을 닫은 뒤 보내는 RentalLockerEventMessage 수신
 *  topic 예)  locker/request/event   또는  locker/request/event/{something}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalLockerEventListener {

    private final ObjectMapper          mapper;
    private final RentalService         rentalService;
    private final LockerDeviceProducer  producer;

    /** LockerMessagingConfig 에서 mqttInboundChannel = 단말→서버 모든 구독 */
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void consume(Message<byte[]> mqttMsg) throws Exception {

        MessageHeaders headers = mqttMsg.getHeaders();
        String topic = headers.get(MqttHeaders.RECEIVED_TOPIC, String.class);

        // event 토픽만 처리
        if (!topic.startsWith("locker/request/event")) {
            return;                 // 다른 메시지는 무시
        }

        RentalLockerEventMessage msg =
                mapper.readValue(mqttMsg.getPayload(), RentalLockerEventMessage.class);

        log.info("RentalLockerEvent ▶ {}", msg);

        CommonResponse<?> response;
        try {
            switch (msg.action()) {
                case DROP_OFF_BY_OWNER ->
                        rentalService.dropOffToLocker(
                                msg.rentalId(), msg.memberId(), msg.deviceId(), msg.lockerId());
                case PICK_UP_BY_RENTER ->
                        rentalService.pickUpByRenter(
                                msg.rentalId(), msg.memberId(), msg.fee());
                case RETURN_BY_RENTER ->
                        rentalService.returnToLocker(
                                msg.rentalId(), msg.memberId(), msg.deviceId(), msg.lockerId(), null);
                case RETRIEVE_BY_OWNER ->
                        rentalService.retrieveByOwner(
                                msg.rentalId(), msg.memberId(), msg.fee());
            }
            response = CommonResponse.success(new LockerActionResultEvent(msg.deviceId(), msg.lockerId(), msg.rentalId()));
        } catch (Exception e) {
            log.error("RentalLockerEvent 처리 실패", e);
            response = CommonResponse.failure(e.getMessage());
        }

        /* 성공 / 실패 결과 MQTT push */
        producer.pushResult(msg.deviceId(), response);
    }
}
