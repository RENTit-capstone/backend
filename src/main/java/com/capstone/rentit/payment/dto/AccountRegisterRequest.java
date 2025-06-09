package com.capstone.rentit.payment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AccountRegisterRequest(
       @NotNull Long memberId,
       @NotEmpty String finAcno,
       @NotEmpty String bankCode
) {}
