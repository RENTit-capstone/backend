package com.capstone.rentit.notification.service;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.notification.domain.Notification;
import com.capstone.rentit.notification.exception.NotificationAccessDenied;
import com.capstone.rentit.notification.repository.NotificationRepository;
import com.capstone.rentit.notification.type.NotificationType;
import com.capstone.rentit.rental.domain.Rental;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock FcmService fcmService;

    @Mock MemberRepository memberRepository;

    /* 공통 테스트 데이터 */
    private Member owner(String token) {
        Member m = mock(Student.class);
        when(m.getMemberId()).thenReturn(1L);
        lenient().when(m.getFcmToken()).thenReturn(token);
        return m;
    }

    private Member renter() {
        Member m = mock(Student.class);
        lenient().when(m.getMemberId()).thenReturn(2L);
        return m;
    }

    private Rental rental(Member owner, Member renter) {
        Rental r = mock(Rental.class);

        when(r.getRentalId()).thenReturn(99L);
        when(r.getOwnerMember()).thenReturn(owner);

        Item item = Item.builder()
                .itemId(1L)
                .name("맥북")
                .ownerId(owner.getMemberId())
                .build();
        doReturn(item).when(r).getItem();

        return r;
    }


    @Nested
    @DisplayName("notify() – 디바이스 토큰 유무")
    class NotifyTests {

        @Test
        @DisplayName("토큰이 있으면 FCM 전송까지 수행한다")
        void notify_withToken_savesAndSends() {
            // arrange
            Member owner = owner("token-123");
            Rental rental = rental(owner, renter());

            NotificationService sut = new NotificationService(notificationRepository, memberRepository, fcmService);

            // save() 동작만 확인하면 되므로 반환값 무시
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // act
            sut.notifyRentRequest(rental);

            // assert – DB 저장
            verify(notificationRepository).save(argThat(n ->
                    n.getTarget().equals(owner)
                            && n.getType() == NotificationType.RENT_REQUESTED
                            && n.getTitle().equals("새 대여 신청")
                            && !n.isRead()
            ));

            // assert – FCM 전송
            verify(fcmService).sendToToken(eq("token-123"),
                    eq("새 대여 신청"),
                    contains("새 대여 신청이 들어왔어요"),
                    anyMap());
        }

        @Test
        @DisplayName("토큰이 없으면 FCM을 호출하지 않는다")
        void notify_withoutToken_onlySaves() {
            Member owner = owner(null);          // 토큰 없음
            Rental rental = rental(owner, renter());

            NotificationService sut = new NotificationService(notificationRepository, memberRepository, fcmService);
            when(notificationRepository.save(any(Notification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            sut.notifyRentRequest(rental);

            verify(notificationRepository, times(1)).save(any(Notification.class));
            verifyNoInteractions(fcmService);   // 전혀 호출되지 않음
        }
    }


    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("알림 소유자가 호출하면 read 플래그를 true 로 변경한다")
        void markAsRead_success() {
            Member me = owner("token");
            Notification n = Notification.builder()
                    .id(10L)
                    .target(me)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t")
                    .body("b")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(notificationRepository.findById(10L)).thenReturn(Optional.of(n));

            NotificationService sut = new NotificationService(notificationRepository, memberRepository, fcmService);

            sut.markAsRead(10L, MemberDto.fromEntity(me, ""));

            assertThat(n.isRead()).isTrue();
        }

        @Test
        @DisplayName("타인의 알림이면 NotificationAccessDenied 예외를 던진다")
        void markAsRead_accessDenied() {
            Member owner = owner("token");
            Member other = renter();

            Notification n = Notification.builder()
                    .id(11L)
                    .target(owner)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t")
                    .body("b")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(notificationRepository.findById(11L)).thenReturn(Optional.of(n));

            NotificationService sut = new NotificationService(notificationRepository, memberRepository, fcmService);

            assertThatThrownBy(() -> sut.markAsRead(11L, MemberDto.fromEntity(other, "")))
                    .isInstanceOf(NotificationAccessDenied.class);
        }
    }


    @Test
    @DisplayName("findByTarget()은 페이지를 DTO로 변환한다")
    void findByTarget_returnsDtoPage() {
        Member me = owner("t");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        Notification n = Notification.builder()
                .id(42L)
                .target(me)
                .type(NotificationType.RENT_REQUESTED)
                .title("t")
                .body("b")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(memberRepository.findById(me.getMemberId())).thenReturn(Optional.of(me));
        when(notificationRepository.findByTarget(me, pageable))
                .thenReturn(new PageImpl<>(List.of(n), pageable, 1));

        NotificationService sut = new NotificationService(notificationRepository, memberRepository, fcmService);

        Page<?> result = sut.findByTarget(MemberDto.fromEntity(me, ""), pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0))
                .hasFieldOrPropertyWithValue("id", 42L)
                .hasFieldOrPropertyWithValue("type", NotificationType.RENT_REQUESTED);
    }
}
