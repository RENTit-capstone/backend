package com.capstone.rentit.payment.dto;

public record DrawingTransferResponse(
        NhHeader Header,
        String FinAcno,
        String RgsnYmd
) {}
