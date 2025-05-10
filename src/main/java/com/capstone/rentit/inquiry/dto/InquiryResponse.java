package com.capstone.rentit.inquiry.dto;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.type.InquiryType;

import java.time.LocalDateTime;

public record InquiryResponse(
        Long inquiryId,
        Long memberId,
        InquiryType type,
        String title,
        String content,
        boolean processed,
        LocalDateTime createdAt
) {
    public static InquiryResponse fromEntity(Inquiry e) {
        return new InquiryResponse(
                e.getInquiryId(),
                e.getMemberId(),
                e.getType(),
                e.getTitle(),
                e.getContent(),
                e.isProcessed(),
                e.getCreatedAt()
        );
    }
}
