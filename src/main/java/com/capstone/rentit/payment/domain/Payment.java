package com.capstone.rentit.payment.domain;

import com.capstone.rentit.payment.type.PaymentStatus;
import com.capstone.rentit.payment.type.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status;

    private Long fromMemberId;
    private Long toMemberId;

    @Column(nullable = false)
    private long amount;

    private String extTxId;           // NH 거래 번호
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public static Payment create(PaymentType type, Long fromId, Long toId, long amount) {
        return Payment.builder()
                .type(type)
                .status(PaymentStatus.REQUESTED)
                .fromMemberId(fromId)
                .toMemberId(toId)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void approve(String extTxId) {
        this.status = PaymentStatus.APPROVED;
        this.extTxId = extTxId;
        this.approvedAt = LocalDateTime.now();
    }
}
