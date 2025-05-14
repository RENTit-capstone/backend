package com.capstone.rentit.locker.dto;

import com.capstone.rentit.locker.event.RentalLockerAction;

public record RentalLockerEventMessage(
        Long deviceId,
        Long lockerId,
        Long rentalId,
        Long memberId,
        RentalLockerAction action,
        Long fee
) { }
