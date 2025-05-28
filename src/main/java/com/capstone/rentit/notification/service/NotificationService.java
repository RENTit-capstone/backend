package com.capstone.rentit.notification.service;

import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.member.dto.MemberDto;
import com.capstone.rentit.member.repository.MemberRepository;
import com.capstone.rentit.notification.domain.Notification;
import com.capstone.rentit.notification.dto.NotificationDto;
import com.capstone.rentit.notification.exception.NotificationAccessDenied;
import com.capstone.rentit.notification.repository.NotificationRepository;
import com.capstone.rentit.notification.type.NotificationType;
import com.capstone.rentit.rental.domain.Rental;
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
    private final FcmService fcmService;

    public Page<NotificationDto> findByTarget(MemberDto memberDto, Pageable pageable) {
        Member target = memberRepository.findById(memberDto.getMemberId()).orElse(null);
        return notificationRepository.findByTarget(target, pageable)
                .map(NotificationDto::from);
    }

    public void notifyRentRequest(Rental rental){
        notify(
                rental.getOwnerMember(),
                NotificationType.RENT_REQUESTED,
                "새 대여 신청",
                rental.getOwnerMember().getNickname() + "님, " + rental.getItem().getName() + "에 새 대여 신청이 들어왔어요.",
                Map.of("rentalId", rental.getRentalId().toString())
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

    public void notifyItemReturned(Rental rental){
        notify(
                rental.getOwnerMember(),
                NotificationType.ITEM_RETURNED,
                "물품 반납 완료",
                rental.getOwnerMember().getNickname() + "님, " + rental.getItem().getName() + " 물품이 반납 되었어요.\n\n"
                        + "사물함 위치 : " + rental.getLocker().getDevice().getUniversity()
                        + " " + rental.getLocker().getDevice().getLocationDescription()
                        + " " + rental.getLockerId() + "번 사물함",
                Map.of("rentalId", rental.getRentalId().toString())
        );
    }

    public void notifyRequestAccepted(Rental rental){
        notify(
                rental.getRenterMember(),
                NotificationType.REQUEST_ACCEPTED,
                "물품 대여 승인",
                rental.getRenterMember().getNickname() + "님, " + rental.getItem().getName() + "의 대여 신청이 승락되었어요.",
                Map.of("rentalId", rental.getRentalId().toString())
        );
    }

    public void notifyItemPlaced(Rental rental){
        notify(
                rental.getRenterMember(),
                NotificationType.ITEM_PLACED,
                "물품 입고 완료",
                rental.getRenterMember().getNickname() + "님, " + rental.getItem().getName() + " 물품이 사물함으로 들어왔어요.\n\n"
                        + "사물함 위치 : " + rental.getLocker().getDevice().getUniversity()
                        + " " + rental.getLocker().getDevice().getLocationDescription()
                        + " " + rental.getLockerId() + "번 사물함",
                Map.of("rentalId", rental.getRentalId().toString())
        );
    }

    public void notifyRentRejected(Rental rental){
        notify(
                rental.getRenterMember(),
                NotificationType.REQUEST_REJECTED,
                "물품 대여 거부",
                rental.getRenterMember().getNickname() + "님, " + rental.getItem().getName() + "의 대여 신청이 거부되었어요.",
                Map.of("rentalId", rental.getRentalId().toString())
        );
    }

    public void notifyRequestCancel(Rental rental){
        notify(
                rental.getOwnerMember(),
                NotificationType.RENT_CANCEL,
                "물품 대여 취소",
                rental.getOwnerMember().getNickname() + "님, " + rental.getItem().getName() + "의 대여 신청이 취소되었어요.",
                Map.of("rentalId", rental.getRentalId().toString())
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
}