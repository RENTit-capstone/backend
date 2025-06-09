package com.capstone.rentit.locker.dto;

import com.capstone.rentit.locker.event.RentalLockerAction;

public record LockerDeviceRequest(
        Long deviceId,            // 키오스크 ID
        String otpCode,
        RentalLockerAction action,
        Long rentalId            // 대여 선택 후
) {}
