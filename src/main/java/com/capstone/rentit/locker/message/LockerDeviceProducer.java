package com.capstone.rentit.locker.message;

import com.capstone.rentit.config.LockerMessagingConfig;
import com.capstone.rentit.locker.dto.*;
import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.rental.dto.RentalBriefResponseForLocker;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LockerDeviceProducer {

    private final RabbitTemplate rabbit;

    private void send(Long lockerId, String suffix, LockerDeviceEvent payload) {
        String key = "locker." + lockerId + "." + suffix;
        rabbit.convertAndSend(LockerMessagingConfig.DEVICE_EX, key, payload);
    }

    public void pushEligibleRentals(Long lockerId, RentalLockerAction action,
                                    List<RentalBriefResponseForLocker> responses) {
        send(lockerId, "eligible", new EligibleRentalsEvent(lockerId, action, responses));
    }

    public void pushAvailableLockers(Long lockerId, Long rentalId,
                                     List<LockerDto> lockers) {
        send(lockerId, "available", new AvailableLockersEvent(lockerId, rentalId, lockers));
    }

    public void pushResult(Long lockerId, Long rentalId,
                           boolean success, String error) {
        send(lockerId, "result",
                new LockerActionResultEvent(lockerId, rentalId, success,
                        success ? "" : error));
    }
}

