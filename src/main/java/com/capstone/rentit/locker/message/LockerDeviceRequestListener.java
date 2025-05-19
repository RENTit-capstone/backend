package com.capstone.rentit.locker.message;

import com.capstone.rentit.common.CommonResponse;
import com.capstone.rentit.config.LockerMessagingConfig;
import com.capstone.rentit.locker.dto.*;
import com.capstone.rentit.locker.service.LockerService;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.otp.service.OtpService;
import com.capstone.rentit.rental.dto.RentalBriefResponseForLocker;
import com.capstone.rentit.rental.service.RentalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.capstone.rentit.locker.event.RentalLockerAction.*;

/**
 * MQTT Inbound Listener
 *  topic : locker/request/{eligible|available}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockerDeviceRequestListener {

    private final ObjectMapper mapper;
    private final OtpService otpService;
    private final MemberService memberService;
    private final RentalService rentalService;
    private final LockerService lockerService;
    private final LockerDeviceProducer producer;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void consume(Message<?> mqttMsg) {
        // 1) 토픽 추출
        String topic = mqttMsg.getHeaders()
                .get(MqttHeaders.RECEIVED_TOPIC, String.class);
        if (topic == null) return;

        // 2) payload → String
        Object payloadObj = mqttMsg.getPayload();
        String json;
        if (payloadObj instanceof byte[] bytes) {
            json = new String(bytes, StandardCharsets.UTF_8);
        } else {
            json = payloadObj.toString();
        }

        // 3) sub-topic 분기
        String sub = topic.substring(LockerMessagingConfig.REQ_TOPIC_PREFIX.length());

        try {
            switch (sub) {
                case "eligible", "available" -> handleRequest(sub, json);
                default -> {
                    if (sub.startsWith("event")) {
                        handleEvent(json, topic);
                    } else {
                        log.debug("알 수 없는 요청 sub-topic: {}", sub);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error handling MQTT [{}]: {}", topic, e.getMessage(), e);
        }
    }

    // ---------------------------------------------------
    // { eligible | available } 요청 처리
    // ---------------------------------------------------
    private void handleRequest(String sub, String json) throws Exception {
        LockerDeviceRequest req = mapper.readValue(json, LockerDeviceRequest.class);

        if ("eligible".equals(sub)) {
            String email = otpService.validateAndResolveIdentifier(req.otpCode());
            MemberDto member = memberService.getMemberByEmail(email);
            List<RentalBriefResponseForLocker> rentals =
                    rentalService.findEligibleRentals(member.getMemberId(), req.action());
            producer.pushEligibleRentals(req.deviceId(),
                    CommonResponse.success(
                            new EligibleRentalsEvent(req.deviceId(), req.action(),
                                    member.getMemberId(), rentals)
                    ));
        } else { // "available"
            var lockers = lockerService.findAvailableLockers(req.deviceId());
            producer.pushAvailableLockers(req.deviceId(),
                    CommonResponse.success(
                            new AvailableLockersEvent(req.deviceId(), req.rentalId(), lockers)
                    ));
        }
    }

    private void handleEvent(String json, String topic) throws Exception {
        RentalLockerEventMessage event =
                mapper.readValue(json, RentalLockerEventMessage.class);

        log.info("RentalLockerEvent ▶ {}", event);

        CommonResponse<?> response;
        try {
            switch (event.action()) {
                case DROP_OFF_BY_OWNER ->
                        rentalService.dropOffToLocker(
                                event.rentalId(), event.memberId(),
                                event.deviceId(), event.lockerId());
                case PICK_UP_BY_RENTER ->
                        rentalService.pickUpByRenter(
                                event.rentalId(), event.memberId(), event.fee());
                case RETURN_BY_RENTER ->
                        rentalService.returnToLocker(
                                event.rentalId(), event.memberId(),
                                event.deviceId(), event.lockerId());
                case RETRIEVE_BY_OWNER ->
                        rentalService.retrieveByOwner(
                                event.rentalId(), event.memberId(), event.fee());
            }
            response = CommonResponse.success(
                    new LockerActionResultEvent(
                            event.deviceId(), event.lockerId(), event.rentalId()));
        } catch (Exception e) {
            log.error("RentalLockerEvent 처리 실패", e);
            response = CommonResponse.failure(e.getMessage());
        }

        // 결과 MQTT push
        producer.pushResult(event.deviceId(), response);
    }
}
