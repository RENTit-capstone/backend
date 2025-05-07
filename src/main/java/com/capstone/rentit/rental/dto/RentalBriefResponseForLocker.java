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

    public static RentalBriefResponseForLocker fromEntity(Rental entity) {
        return new RentalBriefResponseForLocker(
                entity.getRentalId(),
                entity.getItemId(),
                entity.getItem().getName(),
                entity.getLockerId()
        );
    }
}
