package com.capstone.rentit.locker.dto;

import com.capstone.rentit.locker.event.RentalLockerAction;

public record LockerDeviceRequest(
        Long deviceId,            // 키오스크 ID
        String otpCode,
        RentalLockerAction action,
        Long rentalId,            // 대여 선택 후
        Long targetLockerId,      // DROP_OFF/RETURN 시 사용자가 고른 칸
        String university         // 빈 사물함 조회용 대학교 필터
) {}
