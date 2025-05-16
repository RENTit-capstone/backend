package com.capstone.rentit.payment.dto;

// ReceivedTransfer (농협입금이체 – 핀테크社 → 고객)
public record DepositRequest(
        NhHeader Header,
        String FinAcno,    // 고객 핀-어카운트
        String Tram,       // 금액
        String MractOtlt   // 입금 통장표시
) {}
