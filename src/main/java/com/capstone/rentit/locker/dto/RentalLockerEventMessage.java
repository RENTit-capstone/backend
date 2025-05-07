package com.capstone.rentit.locker.dto;

import com.capstone.rentit.locker.event.RentalLockerEventType;

public record RentalLockerEventMessage(
        Long lockerId,
        Long rentalId,
        Long memberId,
        RentalLockerEventType type
) { }
