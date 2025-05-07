package com.capstone.rentit.locker.dto;

import com.capstone.rentit.locker.event.RentalLockerAction;

public record LockerDeviceRequest(
        Long lockerId,
        String email,
        String otpCode,
        RentalLockerAction action,
        Long rentalId,

        /* DROP_OFF/RETURN: 사용자가 고른 빈 사물함.
           PICK_UP/RETRIEVE: null → 서버가 rental.lockerId 사용 */
        Long targetLockerId,

        String university
) {}
