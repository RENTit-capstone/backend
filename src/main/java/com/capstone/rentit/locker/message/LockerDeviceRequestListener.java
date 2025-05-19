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
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MQTT Inbound Listener
 *  topic : locker/request/{eligible|available}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockerDeviceRequestListener {

    private final ObjectMapper        mapper;
    private final OtpService          otpService;
    private final MemberService       memberService;
    private final RentalService       rentalService;
    private final LockerService       lockerService;
    private final LockerDeviceProducer producer;

    /** mqttInboundChannel 은 LockerMessagingConfig 에서 정의 */
    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handle(Message<?> message) throws Exception {
        // 1) Inspect raw payload for debugging
        Object raw = message.getPayload();
        log.info("Raw payload action={}, value={}", raw.getClass().getSimpleName(), raw);

        // 2) Convert to JSON string
        String json;
        if (raw instanceof byte[] bytes) {
            json = new String(bytes, StandardCharsets.UTF_8);
        } else {
            json = raw.toString();  // already a String
        }

        // 3) Deserialize
        LockerDeviceRequest req = mapper.readValue(json, LockerDeviceRequest.class);

        // 4) Extract sub-topic
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String sub   = topic.substring(LockerMessagingConfig.REQ_TOPIC_PREFIX.length());

        // 5) Now this log will fire
        log.info("MQTT REQ [{}] ▶ {}", sub, req);

        switch (sub) {
            case "eligible"  -> sendEligible(req);
            case "available" -> sendAvailable(req);
            default          -> log.warn("알 수 없는 요청 sub-topic: {}", sub);
        }
    }

    /* ---------- helpers ---------- */

    private void sendEligible(LockerDeviceRequest r) {
        /* OTP → 사용자 */
        String email = "";
        try {
            email = otpService.validateAndResolveIdentifier(r.otpCode());
        }
        catch (RuntimeException e){
            producer.pushEligibleRentals(r.deviceId(), CommonResponse.failure(e.getMessage()));
            return;
        }
        MemberDto member = memberService.getMemberByEmail(email);

        List<RentalBriefResponseForLocker> rentals =
                rentalService.findEligibleRentals(member.getMemberId(), r.action());

        producer.pushEligibleRentals(r.deviceId(),
                CommonResponse.success(new EligibleRentalsEvent(r.deviceId(), r.action(), member.getMemberId(), rentals)));
    }


    private void sendAvailable(LockerDeviceRequest r) {
        log.info("find locker start--------------------------------------------");
        List<LockerBriefResponse> lockers =
                lockerService.findAvailableLockers(r.deviceId());
        log.info("find locker end--------------------------------------------");
        producer.pushAvailableLockers(r.deviceId(),
                CommonResponse.success(new AvailableLockersEvent(r.deviceId(), r.rentalId(), lockers)));
    }
}
