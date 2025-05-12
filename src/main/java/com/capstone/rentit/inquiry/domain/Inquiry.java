package com.capstone.rentit.inquiry.domain;

import com.capstone.rentit.inquiry.dto.InquiryAnswerForm;
import com.capstone.rentit.inquiry.type.InquiryType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx_inquiry_type_processed_created",
                columnList = "type, processed, createdAt DESC")
})
public class Inquiry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inquiryId;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Builder.Default
    private boolean processed = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 4000)
    private String answer;

    public void answerInquiry(InquiryAnswerForm form){
        answer = form.answer();
        markProcessed();
    }

    public void markProcessed() {
        this.processed = true;
    }
}