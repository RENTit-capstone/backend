package com.capstone.rentit.notification.service;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.locker.domain.Device;
import com.capstone.rentit.locker.domain.Locker;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.domain.Student;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.exception.MemberNotFoundException;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.notification.domain.Notification;
import com.capstone.rentit.notification.dto.NotificationDto;
import com.capstone.rentit.notification.exception.NotificationAccessDenied;
import com.capstone.rentit.notification.repository.NotificationRepository;
import com.capstone.rentit.notification.type.NotificationType;
import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.exception.RentalNotFoundException;
import com.capstone.rentit.rental.repository.RentalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private MemberRepository       memberRepository;
    @Mock private RentalRepository       rentalRepository;
    @Mock private FcmService             fcmService;
    @Captor private ArgumentCaptor<Notification> notificationCaptor;

    // 헬퍼: Member stub
    private Member member(long id, String token) {
        Member m = mock(Student.class);
        when(m.getMemberId()).thenReturn(id);
        when(m.getFcmToken()).thenReturn(token);
        when(m.getNickname()).thenReturn("nick" + id);
        return m;
    }

    // 헬퍼: 기본 Rental stub (item만)
    private Rental stubRental(long rentalId, long ownerId, long renterId) {
        Rental r = mock(Rental.class);
        when(r.getRentalId()).thenReturn(rentalId);
        when(r.getOwnerId()).thenReturn(ownerId);
        when(r.getRenterId()).thenReturn(renterId);
        var item = com.capstone.rentit.item.domain.Item.builder()
                .itemId(1L).name("TestItem").ownerId(ownerId).build();
        when(r.getItem()).thenReturn(item);
        return r;
    }

    // 헬퍼: Locker 및 Device stub
    private void stubLocker(Rental r, String uni, String desc, long lockerId) {
        Locker locker = mock(Locker.class);
        Device device = mock(Device.class);
        when(device.getUniversity()).thenReturn(uni);
        when(device.getLocationDescription()).thenReturn(desc);
        when(locker.getDevice()).thenReturn(device);
        when(r.getLocker()).thenReturn(locker);
        when(r.getLockerId()).thenReturn(lockerId);
    }

    @Nested @DisplayName("notifyRentRequest")
    class RentRequest {
        @Test @DisplayName("정상 호출")
        void success() {
            long rid = 1, ownerId = 10;
            Rental r = stubRental(rid, ownerId, 20);
            Member owner = member(ownerId, "tokenA");
            when(rentalRepository.findByIdWithItem(rid)).thenReturn(Optional.of(r));
            when(memberRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            svc.notifyRentRequest(rid);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.RENT_REQUESTED);
            assertThat(n.getBody()).contains("nick10").contains("TestItem");

            verify(fcmService).sendToToken(
                    eq("tokenA"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    anyMap()
            );
        }

        @Test @DisplayName("없는 대여 -> 예외")
        void notFound() {
            when(rentalRepository.findById(99L)).thenReturn(Optional.empty());
            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            assertThatThrownBy(() -> svc.notifyRentRequest(99L))
                    .isInstanceOf(RentalNotFoundException.class);
        }
    }

    @Nested @DisplayName("notifyItemReturned")
    class ItemReturned {
        @Test @DisplayName("정상 호출")
        void success() {
            long rid = 2, ownerId = 11;
            Rental r = stubRental(rid, ownerId, 21);
            stubLocker(r, "UniX", "Floor2", 5);
            Member owner = member(ownerId, "tokenB");
            when(rentalRepository.findByIdWithItem(rid)).thenReturn(Optional.of(r));
            when(memberRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            svc.notifyItemReturned(rid);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.ITEM_RETURNED);
            assertThat(n.getBody()).contains("UniX").contains("Floor2").contains("5번 사물함");

            verify(fcmService).sendToToken(
                    eq("tokenB"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    anyMap()
            );
        }
    }

    @Nested @DisplayName("notifyRequestAccepted")
    class RequestAccepted {
        @Test @DisplayName("정상 호출")
        void success() {
            long rid = 3, renterId = 22;
            Rental r = stubRental(rid, 12, renterId);
            Member renterMember = member(renterId, "tokenC");
            when(r.getRenterMember()).thenReturn(renterMember);
            when(rentalRepository.findByIdWithItem(rid)).thenReturn(Optional.of(r));
            when(memberRepository.findById(renterId)).thenReturn(Optional.of(renterMember));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            svc.notifyRequestAccepted(rid);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.REQUEST_ACCEPTED);

            verify(fcmService).sendToToken(
                    eq("tokenC"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    anyMap()
            );
        }
    }

    @Nested @DisplayName("notifyItemPlaced")
    class ItemPlaced {
        @Test @DisplayName("정상 호출")
        void success() {
            long rid = 4, renterId = 23;
            Rental r = stubRental(rid, 13, renterId);
            stubLocker(r, "UniY", "B1", 7);
            Member renter = member(renterId, "tokenD");
            when(rentalRepository.findByIdWithItem(rid)).thenReturn(Optional.of(r));
            when(memberRepository.findById(renterId)).thenReturn(Optional.of(renter));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            svc.notifyItemPlaced(rid);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.ITEM_PLACED);
            assertThat(n.getBody()).contains("UniY").contains("B1").contains("7번 사물함");

            verify(fcmService).sendToToken(
                    eq("tokenD"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    anyMap()
            );
        }
    }

    @Nested @DisplayName("notifyRentRejected")
    class RentRejected {
        @Test @DisplayName("정상 호출")
        void success() {
            long rid = 5, renterId = 24;
            Rental r = stubRental(rid, 14, renterId);
            Member renter = member(renterId, "tokenE");
            when(rentalRepository.findByIdWithItem(rid)).thenReturn(Optional.of(r));
            when(memberRepository.findById(renterId)).thenReturn(Optional.of(renter));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            svc.notifyRentRejected(rid);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.REQUEST_REJECTED);

            verify(fcmService).sendToToken(
                    eq("tokenE"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    anyMap()
            );
        }
    }

    @Nested @DisplayName("notifyRequestCancel")
    class RequestCancel {
        @Test @DisplayName("정상 호출")
        void success() {
            long rid = 6, ownerId = 15;
            Rental r = stubRental(rid, ownerId, 25);
            Member owner = member(ownerId, "tokenF");
            when(rentalRepository.findByIdWithItem(rid)).thenReturn(Optional.of(r));
            when(memberRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            svc.notifyRequestCancel(rid);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.RENT_CANCEL);

            verify(fcmService).sendToToken(
                    eq("tokenF"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    anyMap()
            );
        }
    }

    @Nested @DisplayName("notifyItemDamagedRequest / notifyItemDamagedResponse")
    class Damaged {
        @Test
        @DisplayName("notifyItemDamagedRequest 정상 호출")
        void request() {
            long rid = 7, ownerId = 16;
            Rental r = stubRental(rid, ownerId, 26);
            Member owner = member(ownerId, "tokenG");
            when(rentalRepository.findByIdWithItem(rid)).thenReturn(Optional.of(r));
            when(memberRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            svc.notifyItemDamagedRequest(rid);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.ITEM_DAMAGED_REQUEST);

            verify(fcmService).sendToToken(
                    eq("tokenG"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    anyMap()
            );
        }

        @Test
        @DisplayName("notifyItemDamagedResponse 정상 호출")
        void response() {
            // given
            long memberId = 42L;
            long inquiryId = 7L;
            String title = "파손문의";
            Inquiry inq = mock(Inquiry.class);
            when(inq.getMemberId()).thenReturn(memberId);
            when(inq.getInquiryId()).thenReturn(inquiryId);
            when(inq.getTitle()).thenReturn(title);

            Member renter = member(memberId, "tokenX");
            when(memberRepository.findById(memberId)).thenReturn(Optional.of(renter));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            NotificationService svc = new NotificationService(
                    notificationRepository, memberRepository, rentalRepository, fcmService
            );

            // when
            svc.notifyItemDamagedResponse(inq);

            // then – DB 저장 검증
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getTarget()).isEqualTo(renter);
            assertThat(n.getType()).isEqualTo(NotificationType.ITEM_DAMAGED_RESPONSE);
            assertThat(n.getTitle()).isEqualTo("물품 파손 신고");
            assertThat(n.getBody()).contains("nick42").contains(title);

            // then – FCM 전송 검증
            verify(fcmService).sendToToken(
                    eq("tokenX"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    argThat(map -> "7".equals(map.get("inquiryId")))
            );
        }
    }

    @Nested @DisplayName("notifyInquiryResponse")
    class InquiryResponse {
        @Test @DisplayName("정상 호출")
        void success() {
            long mid = 30, inqId = 99;
            Inquiry i = mock(Inquiry.class);
            when(i.getMemberId()).thenReturn(mid);
            when(i.getInquiryId()).thenReturn(inqId);
            when(i.getTitle()).thenReturn("Question");

            Member m = member(mid, "tokenI");
            when(memberRepository.findById(mid)).thenReturn(Optional.of(m));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            svc.notifyInquiryResponse(i);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.INQUIRY_RESPONSE);
            assertThat(n.getBody()).contains("Question");

            verify(fcmService).sendToToken(
                    eq("tokenI"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    anyMap()
            );
        }

        @Test @DisplayName("없는 회원 -> 예외")
        void memberNotFound() {
            Inquiry i = mock(Inquiry.class);
            when(i.getMemberId()).thenReturn(999L);
            when(memberRepository.findById(999L)).thenReturn(Optional.empty());

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            assertThatThrownBy(() -> svc.notifyInquiryResponse(i))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    @Nested @DisplayName("markAsRead / findByTarget")
    class Common {
        @Test @DisplayName("markAsRead: 성공")
        void markSuccess() {
            Member me = member(40L, null);
            Notification n = Notification.builder()
                    .id(123L).target(me)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t").body("b")
                    .isRead(false).createdAt(LocalDateTime.now()).build();
            when(notificationRepository.findById(123L)).thenReturn(Optional.of(n));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            svc.markAsRead(123L, MemberDto.fromEntity(me, ""));
            assertThat(n.isRead()).isTrue();
        }

        @Test @DisplayName("markAsRead: 타인 접근 예외")
        void markDenied() {
            Member owner = member(41L, null);
            Member other = member(42L, null);
            Notification n = Notification.builder()
                    .id(124L).target(owner)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t").body("b")
                    .isRead(false).createdAt(LocalDateTime.now()).build();
            when(notificationRepository.findById(124L)).thenReturn(Optional.of(n));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            assertThatThrownBy(() ->
                    svc.markAsRead(124L, MemberDto.fromEntity(other, ""))
            ).isInstanceOf(NotificationAccessDenied.class);
        }

        @Test @DisplayName("findByTarget: DTO 페이지 반환")
        void findByTarget() {
            Member me = member(43L, null);
            Pageable page = PageRequest.of(0, 5, Sort.by("createdAt").descending());
            Notification n = Notification.builder()
                    .id(200L).target(me)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t").body("b")
                    .isRead(false).createdAt(LocalDateTime.now()).build();
            when(memberRepository.findById(43L)).thenReturn(Optional.of(me));
            when(notificationRepository.findByTarget(me, page))
                    .thenReturn(new PageImpl<>(List.of(n), page, 1));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, fcmService);
            Page<NotificationDto> result = svc.findByTarget(MemberDto.fromEntity(me, ""), page);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(200L);
        }
    }
}