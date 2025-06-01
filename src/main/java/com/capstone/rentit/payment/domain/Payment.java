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
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_type", columnList = "type"),
                @Index(name = "idx_payment_from_member", columnList = "from_member_id"),
                @Index(name = "idx_payment_to_member", columnList = "to_member_id"),
                @Index(name = "idx_payment_created_at", columnList = "created_at")
        }
)
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

    private Long paymentRentalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private Rental rental;

    public static Payment create(PaymentType type, Long fromId, Long toId, long amount, Long rentalId) {
        return Payment.builder()
                .type(type)
                .status(PaymentStatus.REQUESTED)
                .fromMemberId(fromId)
                .toMemberId(toId)
                .amount(amount)
                .paymentRentalId(rentalId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void approve(String istuno) {
        this.status = PaymentStatus.APPROVED;
        this.istuno = istuno;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel(){
        this.status = PaymentStatus.CANCELED;
    }
}
