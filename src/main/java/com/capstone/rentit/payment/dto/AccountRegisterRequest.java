package com.capstone.rentit.payment.dto;

public record AccountRegisterRequest(
        Long memberId,
        String finAcno,
        String bankCode
) {}
