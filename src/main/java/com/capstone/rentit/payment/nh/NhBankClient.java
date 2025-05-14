package com.capstone.rentit.payment.nh;

public interface NhBankClient {
    NhTransferResponse withdrawFromUser(Long memberId, long amount, String desc); // 현금 → 서비스 계좌
    NhTransferResponse depositToUser(Long memberId, long amount, String desc);    // 서비스 계좌 → 현금
}