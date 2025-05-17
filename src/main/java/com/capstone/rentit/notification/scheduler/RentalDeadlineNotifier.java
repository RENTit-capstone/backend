package com.capstone.rentit.notification.scheduler;

import com.capstone.rentit.notification.service.NotificationService;
import com.capstone.rentit.notification.type.NotificationType;
import com.capstone.rentit.rental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RentalDeadlineNotifier {

    private final RentalRepository rentalRepository;
    private final NotificationService notificationService;

    // 매일 새벽 0시(00:10)에 실행
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void sendStartAndEndAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate d3 = today.plusDays(3);

        // 대여 시작 3일 전 & 당일 → 소유자
        rentalRepository.findByStartDate(d3)
                .forEach(r -> notificationService.notify(
                        r.getItem().getOwner(),
                        NotificationType.RENT_START_D_3,
                        "대여 시작 D-3",
                        r.getItem().getName() + " 대여가 3일 후 시작됩니다.",
                        Map.of("rentalId", r.getRentalId().toString())
                ));

        rentalRepository.findByStartDate(today)
                .forEach(r -> notificationService.notify(
                        r.getItem().getOwner(),
                        NotificationType.RENT_START_D_0,
                        "대여 시작일!",
                        r.getItem().getName() + " 대여가 오늘부터 시작됩니다.",
                        Map.of("rentalId", r.getRentalId().toString())
                ));

        // 대여 마감 3일 전 & 당일 → 대여자
        rentalRepository.findByDueDate(d3)
                .forEach(r -> notificationService.notify(
                        r.getRenterMember(),
                        NotificationType.RENT_END_D_3,
                        "반납 D-3",
                        r.getItem().getName() + " 반납일까지 3일 남았습니다.",
                        Map.of("rentalId", r.getRentalId().toString())
                ));

        rentalRepository.findByDueDate(today)
                .forEach(r -> notificationService.notify(
                        r.getRenterMember(),
                        NotificationType.RENT_END_D_0,
                        "반납 마감일!",
                        r.getItem().getName() + " 오늘까지 반납해야 합니다.",
                        Map.of("rentalId", r.getRentalId().toString())
                ));
    }
}