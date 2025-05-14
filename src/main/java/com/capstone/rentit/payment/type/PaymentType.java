package com.capstone.rentit.payment.type;

public enum PaymentType {
    TOP_UP,        // 현금 → 포인트 충전
    WITHDRAWAL,    // 포인트 → 현금 출금

    RENTAL_FEE,            // 대여자가 소유자에게 지불
    LOCKER_FEE_RENTER,     // 대여자가 물품을 꺼낼 때
    LOCKER_FEE_OWNER       // 소유자가 물품을 회수할 때
}