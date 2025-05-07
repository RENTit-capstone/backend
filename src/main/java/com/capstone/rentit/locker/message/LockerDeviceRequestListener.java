package com.capstone.rentit.locker.message;

import com.capstone.rentit.config.LockerMessagingConfig;
import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.locker.dto.LockerDeviceRequest;
import com.capstone.rentit.locker.dto.LockerDto;
import com.capstone.rentit.locker.service.LockerService;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.member.service.MemberService;
import com.capstone.rentit.otp.service.OtpService;
import com.capstone.rentit.rental.dto.RentalBriefResponseForLocker;
import com.capstone.rentit.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LockerDeviceRequestListener {

    private final OtpService otpService;
    private final MemberService memberService;
    private final RentalService rentalService;
    private final LockerService lockerService;
    private final LockerDeviceProducer producer;

    /* 모든 locker.request.# 를 한 큐로 수신 */
    @RabbitListener(queues = LockerMessagingConfig.REQUEST_Q)
    public void handle(LockerDeviceRequest req, Message msg) {

        String routingKey = msg.getMessageProperties().getReceivedRoutingKey();
        log.info("REQ [{}] ▶ {}", routingKey, req);

        /* OTP 검증 → 사용자 */
        String email = otpService.validateAndResolveIdentifier(req.otpCode(), req.email());
        MemberDto member = memberService.getMemberByEmail(email);

        switch (routingKey) {
            case "locker.request.eligible"  -> sendEligible(req, member);
            case "locker.request.available" -> sendAvailable(req);
            default -> log.warn("알 수 없는 라우팅키: {}", routingKey);
        }
    }

    private void sendEligible(LockerDeviceRequest request, MemberDto member) {
        List<RentalBriefResponseForLocker> list = rentalService
                .findEligibleRentals(member.getMemberId(), request.action());
        producer.pushEligibleRentals(request.lockerId(), request.action(), list);
    }

    private void sendAvailable(LockerDeviceRequest request) {
        List<LockerDto> lockers =
                lockerService.findAvailableLockers(request.university());
        producer.pushAvailableLockers(request.lockerId(), request.rentalId(), lockers);
    }
}