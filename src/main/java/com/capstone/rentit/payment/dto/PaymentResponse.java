package com.capstone.rentit.payment.dto;

import com.capstone.rentit.payment.domain.Payment;
import com.capstone.rentit.payment.type.PaymentStatus;
import com.capstone.rentit.payment.type.PaymentType;
import com.capstone.rentit.rental.domain.Rental;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PaymentResponse(
        Long paymentId,
        PaymentType type,
        PaymentStatus status,
        long amount,
        LocalDateTime createdAt,
        Rental rental
) {
    public static PaymentResponse fromEntity(Payment entity){
        return PaymentResponse.builder()
                .paymentId(entity.getId())
                .type(entity.getType())
                .status(entity.getStatus())
                .amount(entity.getAmount())
                .createdAt(entity.getCreatedAt())
                .rental(entity.getRental() == null ? null : entity.getRental())
                .build();
    }
}

