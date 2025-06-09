package com.capstone.rentit.notification.scheduler;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.notification.service.NotificationService;
import com.capstone.rentit.notification.type.NotificationType;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.repository.RentalRepository;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalDeadlineNotifierTest {

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private RentalDeadlineNotifier rentalDeadlineNotifier;

    private Rental createMockRental(Long id, Member owner, Member renter, RentalStatusEnum status) {
        Rental rental = mock(Rental.class);
        Item item = mock(Item.class);

        // 공통 Mock 설정
        lenient().when(rental.getRentalId()).thenReturn(id);
        lenient().when(item.getName()).thenReturn("물품" + id);
        lenient().when(item.getOwner()).thenReturn(owner);
        lenient().when(rental.getItem()).thenReturn(item);
        lenient().when(rental.getRenterMember()).thenReturn(renter);

        // 상태(Status) Mock 설정 추가
        lenient().when(rental.getStatus()).thenReturn(status);

        return rental;
    }

    private Member createMockMember(Long id) {
        Member member = mock(Member.class);
        lenient().when(member.getMemberId()).thenReturn(id);
        return member;
    }

    @Nested
    @DisplayName("sendStartAndEndAlerts() 메서드는")
    class SendAlerts {

        private final LocalDate today = LocalDate.now();
        private final LocalDate d3 = today.plusDays(3);

        @Test
        @DisplayName("올바른 날짜와 상태를 가진 모든 렌탈 건에 대해 알림을 전송한다")
        void sendAlerts_forAllValidCases() {
            // given: 각 조건에 맞는 Mock Rental 객체 생성
            Member owner1 = createMockMember(1L);
            Member renter2 = createMockMember(2L);
            Member owner3 = createMockMember(3L);
            Member renter4 = createMockMember(4L);

            // 대여 시작 알림 대상 (APPROVED 상태)
            Rental startD3 = createMockRental(1L, owner1, renter2, RentalStatusEnum.APPROVED);
            Rental startD0 = createMockRental(2L, owner3, renter4, RentalStatusEnum.APPROVED);

            // 반납 마감 알림 대상 (PICKED_UP 상태)
            Rental endD3 = createMockRental(3L, createMockMember(5L), createMockMember(6L), RentalStatusEnum.PICKED_UP);
            Rental endD0 = createMockRental(4L, createMockMember(7L), createMockMember(8L), RentalStatusEnum.PICKED_UP);

            // Repository Mock 설정
            when(rentalRepository.findByStartDateBetween(d3.atStartOfDay(), d3.atTime(LocalTime.MAX))).thenReturn(List.of(startD3));
            when(rentalRepository.findByStartDateBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX))).thenReturn(List.of(startD0));
            when(rentalRepository.findByDueDateBetween(d3.atStartOfDay(), d3.atTime(LocalTime.MAX))).thenReturn(List.of(endD3));
            when(rentalRepository.findByDueDateBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX))).thenReturn(List.of(endD0));

            // when
            rentalDeadlineNotifier.sendStartAndEndAlerts();

            // then: 각 타입별 알림이 정확히 한 번씩 호출되었는지 검증
            verify(notificationService).notify(eq(startD3.getItem().getOwner()), eq(NotificationType.RENT_START_D_3), anyString(), anyString(), any(Map.class));
            verify(notificationService).notify(eq(startD0.getItem().getOwner()), eq(NotificationType.RENT_START_D_0), anyString(), anyString(), any(Map.class));
            verify(notificationService).notify(eq(endD3.getRenterMember()), eq(NotificationType.RENT_END_D_3), anyString(), anyString(), any(Map.class));
            verify(notificationService).notify(eq(endD0.getRenterMember()), eq(NotificationType.RENT_END_D_0), anyString(), anyString(), any(Map.class));

            // 총 4회 호출 검증
            verify(notificationService, times(4)).notify(any(Member.class), any(NotificationType.class), anyString(), anyString(), any(Map.class));
        }

        @Test
        @DisplayName("날짜는 맞지만 상태가 올바르지 않으면 알림을 보내지 않는다")
        void sendAlerts_ignoresRentals_withWrongStatus() {
            // given: 날짜는 맞지만 상태가 잘못된 Mock Rental 객체
            Rental startD3WrongStatus = createMockRental(1L, createMockMember(1L), createMockMember(2L), RentalStatusEnum.PICKED_UP); // APPROVED여야 함
            Rental endD3WrongStatus = createMockRental(2L, createMockMember(3L), createMockMember(4L), RentalStatusEnum.APPROVED);   // PICKED_UP이어야 함

            when(rentalRepository.findByStartDateBetween(d3.atStartOfDay(), d3.atTime(LocalTime.MAX))).thenReturn(List.of(startD3WrongStatus));
            when(rentalRepository.findByDueDateBetween(d3.atStartOfDay(), d3.atTime(LocalTime.MAX))).thenReturn(List.of(endD3WrongStatus));

            // when
            rentalDeadlineNotifier.sendStartAndEndAlerts();

            // then: notify 메서드가 한 번도 호출되지 않았는지 검증
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("해당 날짜의 렌탈이 없으면 알림을 보내지 않는다")
        void sendAlerts_noRentals_callsNothing() {
            // given: 모든 Repository 메서드가 빈 리스트를 반환하도록 설정
            when(rentalRepository.findByStartDateBetween(any(), any())).thenReturn(List.of());
            when(rentalRepository.findByDueDateBetween(any(), any())).thenReturn(List.of());

            // when
            rentalDeadlineNotifier.sendStartAndEndAlerts();

            // then: notify 메서드가 한 번도 호출되지 않았는지 검증
            verifyNoInteractions(notificationService);
        }
    }
}