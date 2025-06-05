package com.capstone.rentit.rental.scheduler;

import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalOverdueSchedulerTest {

    @Mock
    private RentalRepository rentalRepository;

    @InjectMocks
    private RentalOverdueScheduler scheduler;

    private LocalDateTime baseNow;

    @BeforeEach
    void setUp() {
        // 테스트 수행 시점의 기준 시간. 각 테스트 메서드에서 dueDate 계산에 사용한다.
        baseNow = LocalDateTime.now();
    }

    @Test
    void whenOverdueRentalsExist_thenMarkStatusDelayedAndSaveAll() {
        // 1) 연체될 두 개의 Rental 엔티티를 만든다.
        Rental rentalPickedUp = Rental.builder()
                .itemId(1L)
                .ownerId(10L)
                .renterId(20L)
                .requestDate(baseNow.minusDays(5))
                .startDate(baseNow.minusDays(5))
                .status(RentalStatusEnum.PICKED_UP)
                // dueDate가 baseNow보다 하루 전이므로 연체 대상
                .dueDate(baseNow.minusDays(1))
                .build();

        Rental rentalLeftInLocker = Rental.builder()
                .itemId(2L)
                .ownerId(11L)
                .renterId(21L)
                .requestDate(baseNow.minusDays(4))
                .startDate(baseNow.minusDays(4))
                .status(RentalStatusEnum.LEFT_IN_LOCKER)
                // dueDate가 baseNow보다 이틀 전이므로 연체 대상
                .dueDate(baseNow.minusDays(2))
                .build();

        List<Rental> overdueList = Arrays.asList(rentalPickedUp, rentalLeftInLocker);

        // 2) rentalRepository.findByStatusInAndDueDateBefore(...) 호출 시 위 목록을 반환하도록 stub 설정
        when(rentalRepository.findByStatusInAndDueDateBefore(
                eq(Arrays.asList(RentalStatusEnum.LEFT_IN_LOCKER, RentalStatusEnum.PICKED_UP)),
                any(LocalDateTime.class))
        ).thenReturn(overdueList);

        scheduler.markOverdueRentals();

        // 1) 각 Rental의 상태가 DELAYED로 변경되었는지 확인
        assertEquals(RentalStatusEnum.DELAYED, rentalPickedUp.getStatus(),
                "PICKED_UP 상태였던 대여는 DELAYED로 바뀌어야 한다.");
        assertEquals(RentalStatusEnum.DELAYED, rentalLeftInLocker.getStatus(),
                "LEFT_IN_LOCKER 상태였던 대여는 DELAYED로 바뀌어야 한다.");

        // 2) saveAll(...)이 정확히 overdueList를 인수로 한 번 호출했는지 검증
        verify(rentalRepository, times(1)).saveAll(overdueList);

        // 3) findByStatusInAndDueDateBefore(...)이 정확한 인자로 호출되었는지 확인
        ArgumentCaptor<List<RentalStatusEnum>> statusCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(rentalRepository).findByStatusInAndDueDateBefore(
                statusCaptor.capture(),
                timeCaptor.capture()
        );

        List<RentalStatusEnum> capturedStatuses = statusCaptor.getValue();
        // 조회할 상태 리스트가 정확히 [LEFT_IN_LOCKER, PICKED_UP]이어야 한다.
        assertEquals(
                Arrays.asList(RentalStatusEnum.LEFT_IN_LOCKER, RentalStatusEnum.PICKED_UP),
                capturedStatuses,
                "조회할 상태 리스트는 LEFT_IN_LOCKER, PICKED_UP 순이어야 한다."
        );
        // 조회 시점(now)이 baseNow와 큰 차이가 없어야 한다.
        assertTrue(
                !timeCaptor.getValue().isBefore(baseNow.minusSeconds(1))
                        && !timeCaptor.getValue().isAfter(LocalDateTime.now().plusSeconds(1)),
                "조회 시점(now)는 현재 시각과 거의 일치해야 한다."
        );
    }

    @Test
    void whenNoOverdueRentals_thenDoNothing() {
        // overdue 대상이 없도록 빈 리스트 반환
        when(rentalRepository.findByStatusInAndDueDateBefore(
                anyList(),
                any(LocalDateTime.class))
        ).thenReturn(Collections.emptyList());

        scheduler.markOverdueRentals();

        // 1) saveAll이 한 번도 호출되지 않아야 한다.
        verify(rentalRepository, never()).saveAll(anyList());
        // 2) findByStatusInAndDueDateBefore는 한 번 호출된다.
        verify(rentalRepository, times(1)).findByStatusInAndDueDateBefore(
                eq(Arrays.asList(RentalStatusEnum.LEFT_IN_LOCKER, RentalStatusEnum.PICKED_UP)),
                any(LocalDateTime.class)
        );
    }
}
