package com.capstone.rentit.rental.dto;

import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RentalDto {
    private Long rentalId;
    private Long itemId;
    private Long ownerId;
    private Long renterId;

    private LocalDateTime requestDate;
    private RentalStatusEnum status;

    private LocalDateTime approvedDate;
    private LocalDateTime rejectedDate;

    private LocalDateTime startDate;    // 예정 대여일
    private LocalDateTime dueDate;      // 예정 반납일

    private LocalDateTime leftAt;       // 소유자 → 사물함 맡김
    private LocalDateTime pickedUpAt;   // 대여자 → 사물함에서 픽업
    private LocalDateTime returnedAt;   // 대여자 → 사물함 맡김
    private LocalDateTime retrievedAt;  // 소유자 → 사물함에서 회수

    private Long lockerId;
    private Long paymentId;

    private String returnImageUrl; //pre-signed Url

    public static RentalDto fromEntity(Rental r, String presignedUrl) {
        return RentalDto.builder()
                .rentalId(r.getRentalId())
                .itemId(r.getItemId())
                .ownerId(r.getOwnerId())
                .renterId(r.getRenterId())
                .requestDate(r.getRequestDate())
                .status(r.getStatus())
                .approvedDate(r.getApprovedDate())
                .rejectedDate(r.getRejectedDate())
                .startDate(r.getStartDate())
                .dueDate(r.getDueDate())
                .leftAt(r.getLeftAt())
                .pickedUpAt(r.getPickedUpAt())
                .returnedAt(r.getReturnedAt())
                .retrievedAt(r.getRetrievedAt())
                .lockerId(r.getLockerId())
                .paymentId(r.getPaymentId())
                .returnImageUrl(presignedUrl)
                .build();
    }
}
