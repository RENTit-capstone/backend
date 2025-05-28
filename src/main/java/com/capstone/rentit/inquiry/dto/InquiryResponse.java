package com.capstone.rentit.inquiry.dto;

import com.capstone.rentit.inquiry.domain.Inquiry;
import com.capstone.rentit.inquiry.type.InquiryType;

import java.time.LocalDateTime;
import java.util.List;

public record InquiryResponse(
        Long inquiryId,
        Long memberId,
        InquiryType type,
        String title,
        String content,
        List<String> images,
        boolean processed,
        LocalDateTime createdAt
) {
    public static InquiryResponse fromEntity(Inquiry e, List<String> images) {
        return new InquiryResponse(
                e.getInquiryId(),
                e.getMemberId(),
                e.getType(),
                e.getTitle(),
                e.getContent(),
                images,
                e.isProcessed(),
                e.getCreatedAt()
        );
    }
}
