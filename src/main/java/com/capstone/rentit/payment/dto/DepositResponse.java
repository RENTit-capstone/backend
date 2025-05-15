package com.capstone.rentit.payment.dto;

public record DepositResponse(
        NhHeader Header,
        String FinAcno,
        String RgsnYmd
) {}
