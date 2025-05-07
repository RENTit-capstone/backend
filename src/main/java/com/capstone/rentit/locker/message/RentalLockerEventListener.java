package com.capstone.rentit.locker.message;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.locker.dto.RentalLockerEventMessage;
import com.capstone.rentit.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentalLockerEventListener {

    private final RentalService rentalService;
    private final LockerDeviceProducer producer;
    private final FileStorageService fileStorage;

    @RabbitListener(queues = "rental.locker.queue") // 기존 큐
    public void consume(RentalLockerEventMessage message) {
        log.info("RentalLockerEvent ▶ {}", message);

        boolean ok  = false;
        String  err = null;

        try {
            switch (message.type()) {
                case DROP_OFF_BY_OWNER ->
                        rentalService.dropOffToLocker(message.rentalId(), message.memberId(), message.lockerId());
                case PICK_UP_BY_RENTER ->
                        rentalService.pickUpByRenter(message.rentalId(), message.memberId());
                case RETURN_TO_LOCKER ->
                        rentalService.returnToLocker(message.rentalId(), message.memberId(), message.lockerId(), null);
                case RETRIEVE_BY_OWNER ->
                        rentalService.retrieveByOwner(message.rentalId(), message.memberId());
            }
            ok = true;
        } catch (Exception e) {
            log.error("RentalLockerEvent 처리 실패", e);
            err = e.getMessage();
        }

        producer.pushResult(message.lockerId(), message.rentalId(), ok, err);
    }
}
