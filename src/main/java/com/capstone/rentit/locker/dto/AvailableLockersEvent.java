package com.capstone.rentit.locker.dto;

import java.util.List;

public record AvailableLockersEvent(
        Long lockerId,
        Long rentalId,
        List<LockerDto> lockers
) implements LockerDeviceEvent { }
