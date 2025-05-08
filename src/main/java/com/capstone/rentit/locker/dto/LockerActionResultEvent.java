package com.capstone.rentit.locker.dto;

public record LockerActionResultEvent(
        Long deviceId,
        Long lockerId,
        Long rentalId,
        boolean success,
        String message
) implements LockerDeviceEvent { }
