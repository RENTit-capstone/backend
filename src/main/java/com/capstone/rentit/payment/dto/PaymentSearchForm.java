package com.capstone.rentit.payment.dto;

import com.capstone.rentit.payment.type.PaymentType;

public record PaymentSearchForm(
        Long memberId,
        PaymentType type               // nullable → 전체 타입 조회
) { }
