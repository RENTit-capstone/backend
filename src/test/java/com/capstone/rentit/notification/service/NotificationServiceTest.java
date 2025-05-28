package com.capstone.rentit.notification.service;

import com.capstone.rentit.locker.domain.Device;
import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.notification.domain.Notification;
import com.capstone.rentit.notification.dto.NotificationDto;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock MemberRepository memberRepository;
    @Mock FcmService fcmService;

    @Captor ArgumentCaptor<Notification> notificationCaptor;

    // helper: 가짜 Member 생성
    private Member member(long id, String token) {
        Member m = mock(Student.class);
        lenient().when(m.getMemberId()).thenReturn(id);
        lenient().when(m.getFcmToken()).thenReturn(token);
        lenient().when(m.getNickname()).thenReturn("nick" + id);
        return m;
    }

    // helper: 기본 Rental stub (owner, renter, item)
    private Rental basicRental(Member owner, Member renter) {
        Rental r = mock(Rental.class);
        lenient().when(r.getRentalId()).thenReturn(99L);
        lenient().when(r.getOwnerMember()).thenReturn(owner);
        lenient().when(r.getRenterMember()).thenReturn(renter);
        // item 최소 스텁
        var item = com.capstone.rentit.item.domain.Item.builder()
                .itemId(1L)
                .name("TestItem")
                .ownerId(owner.getMemberId())
                .build();
        lenient().when(r.getItem()).thenReturn(item);
        return r;
    }

    // helper: Locker + Device stub
    private void stubLocker(Rental r, String uni, String locDesc, long lockerId) {
        Locker locker = mock(Locker.class);
        Device device = mock(Device.class);
        lenient().when(device.getUniversity()).thenReturn(uni);
        lenient().when(device.getLocationDescription()).thenReturn(locDesc);
        lenient().when(locker.getDevice()).thenReturn(device);
        lenient().when(r.getLocker()).thenReturn(locker);
        lenient().when(r.getLockerId()).thenReturn(lockerId);
    }

    @Nested
    @DisplayName("notifyX() – 다양한 알림 전송")
    class NotifyMethods {

        @Test
        @DisplayName("notifyRentRequest: owner에게 RENT_REQUESTED 알림 저장 및 FCM 전송")
        void rentRequest_sendsToOwner() {
            Member owner = member(1, "tokenA");
            Rental r = basicRental(owner, member(2, null));
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            svc.notifyRentRequest(r);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getTarget()).isEqualTo(owner);
            assertThat(n.getType()).isEqualTo(NotificationType.RENT_REQUESTED);
            assertThat(n.getTitle()).isEqualTo("새 대여 신청");
            assertThat(n.getBody()).contains("새 대여 신청이 들어왔어요").contains("nick1");
            assertThat(n.isRead()).isFalse();
            assertThat(n.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());

            verify(fcmService).sendToToken(eq("tokenA"), eq(n.getTitle()), eq(n.getBody()), anyMap());
        }

        @Test
        @DisplayName("notifyRequestAccepted: renter에게 REQUEST_ACCEPTED 알림")
        void requestAccepted_sendsToRenter() {
            Member owner  = member(1, null);
            Member renter = member(2, "tokenB");
            Rental r = basicRental(owner, renter);
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            svc.notifyRequestAccepted(r);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getTarget()).isEqualTo(renter);
            assertThat(n.getType()).isEqualTo(NotificationType.REQUEST_ACCEPTED);
            assertThat(n.getTitle()).isEqualTo("물품 대여 승인");
            assertThat(n.getBody()).contains("nick2").contains("TestItem");

            verify(fcmService).sendToToken(eq("tokenB"), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("notifyRentRejected: renter에게 REQUEST_REJECTED 알림")
        void rentRejected_sendsToRenter() {
            Member owner  = member(1, null);
            Member renter = member(2, "tokenC");
            Rental r = basicRental(owner, renter);
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            svc.notifyRentRejected(r);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.REQUEST_REJECTED);
            verify(fcmService).sendToToken(eq("tokenC"), eq("물품 대여 거부"), anyString(), anyMap());
        }

        @Test
        @DisplayName("notifyRequestCancel: owner에게 RENT_CANCEL 알림")
        void requestCancel_sendsToOwner() {
            Member owner = member(1, "tokenD");
            Rental r = basicRental(owner, member(2, null));
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            svc.notifyRequestCancel(r);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.RENT_CANCEL);
            verify(fcmService).sendToToken(eq("tokenD"), eq("물품 대여 취소"), anyString(), anyMap());
        }

        @Test
        @DisplayName("notifyItemPlaced: renter에게 ITEM_PLACED 알림 (락커 위치 포함)")
        void itemPlaced_includesLockerInfo() {
            Member owner  = member(1, null);
            Member renter = member(2, "tokenE");
            Rental r = basicRental(owner, renter);
            stubLocker(r, "UniX", "Floor1", 5L);
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            svc.notifyItemPlaced(r);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.ITEM_PLACED);
            assertThat(n.getBody()).contains("UniX").contains("Floor1").contains("5번 사물함");
            verify(fcmService).sendToToken(eq("tokenE"), eq("물품 입고 완료"), anyString(), anyMap());
        }

        @Test
        @DisplayName("notifyItemReturned: owner에게 ITEM_RETURNED 알림 (락커 위치 포함)")
        void itemReturned_includesLockerInfo() {
            Member owner = member(1, "tokenF");
            Member renter = member(2, null);
            Rental r = basicRental(owner, renter);
            stubLocker(r, "UniY", "B1", 7L);
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            svc.notifyItemReturned(r);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.ITEM_RETURNED);
            assertThat(n.getBody()).contains("UniY").contains("B1").contains("7번 사물함");
            verify(fcmService).sendToToken(eq("tokenF"), eq("물품 반납 완료"), anyString(), anyMap());
        }

        @Test
        @DisplayName("토큰이 없으면 FCM을 호출하지 않는다")
        void withoutToken_onlySaves() {
            Member owner = member(1, null);
            Rental r = basicRental(owner, member(2, null));
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            svc.notifyRentRequest(r);

            verify(notificationRepository, times(1)).save(any());
            verifyNoInteractions(fcmService);
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("소유자 호출 시 읽음 처리")
        void markAsRead_success() {
            Member me = member(1, null);
            Notification noti = Notification.builder()
                    .id(10L)
                    .target(me)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t").body("b")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            when(notificationRepository.findById(10L)).thenReturn(Optional.of(noti));
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);

            svc.markAsRead(10L, MemberDto.fromEntity(me, ""));

            assertThat(noti.isRead()).isTrue();
        }

        @Test
        @DisplayName("타인 알림이면 예외 발생")
        void markAsRead_accessDenied() {
            Member owner = member(1, null);
            Member other = member(2, null);
            Notification noti = Notification.builder()
                    .id(11L).target(owner)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t").body("b")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            when(notificationRepository.findById(11L)).thenReturn(Optional.of(noti));
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);

            assertThatThrownBy(() ->
                    svc.markAsRead(11L, MemberDto.fromEntity(other, ""))
            ).isInstanceOf(NotificationAccessDenied.class);
        }

        @Test
        @DisplayName("이미 읽힌 알림은 updateRead 미호출")
        void markAsRead_alreadyRead_noUpdate() {
            Member me = member(1, null);
            Notification noti = spy(Notification.builder()
                    .id(12L).target(me)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t").body("b")
                    .isRead(true)
                    .createdAt(LocalDateTime.now())
                    .build());
            when(notificationRepository.findById(12L)).thenReturn(Optional.of(noti));
            NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);

            svc.markAsRead(12L, MemberDto.fromEntity(me, ""));

            verify(noti, never()).updateRead(true);
        }
    }

    @Test
    @DisplayName("findByTarget(): 페이지를 DTO로 변환")
    void findByTarget_returnsDtoPage() {
        Member me = member(1, null);
        Pageable page = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Notification n = Notification.builder()
                .id(42L).target(me)
                .type(NotificationType.RENT_REQUESTED)
                .title("t").body("b")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(me));
        when(notificationRepository.findByTarget(me, page)).thenReturn(
                new PageImpl<>(List.of(n), page, 1)
        );
        NotificationService svc = new NotificationService(notificationRepository, memberRepository, fcmService);

        Page<NotificationDto> result = svc.findByTarget(MemberDto.fromEntity(me, ""), page);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0))
                .hasFieldOrPropertyWithValue("id", 42L)
                .hasFieldOrPropertyWithValue("type", NotificationType.RENT_REQUESTED);
    }
}
