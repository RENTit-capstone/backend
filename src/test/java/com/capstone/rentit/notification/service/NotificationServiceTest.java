package com.capstone.rentit.notification.service;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.locker.domain.Device;
import com.capstone.rentit.locker.repository.DeviceRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private MemberRepository       memberRepository;
    @Mock private RentalRepository       rentalRepository;
    @Mock private DeviceRepository       deviceRepository;
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
        when(r.getOwnerId()).thenReturn(ownerId);
        when(r.getRenterId()).thenReturn(renterId);
        var item = com.capstone.rentit.item.domain.Item.builder()
                .itemId(1L).name("TestItem").ownerId(ownerId).build();
        when(r.getItem()).thenReturn(item);
        return r;
    }

    // 헬퍼: Device stub
    private void stubDevice(long deviceId, String university, String locationDesc) {
        Device device = mock(Device.class);
        when(device.getUniversity()).thenReturn(university);
        when(device.getLocationDescription()).thenReturn(locationDesc);
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
    }


    @Nested @DisplayName("notifyRentRequest")
    class RentRequest {
        @Test @DisplayName("정상 호출")
        void success() {
            // given
            long rentalId = 1L, ownerId = 10L;
            Rental rental = stubRental(rentalId, ownerId, 20L);
            Member owner = member(ownerId, "tokenA");
            when(rentalRepository.findByIdWithItem(rentalId)).thenReturn(Optional.of(rental));
            when(memberRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.notifyRentRequest(rentalId);

            // then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.RENT_REQUESTED);
            assertThat(n.getBody()).contains("nick10", "TestItem");

            verify(fcmService).sendToToken(
                    eq("tokenA"),
                    eq(n.getTitle()),
                    eq(n.getBody()),
                    anyMap()
            );
        }

        @Test @DisplayName("없는 대여 -> 예외")
        void notFound() {
            // given
            when(rentalRepository.findByIdWithItem(99L)).thenReturn(Optional.empty());
            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when & then
            assertThatThrownBy(() -> svc.notifyRentRequest(99L))
                    .isInstanceOf(RentalNotFoundException.class);
        }
    }

    @Nested @DisplayName("notifyItemReturned")
    class ItemReturned {
        @Test @DisplayName("정상 호출")
        void success() {
            // given
            long rentalId = 2L, ownerId = 11L, deviceId = 3L, lockerId = 5L;
            Rental rental = stubRental(rentalId, ownerId, 21L);
            Member owner = member(ownerId, "tokenB");

            stubDevice(deviceId, "UniX", "Floor2");
            when(rentalRepository.findByIdWithItem(rentalId)).thenReturn(Optional.of(rental));
            when(memberRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.notifyItemReturned(rentalId, deviceId, lockerId);

            // then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.ITEM_RETURNED);
            assertThat(n.getBody()).contains("UniX", "Floor2", "5번 사물함");

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
            // given
            long rid = 3, renterId = 22;
            Rental r = stubRental(rid, 12, renterId); // Rental Mock 객체 생성
            Member renterMember = member(renterId, "tokenC"); // Member Mock 객체 생성

            when(r.getRenterMember()).thenReturn(renterMember);

            when(rentalRepository.findByIdWithItem(rid)).thenReturn(Optional.of(r));
            when(memberRepository.findById(renterId)).thenReturn(Optional.of(renterMember));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.notifyRequestAccepted(rid);

            // then
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
            // given
            long rentalId = 4L, renterId = 23L, deviceId = 4L, lockerId = 7L;
            Rental rental = stubRental(rentalId, 13L, renterId);
            Member renter = member(renterId, "tokenD");

            stubDevice(deviceId, "UniY", "B1");
            when(rentalRepository.findByIdWithItem(rentalId)).thenReturn(Optional.of(rental));
            when(memberRepository.findById(renterId)).thenReturn(Optional.of(renter));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.notifyItemPlaced(rentalId, deviceId, lockerId);

            // then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getType()).isEqualTo(NotificationType.ITEM_PLACED);
            assertThat(n.getBody()).contains("UniY", "B1", "7번 사물함");

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
            // given
            long rentalId = 5L, renterId = 24L;
            Rental rental = stubRental(rentalId, 14L, renterId);
            Member renter = member(renterId, "tokenE");
            when(rentalRepository.findByIdWithItem(rentalId)).thenReturn(Optional.of(rental));
            when(memberRepository.findById(renterId)).thenReturn(Optional.of(renter));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.notifyRentRejected(rentalId);

            // then
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
            // given
            long rentalId = 6L, ownerId = 15L;
            Rental rental = stubRental(rentalId, ownerId, 25L);
            Member owner = member(ownerId, "tokenF");
            when(rentalRepository.findByIdWithItem(rentalId)).thenReturn(Optional.of(rental));
            when(memberRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.notifyRequestCancel(rentalId);

            // then
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

    @Nested @DisplayName("ItemDamaged (Request/Response)")
    class Damaged {
        @Test @DisplayName("notifyItemDamagedRequest 정상 호출")
        void request() {
            // given
            long rentalId = 7L, ownerId = 16L;
            Rental rental = stubRental(rentalId, ownerId, 26L);
            Member owner = member(ownerId, "tokenG");
            when(rentalRepository.findByIdWithItem(rentalId)).thenReturn(Optional.of(rental));
            when(memberRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.notifyItemDamagedRequest(rentalId);

            // then
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

        @Test @DisplayName("notifyItemDamagedResponse 정상 호출")
        void response() {
            // given
            long memberId = 42L, inquiryId = 7L;
            String title = "파손문의";
            Inquiry inquiry = mock(Inquiry.class);
            when(inquiry.getMemberId()).thenReturn(memberId);
            when(inquiry.getInquiryId()).thenReturn(inquiryId);
            when(inquiry.getTitle()).thenReturn(title);

            Member renter = member(memberId, "tokenX");
            when(memberRepository.findById(memberId)).thenReturn(Optional.of(renter));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.notifyItemDamagedResponse(inquiry);

            // then – DB 저장 검증
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification n = notificationCaptor.getValue();
            assertThat(n.getTarget()).isEqualTo(renter);
            assertThat(n.getType()).isEqualTo(NotificationType.ITEM_DAMAGED_RESPONSE);
            assertThat(n.getTitle()).isEqualTo("물품 파손 신고");
            assertThat(n.getBody()).contains("nick42", title);

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
            // given
            long memberId = 30L, inquiryId = 99L;
            Inquiry inquiry = mock(Inquiry.class);
            when(inquiry.getMemberId()).thenReturn(memberId);
            when(inquiry.getInquiryId()).thenReturn(inquiryId);
            when(inquiry.getTitle()).thenReturn("Question");

            Member m = member(memberId, "tokenI");
            when(memberRepository.findById(memberId)).thenReturn(Optional.of(m));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.notifyInquiryResponse(inquiry);

            // then
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
            // given
            Inquiry inquiry = mock(Inquiry.class);
            when(inquiry.getMemberId()).thenReturn(999L);
            when(memberRepository.findById(999L)).thenReturn(Optional.empty());

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when & then
            assertThatThrownBy(() -> svc.notifyInquiryResponse(inquiry))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    @Nested @DisplayName("markAsRead / findByTarget")
    class Common {
        @Test @DisplayName("markAsRead: 성공")
        void markSuccess() {
            // given
            Member me = member(40L, null);
            MemberDto memberDto = MemberDto.fromEntity(me, "");
            Notification n = Notification.builder()
                    .id(123L).target(me)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t").body("b")
                    .isRead(false).createdAt(LocalDateTime.now()).build();
            when(notificationRepository.findById(123L)).thenReturn(Optional.of(n));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            svc.markAsRead(123L, memberDto);

            // then
            assertThat(n.isRead()).isTrue();
        }

        @Test @DisplayName("markAsRead: 타인 접근 예외")
        void markDenied() {
            // given
            Member owner = member(41L, null);
            Member other = member(42L, null);
            MemberDto otherDto = MemberDto.fromEntity(other, "");

            Notification n = Notification.builder()
                    .id(124L).target(owner)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t").body("b")
                    .isRead(false).createdAt(LocalDateTime.now()).build();
            when(notificationRepository.findById(124L)).thenReturn(Optional.of(n));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when & then
            assertThatThrownBy(() -> svc.markAsRead(124L, otherDto))
                    .isInstanceOf(NotificationAccessDenied.class);
        }

        @Test @DisplayName("findByTarget: DTO 페이지 반환")
        void findByTarget() {
            // given
            Member me = member(43L, null);
            MemberDto memberDto = MemberDto.fromEntity(me, "");
            Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
            Notification n = Notification.builder()
                    .id(200L).target(me)
                    .type(NotificationType.RENT_REQUESTED)
                    .title("t").body("b")
                    .isRead(false).createdAt(LocalDateTime.now()).build();

            when(memberRepository.findById(43L)).thenReturn(Optional.of(me));
            when(notificationRepository.findByTarget(me, pageable))
                    .thenReturn(new PageImpl<>(List.of(n), pageable, 1));

            var svc = new NotificationService(notificationRepository, memberRepository, rentalRepository, deviceRepository, fcmService);

            // when
            Page<NotificationDto> result = svc.findByTarget(memberDto, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(200L);
        }
    }
}