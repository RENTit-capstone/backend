package com.capstone.rentit.inquiry.domain;

import com.capstone.rentit.inquiry.dto.DamageReportCreateForm;
import com.capstone.rentit.inquiry.dto.InquiryAnswerForm;
import com.capstone.rentit.inquiry.dto.InquiryCreateForm;
import com.capstone.rentit.inquiry.type.InquiryType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "inquiry",
        indexes = {
                @Index(name = "idx_inquiry_type_processed_createdAt", columnList = "type, processed, createdAt"),
                @Index(name = "idx_inquiry_createdAt", columnList = "createdAt"),
                @Index(name = "idx_inquiry_memberId", columnList = "memberId"),
                @Index(name = "idx_inquiry_targetMemberId", columnList = "targetMemberId")
        }
)
public class Inquiry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inquiryId;

    @Column(nullable = false)
    private Long memberId;

    private Long targetMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "inquiry_damage_images")
    @Column(name = "url")
    private List<String> damageImageKeys;

    @Builder.Default
    private boolean processed = false;

    private LocalDateTime createdAt;

    @Column(length = 4000)
    private String answer;

    @Column
    private LocalDateTime processedAt;

    public static Inquiry create(Long memberId, InquiryCreateForm form){
        return Inquiry.builder()
                .memberId(memberId)
                .title(form.title())
                .content(form.content())
                .type(form.type())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Inquiry create(Long MemberId, Long targetId, DamageReportCreateForm form){
        return Inquiry.builder()
                .memberId(MemberId)
                .targetMemberId(targetId)
                .type(InquiryType.DAMAGE)
                .title(form.title())
                .content(form.content())
                .damageImageKeys(form.images())
                .processed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void answerInquiry(InquiryAnswerForm form){
        answer = form.answer();
        markProcessed();
    }

    public void markProcessed() {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
    }
}