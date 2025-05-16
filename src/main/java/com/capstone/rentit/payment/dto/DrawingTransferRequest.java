package com.capstone.rentit.payment.dto;

// DrawingTransfer (출금이체 – 고객 → 핀테크)
public record DrawingTransferRequest(
        NhHeader Header,
        String FinAcno,    // 고객 핀-어카운트
        String Tram,       // 금액
        String DractOtlt,  // 통장 표시
        String MractOtlt   // 미사용
) {}