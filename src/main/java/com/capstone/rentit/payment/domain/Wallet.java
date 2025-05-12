package com.capstone.rentit.payment.domain;

import com.capstone.rentit.payment.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Wallet {

    @Id
    private Long memberId;

    @Column(nullable = false)
    private long balance;

    public void deposit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount > 0");
        balance += amount;
    }

    public void withdraw(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount > 0");
        if (balance < amount) throw new InsufficientBalanceException("잔액이 부족합니다. 현재 잔액: " + balance + " 결제 금액" + amount);
        balance -= amount;
    }
}
