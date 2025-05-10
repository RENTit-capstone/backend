package com.capstone.rentit.inquiry.dto;

import com.capstone.rentit.inquiry.type.InquiryType;

public record InquiryCreateForm(
        Long memberId,
        String title,
        String content,
        InquiryType type
) {}