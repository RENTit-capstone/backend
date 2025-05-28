package com.capstone.rentit.inquiry.dto;

import com.capstone.rentit.inquiry.type.InquiryType;

public record InquiryCreateForm(
        String title,
        String content,
        InquiryType type
) {}