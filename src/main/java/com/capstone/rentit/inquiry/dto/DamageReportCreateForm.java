package com.capstone.rentit.inquiry.dto;

import java.util.List;

public record DamageReportCreateForm(
        Long rentalId,
        String title,
        String content,
        List<String> images
) {}