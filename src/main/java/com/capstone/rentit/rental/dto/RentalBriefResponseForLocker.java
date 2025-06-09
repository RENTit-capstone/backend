package com.capstone.rentit.rental.dto;

import com.capstone.rentit.rental.domain.Rental;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RentalBriefResponseForLocker {
    Long rentalId;
    Long itemId;
    String itemName;
    Long lockerId;
    long fee;
    long balance;
    boolean payable;

    public static RentalBriefResponseForLocker fromEntity(
            Rental entity, long fee, long balance) {
        return new RentalBriefResponseForLocker(
                entity.getRentalId(),
                entity.getItemId(),
                entity.getItem().getName(),
                entity.getLockerId(),
                fee,
                balance,
                balance >= fee
        );
    }
}
