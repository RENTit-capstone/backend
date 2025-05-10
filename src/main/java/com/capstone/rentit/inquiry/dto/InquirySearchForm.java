package com.capstone.rentit.inquiry.dto;

import com.capstone.rentit.inquiry.type.InquiryType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record InquirySearchForm(
        InquiryType type,
        Boolean processed,
        LocalDateTime fromDate,
        LocalDateTime toDate
) {}