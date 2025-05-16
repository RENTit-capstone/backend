package com.capstone.rentit.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@Validated
public record WithdrawRequest(
        @NotNull Long memberId,
        @Positive long amount
) {}

