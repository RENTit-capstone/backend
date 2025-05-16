package com.capstone.rentit.payment.dto;

import com.capstone.rentit.payment.domain.Wallet;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WalletResponse(
        Long memberId,
        Long balance,
        String finAcno,
        String bankCode,
        LocalDateTime consentAt,
        LocalDateTime expiresAt
) {
    public static WalletResponse fromEntity(Wallet entity){
        return WalletResponse.builder()
                .memberId(entity.getMemberId())
                .balance(entity.getBalance())
                .finAcno(entity.getFinAcno())
                .bankCode(entity.getBankCode())
                .consentAt(entity.getConsentAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }
}
