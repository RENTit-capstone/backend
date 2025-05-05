package com.capstone.rentit.rental.dto;

import com.capstone.rentit.rental.domain.Rental;
import com.capstone.rentit.rental.status.RentalStatusEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentalBriefResponse {

    private Long rentalId;
    private RentalStatusEnum status;
    private LocalDateTime requestDate;
    private LocalDateTime startDate;
    private LocalDateTime dueDate;
    private LocalDateTime approvedDate;
    private LocalDateTime rejectedDate;
    private LocalDateTime leftAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime returnedAt;
    private LocalDateTime retrievedAt;
    private String thumbnailUrl;

    private boolean isOwner;       // true: 내가 소유자, false: 내가 대여자

    public static RentalBriefResponse fromEntity(Rental rental, String presignedUrl, boolean asOwner) {
        return RentalBriefResponse.builder()
                .rentalId(rental.getRentalId())
                .status(rental.getStatus())
                .requestDate(rental.getRequestDate())
                .startDate(rental.getStartDate())
                .dueDate(rental.getDueDate())
                .approvedDate(rental.getApprovedDate())
                .rejectedDate(rental.getRejectedDate())
                .leftAt(rental.getLeftAt())
                .pickedUpAt(rental.getPickedUpAt())
                .returnedAt(rental.getReturnedAt())
                .retrievedAt(rental.getRetrievedAt())
                .thumbnailUrl(presignedUrl)
                .isOwner(asOwner)
                .build();
    }
}
