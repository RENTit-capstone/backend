package com.capstone.rentit.locker.dto;

public sealed interface LockerDeviceEvent permits
        EligibleRentalsEvent, AvailableLockersEvent, LockerActionResultEvent { }
