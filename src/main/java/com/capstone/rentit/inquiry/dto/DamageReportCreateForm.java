package com.capstone.rentit.inquiry.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DamageReportCreateForm(
        @NotNull Long rentalId,
        @NotEmpty String title,
        @NotEmpty String content,
        @NotNull List<String> images
) {}