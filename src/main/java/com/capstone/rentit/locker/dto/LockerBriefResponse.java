package com.capstone.rentit.locker.dto;

public record LockerBriefResponse(
        Long deviceId,
        Long lockerId,
        boolean available
) { }
