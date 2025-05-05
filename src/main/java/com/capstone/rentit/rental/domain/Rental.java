package com.capstone.rentit.rental.domain;

import com.capstone.rentit.item.domain.Item;
import com.capstone.rentit.member.domain.Member;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_id")
    private Long rentalId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "renter_id", nullable = false)
    private Long renterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private Member ownerMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renter_id", insertable = false, updatable = false)
    private Member renterMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private Item item;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RentalStatusEnum status;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @Column(name = "rejected_date")
    private LocalDateTime rejectedDate;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "retrieved_at")
    private LocalDateTime retrievedAt;

    @Column(name = "locker_id")
    private Long lockerId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "return_image_url")
    private String returnImageUrl; //image object key

    /** 승인 처리 */
    public void approve(LocalDateTime time) {
        this.status = RentalStatusEnum.APPROVED;
        this.approvedDate = time;
    }

    /** 거절 처리 */
    public void reject(LocalDateTime time) {
        this.status = RentalStatusEnum.REJECTED;
        this.rejectedDate = time;
    }

    /** 취소 처리 */
    public void cancel() {
        this.status = RentalStatusEnum.CANCELLED;
    }

    /** 사물함에 물건을 맡긴 상태 (소유자) */
    public void dropOffByOwner(LocalDateTime time) {
        this.status = RentalStatusEnum.LEFT_IN_LOCKER;
        this.leftAt = time;
    }

    /** 물건 픽업 (대여 시작) */
    public void pickUpByRenter(LocalDateTime time) {
        this.status = RentalStatusEnum.PICKED_UP;
        this.pickedUpAt = time;
    }

    /** 사물함에 물건 반환 (대여 반환) */
    public void returnToLocker(LocalDateTime time) {
        this.status = RentalStatusEnum.RETURNED_TO_LOCKER;
        this.returnedAt = time;
    }

    /** 물건 회수 (대여 완료) */
    public void retrieveByOwner(LocalDateTime time) {
        this.status = RentalStatusEnum.COMPLETED;
        this.retrievedAt = time;
    }

    /** 사물함 ID 를 갱신 */
    public void assignLocker(Long lockerId) {
        this.lockerId = lockerId;
    }
    
    /** 사물함 회수 */
    public void clearLocker() {
        this.lockerId = null;
    }

    /** 반납 사진 URL 설정 */
    public void uploadReturnImageUrl(String url) {
        this.returnImageUrl = url;
    }
}
