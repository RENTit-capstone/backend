package com.capstone.rentit.notification.type;

public enum NotificationType {

    /* ─── 물품 소유자 ─── */
    RENT_REQUESTED,          // 대여자가 대여 요청
    RENT_CANCEL,             // 대여자가 대여 취소
    ITEM_RETURNED,           // 대여자가 반납 완료
    ITEM_DAMAGED_REQUEST,    // 물품 파손 신고 요청
    RENT_START_D_3,          // 대여 시작 3일 전
    RENT_START_D_0,          // 대여 시작 당일

    /* ─── 물품 대여자 ─── */
    REQUEST_ACCEPTED,        // 소유자가 신청 승낙
    REQUEST_REJECTED,        // 소유자가 신청 거절
    ITEM_DAMAGED_RESPONSE,   // 물품 파손 신고 응답
    ITEM_PLACED,             // 소유자가 사물함에 물건 넣음
    RENT_END_D_3,            // 대여 만료 3일 전
    RENT_END_D_0,            // 대여 만료 당일

    INQUIRY_RESPONSE
}
