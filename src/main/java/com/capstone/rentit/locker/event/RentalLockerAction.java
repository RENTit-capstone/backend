package com.capstone.rentit.locker.event;

//해야 할 일
public enum RentalLockerAction {
    DROP_OFF_BY_OWNER,   // 소유자 물건 맡기기
    PICK_UP_BY_RENTER,   // 대여자 픽업
    RETURN_BY_RENTER,    // 대여자 반납
    RETRIEVE_BY_OWNER    // 소유자 회수
}
