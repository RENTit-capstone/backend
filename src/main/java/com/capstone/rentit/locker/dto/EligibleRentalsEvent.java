package com.capstone.rentit.locker.dto;

import com.capstone.rentit.locker.event.RentalLockerAction;
import com.capstone.rentit.rental.dto.RentalBriefResponseForLocker;

import java.util.List;

public record EligibleRentalsEvent(
        Long deviceId,
        RentalLockerAction action,
        Long memberId,
        String nickname,
        List<RentalBriefResponseForLocker> rentals
) implements LockerDeviceEvent { }
