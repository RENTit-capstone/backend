package com.capstone.rentit.rental.scheduler;

import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RentalOverdueScheduler {

    private final RentalRepository rentalRepository;

    /**
     * 매일 자정(00:00)마다 실행.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void markOverdueRentals() {
        LocalDateTime now = LocalDateTime.now();

        List<RentalStatusEnum> activeStatuses = Arrays.asList(
                RentalStatusEnum.LEFT_IN_LOCKER,
                RentalStatusEnum.PICKED_UP
        );

        // dueDate < now 이면서 아직 반환되지 않은(연체 가능한) 대여 검색
        List<Rental> overdueList = rentalRepository.findByStatusInAndDueDateBefore(activeStatuses, now);

        if (!overdueList.isEmpty()) {
            overdueList.forEach(Rental::markDelayed);
            rentalRepository.saveAll(overdueList);
        }
    }
}
