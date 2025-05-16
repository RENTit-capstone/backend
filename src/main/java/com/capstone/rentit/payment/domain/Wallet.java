package com.capstone.rentit.payment.domain;

import com.capstone.rentit.payment.exception.AccountConsentExpiredException;
import com.capstone.rentit.payment.exception.AccountNotRegisteredException;
import com.capstone.rentit.payment.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Wallet {

    @Id
    private Long memberId;

    @Column(nullable = false)
    private long balance;

    @Column(length = 24)          // 핀어카운트 24자리
    private String finAcno;       // 사용자 계좌(핀어카운트)

    @Column(length = 3)
    private String bankCode;      // 은행코드 (UI·CS용, optional)

    private LocalDateTime consentAt;
    private LocalDateTime expiresAt;

    public void registerAccount(String finAcno, String bankCode) {
        this.finAcno = finAcno;
        this.bankCode = bankCode;
        this.consentAt = LocalDateTime.now();
        this.expiresAt = consentAt.plusYears(1);
    }

    public void ensureAccountRegistered() {
        if (finAcno == null)
            throw new AccountNotRegisteredException("핀어카운트가 등록되지 않았습니다.");

        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now()))
            throw new AccountConsentExpiredException("핀-어카운트 동의가 만료되었습니다. 다시 연결해 주세요.");
    }

    public void deposit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount > 0");
        balance += amount;
    }

    public void withdraw(long amount) {
        checkBalance(amount);
        balance -= amount;
    }

    public void checkBalance(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount > 0");
        if (balance < amount) throw new InsufficientBalanceException("잔액이 부족합니다. 현재 잔액: " + balance + " 결제 금액" + amount);
    }
}
