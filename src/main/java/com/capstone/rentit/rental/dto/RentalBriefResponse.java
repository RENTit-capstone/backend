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
    private String itemName;
    private String ownerName;
    private String renterName;
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
    private String lockerUniversity;
    private String lockerLocation;
    private Long lockerNumber;

    private boolean isOwner;       // true: 내가 소유자, false: 내가 대여자

    public static RentalBriefResponse fromEntity(Rental rental, String presignedUrl, boolean asOwner) {
        String univ = null;
        String location = null;
        Long lockerNum = null;
        if(rental.getRentalId() != null && rental.getLockerId() != null){
            univ = rental.getLocker().getDevice().getUniversity();
            location = rental.getLocker().getDevice().getLocationDescription();
            lockerNum = rental.getLockerId();
        }
        return RentalBriefResponse.builder()
                .rentalId(rental.getRentalId())
                .itemName(rental.getItem().getName())
                .ownerName(rental.getOwnerMember().getNickname())
                .renterName(rental.getRenterMember().getNickname())
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
                .lockerUniversity(univ)
                .lockerLocation(location)
                .lockerNumber(lockerNum)
                .isOwner(asOwner)
                .build();
    }
}
