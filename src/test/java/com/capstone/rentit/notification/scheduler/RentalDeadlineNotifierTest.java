package com.capstone.rentit.notification.scheduler;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.notification.service.NotificationService;
import com.capstone.rentit.notification.type.NotificationType;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.repository.RentalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime; // LocalTime import 추가
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalDeadlineNotifierTest {

    @Mock RentalRepository rentalRepository;
    @Mock NotificationService notificationService;

    // SUT(System Under Test)는 변경 없이 그대로 사용합니다.
    private RentalDeadlineNotifier sut() {
        return new RentalDeadlineNotifier(rentalRepository, notificationService);
    }

    // 테스트 데이터 생성을 위한 헬퍼 메서드도 변경 없이 그대로 사용합니다.
    private Member member(Long id) {
        Member m = mock(Member.class);
        lenient().when(m.getMemberId()).thenReturn(id);
        return m;
    }

    private Rental rental(Long id, Member owner, Member renter) {
        Rental r = mock(Rental.class);
        // getRenterMember()에 대한 Mock 설정 추가
        lenient().when(r.getRenterMember()).thenReturn(renter);
        lenient().when(r.getRentalId()).thenReturn(id);

        Item i = mock(Item.class);
        lenient().when(i.getName()).thenReturn("물품" + id);
        // getItem().getOwner()에 대한 Mock 설정 추가
        lenient().when(i.getOwner()).thenReturn(owner);
        lenient().when(r.getItem()).thenReturn(i);

        return r;
    }

    @Nested
    @DisplayName("sendStartAndEndAlerts()")
    class SendAlerts {

        @Test
        @DisplayName("오늘·D-3 일정에 해당하는 모든 렌탈에 알림을 전송한다")
        void sendAlerts_forAllFourCases() {
            // 날짜 계산
            LocalDate today = LocalDate.now();
            LocalDate d3    = today.plusDays(3);

            // 테스트 렌탈 4건
            Rental startD3  = rental(1L, member(1L), member(2L));
            Rental startD0  = rental(2L, member(3L), member(4L));
            Rental endD3    = rental(3L, member(5L), member(6L));
            Rental endD0    = rental(4L, member(7L), member(8L));

            // Repository 스텁 설정 (변경된 부분)
            // findBy...Between 메서드가 LocalDateTime 범위로 호출될 것을 stubbing 합니다.
            when(rentalRepository.findByStartDateBetween(d3.atStartOfDay(), d3.atTime(LocalTime.MAX))).thenReturn(List.of(startD3));
            when(rentalRepository.findByStartDateBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX))).thenReturn(List.of(startD0));
            when(rentalRepository.findByDueDateBetween(d3.atStartOfDay(), d3.atTime(LocalTime.MAX))).thenReturn(List.of(endD3));
            when(rentalRepository.findByDueDateBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX))).thenReturn(List.of(endD0));

            // when
            sut().sendStartAndEndAlerts();

            // then: 각 타입별 알림이 한 번씩 (검증 로직은 동일)
            verify(notificationService).notify(
                    eq(startD3.getItem().getOwner()),
                    eq(NotificationType.RENT_START_D_3),
                    anyString(), anyString(), anyMap());

            verify(notificationService).notify(
                    eq(startD0.getItem().getOwner()),
                    eq(NotificationType.RENT_START_D_0),
                    anyString(), anyString(), anyMap());

            verify(notificationService).notify(
                    eq(endD3.getRenterMember()),
                    eq(NotificationType.RENT_END_D_3),
                    anyString(), anyString(), anyMap());

            verify(notificationService).notify(
                    eq(endD0.getRenterMember()),
                    eq(NotificationType.RENT_END_D_0),
                    anyString(), anyString(), anyMap());

            // 총 4회 호출 검증
            verify(notificationService, times(4))
                    .notify(any(), any(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("해당 날짜의 렌탈이 없으면 알림을 보내지 않는다")
        void sendAlerts_noRentals_callsNothing() {
            LocalDate today = LocalDate.now();
            LocalDate d3    = today.plusDays(3);

            // 모든 케이스 빈 리스트 반환 (변경된 부분)
            when(rentalRepository.findByStartDateBetween(any(), any())).thenReturn(List.of());
            when(rentalRepository.findByDueDateBetween(any(), any())).thenReturn(List.of());

            sut().sendStartAndEndAlerts();

            verifyNoInteractions(notificationService);
        }
    }
}