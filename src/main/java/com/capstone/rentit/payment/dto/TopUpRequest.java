package com.capstone.rentit.payment.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
public record TopUpRequest(
        @NotNull Long memberId,
        @Positive long amount          // 1원 단위
) {}
