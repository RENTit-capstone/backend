package com.capstone.rentit.notification.service;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.member.domain.Member;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final RentalRepository rentalRepository;
    private final FcmService fcmService;

    public Page<NotificationDto> findByTarget(MemberDto memberDto, Pageable pageable) {
        Member target = memberRepository.findById(memberDto.getMemberId()).orElse(null);
        return notificationRepository.findByTarget(target, pageable)
                .map(NotificationDto::from);
    }

    public void notifyRentRequest(Long rentalId){
        Rental rental = findRental(rentalId);
        Member owner = findMember(rental.getOwnerId());
        notify(
                owner,
                NotificationType.RENT_REQUESTED,
                "새 대여 신청",
                owner.getNickname() + "님, " + rental.getItem().getName() + "에 새 대여 신청이 들어왔어요.",
                Map.of("rentalId", rentalId.toString())
        );
    }

    public void markAsRead(Long notificationId, MemberDto me) {
        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationAccessDenied("알림이 존재하지 않습니다."));

        if (!noti.getTarget().getMemberId().equals(me.getMemberId())) {
            throw new NotificationAccessDenied("본인의 알림만 읽음 처리할 수 있습니다.");
        }

        if (!noti.isRead()) {
            noti.updateRead(true);
        }
    }

    public void notifyItemReturned(Long rentalId){
        Rental rental = findRental(rentalId);
        Member owner = findMember(rental.getOwnerId());
        notify(
                owner,
                NotificationType.ITEM_RETURNED,
                "물품 반납 완료",
                owner.getNickname() + "님, " + rental.getItem().getName() + " 물품이 반납 되었어요.\n\n"
                        + "사물함 위치 : " + rental.getLocker().getDevice().getUniversity()
                        + " " + rental.getLocker().getDevice().getLocationDescription()
                        + " " + rental.getLockerId() + "번 사물함",
                Map.of("rentalId", rentalId.toString())
        );
    }

    public void notifyRequestAccepted(Long rentalId){
        Rental rental = findRental(rentalId);
        Member renter = findMember(rental.getRenterId());
        notify(
                renter,
                NotificationType.REQUEST_ACCEPTED,
                "물품 대여 승인",
                rental.getRenterMember().getNickname() + "님, " + rental.getItem().getName() + "의 대여 신청이 승락되었어요.",
                Map.of("rentalId", rentalId.toString())
        );
    }

    public void notifyItemPlaced(Long rentalId){
        Rental rental = findRental(rentalId);
        Member renter = findMember(rental.getRenterId());
        notify(
                renter,
                NotificationType.ITEM_PLACED,
                "물품 입고 완료",
                renter.getNickname() + "님, " + rental.getItem().getName() + " 물품이 사물함으로 들어왔어요.\n\n"
                        + "사물함 위치 : " + rental.getLocker().getDevice().getUniversity()
                        + " " + rental.getLocker().getDevice().getLocationDescription()
                        + " " + rental.getLockerId() + "번 사물함",
                Map.of("rentalId", rentalId.toString())
        );
    }

    public void notifyRentRejected(Long rentalId){
        Rental rental = findRental(rentalId);
        Member renter = findMember(rental.getRenterId());
        notify(
                renter,
                NotificationType.REQUEST_REJECTED,
                "물품 대여 거부",
                renter.getNickname() + "님, " + rental.getItem().getName() + "의 대여 신청이 거부되었어요.",
                Map.of("rentalId", rentalId.toString())
        );
    }

    public void notifyRequestCancel(Long rentalId){
        Rental rental = findRental(rentalId);
        Member owner = findMember(rental.getOwnerId());
        notify(
                owner,
                NotificationType.RENT_CANCEL,
                "물품 대여 취소",
                owner.getNickname() + "님, " + rental.getItem().getName() + "의 대여 신청이 취소되었어요.",
                Map.of("rentalId", rentalId.toString())
        );
    }

    public void notifyItemDamagedRequest(Long rentalId){
        Rental rental = findRental(rentalId);
        Member owner = findMember(rental.getOwnerId());
        notify(
                owner,
                NotificationType.ITEM_DAMAGED_REQUEST,
                "물품 파손 신고",
                owner.getNickname() + "님, " + rental.getItem().getName() + "의 파손 신고가 들어왔어요.",
                Map.of("rentalId", rentalId.toString())
        );
    }

    public void notifyItemDamagedResponse(Inquiry inquiry){
        Member renter = findMember(inquiry.getMemberId());
        notify(
                renter,
                NotificationType.ITEM_DAMAGED_RESPONSE,
                "물품 파손 신고",
                renter.getNickname() + "님, " + inquiry.getTitle() + "의 파손 신고 응답이 도착했어요.",
                Map.of("inquiryId", inquiry.getInquiryId().toString())
        );
    }

    public void notifyInquiryResponse(Inquiry inquiry){
        Member member = findMember(inquiry.getMemberId());
        notify(
                member,
                NotificationType.INQUIRY_RESPONSE,
                "문의 처리 완료",
                member.getNickname() + "님, " + inquiry.getTitle() + "의 문의가 처리되었어요.",
                Map.of("inquiryId", inquiry.getInquiryId().toString())
        );
    }

    public void notify(Member target,
                       NotificationType type,
                       String title,
                       String body,
                       Map<String, String> data) {

        // 1) DB 저장
        Notification noti = Notification.builder()
                .target(target)
                .type(type)
                .title(title)
                .body(body)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(noti);

        // 2) FCM 전송
        if (target.getFcmToken() != null) {
            fcmService.sendToToken(target.getFcmToken(), title, body, data);
        }
    }

    private Member findMember(Long memberId){
        return memberRepository.findById(memberId).orElseThrow(() -> new MemberNotFoundException("존재하지 않는 사용자입니다."));
    }

    private Rental findRental(Long rentalId){
        return rentalRepository.findById(rentalId).orElseThrow(() -> new RentalNotFoundException("존재하지 않는 대여 정보입니다."));
    }
}