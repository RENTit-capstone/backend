package com.capstone.rentit.payment.domain;

import com.capstone.rentit.payment.type.PaymentStatus;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.domain.Rental;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    private String istuno;           //기관 거래 고유 번호
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private Rental rental;

    public static Payment create(PaymentType type, Long fromId, Long toId, long amount, Rental rental) {
        return Payment.builder()
                .type(type)
                .status(PaymentStatus.REQUESTED)
                .fromMemberId(fromId)
                .toMemberId(toId)
                .amount(amount)
                .rental(rental)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void approve(String istuno) {
        this.status = PaymentStatus.APPROVED;
        this.istuno = istuno;
        this.approvedAt = LocalDateTime.now();
    }
}
