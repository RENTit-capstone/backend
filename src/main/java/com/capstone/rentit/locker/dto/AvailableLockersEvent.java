package com.capstone.rentit.locker.dto;

import java.util.List;

public record AvailableLockersEvent(
        Long deviceId,
        Long rentalId,
        List<LockerBriefResponse> lockers
) implements LockerDeviceEvent { }
